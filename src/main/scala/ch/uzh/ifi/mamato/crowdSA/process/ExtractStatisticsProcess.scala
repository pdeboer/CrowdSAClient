package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAPortalAdapter
import ch.uzh.ifi.mamato.crowdSA.model._
import ch.uzh.ifi.mamato.crowdSA.persistence._
import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
import ch.uzh.ifi.mamato.crowdSA.util.{LazyLogger, PdfUtils}
import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{Recombinable, RecombinationVariant, SimpleRecombinationVariantXMLExporter}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

import scala.collection.mutable

/**
 * Main process which start the two phases: Discovery and Assessment.
 * For each discovery question created by the Main class the discovery process is started. Once the answers converges to
 * one, the second phase starts.
 * Created by mattia on 27.02.15.
 */
class ExtractStatisticsProcess(crowdSA: CrowdSAPortalAdapter, discoveryQuestion: List[CrowdSAPatch])
	extends Recombinable[String] with LazyLogger {

	/**
	 * Runs a recombination variant. First the Discovery step is performed, and second the Assessment step.
	 * @param v The variation to execute
	 * @return A feedback based on all the converged answers for this variant.
	 */
	override def runRecombinedVariant(v: RecombinationVariant): String = {
		logger.debug("running variant " + new SimpleRecombinationVariantXMLExporter(v).xml)

		// Process single variant
		val processes = new ProcessVariant(v)

		// Complex datastructure to store [dataset, statistical method, assumption, List[converged answers]]
		val datasetAssumptionTested = new mutable.MutableList[(Long, String, String, mutable.MutableList[CrowdSAPatch])]

		var result = ""
		this.synchronized {
			result = "**** Start result for paper: " + Main.titlePdf + "\n"
		}

		//Store all the statistical methods already asked for each dataset
		val datasetStatMethod = new mutable.MutableList[(Long, String)]
		// Ugly - otherwise mpar fails
		datasetStatMethod.+=:(-1, "")

		// From each discovery question we will get at the end only one answer.
		// Parallel ask all the discovery questions
		discoveryQuestion.mpar.foreach(d => {

			val paper_id = d.paperId

      if(d.questionType.equalsIgnoreCase("Missing")){
        val missingMethods = v.createProcess[CrowdSAPatch, List[CrowdSAPatch]]("missingProcess").process(d)
        this.synchronized{
          var ss = ""
          missingMethods.foreach(mm => {
            try{
              val a = mm.answer
              if(a.isEmpty){
                ss += "\n* - No method found"
              } else if(a.contains("#")) {
                ss += a.replaceAll("#", "\n* - ")
              } else {
                ss += ("\n* - " + a)
              }
            } catch {
              case e: Exception => {
                ss += "\n* - No method found"
              }
            }
          })
          result = "\n* Methods which were not automatically identified: " + ss + "\n\n"
        }
      } else {
        val datasetConverged = v.createProcess[CrowdSAPatch, CrowdSAPatch]("discoveryProcess").process(d)

        // FIXME: ugly! - used to identify the statistical method of the discovery question
        var stat_method = ""
        var statMethod: Option[StatMethod] = None
        this.synchronized {
          val question = d.question
          stat_method = question.substring(question.indexOf("<i> ") + 4, question.indexOf(" </i>"))
          statMethod = StatMethodsDAO.findByStatMethod(stat_method)
        }

        // If the dataset exists, the statistical method is NOT a false positive
        val answer = AnswersDAO.find(datasetConverged.answerId).get
        if (answer.answer != "" && answer.is_method_used == true) {
          logger.debug("***** result DISCOVERY STEP")
          // Try to create the dataset if it doesn't yet exists!
          var datasetId: Long = -1
          this.synchronized {
            datasetId = CrowdSAPortalAdapter.service.createDataset(answer.id)
          }

          // **** Start the second phase of the process ****
          // Once we have defined a dataset we can start to ask the questions for the assumptions.
          // get the questions to be asked (assumption for each statistical method that has a match)

          // Search if the assumption phase was already started for the actual statistical method and the dataset id
          if (datasetStatMethod.filter(_._1 == datasetId).filter(_._2 == stat_method).length == 0) {
            this.synchronized {
              // Store in the map so that we know this statistical method
              // was already tested for the assumption on the dataset id
              datasetStatMethod.+=:(datasetId, stat_method)
            }
            // Getting assumptions for statistical method
            StatMethod2AssumptionDAO.findByStatMethodId(statMethod.get.id).mpar.foreach(e => {
              // Check if the assumption test name is present in the pdf.
              val assumption = AssumptionsDAO.find(e.assumption_id).get.assumption
              logger.info("Running assumption step")
              checkAssumption(assumption, e, paper_id, answer,
                datasetAssumptionTested,
                v, datasetId, stat_method)
            })

            // Evaluate the results once the process is ended
            this.synchronized {
              result += "\n* Dataset id: " + datasetId + "\n"
              result += "* - Method: " + stat_method + "\n"
              datasetAssumptionTested.foreach(elem => {
                if (elem._1 == datasetId && elem._2 == stat_method) {
                  result += "* -- Assumption: " + elem._3 + "\n"
                  elem._4.foreach(assump => {
                    if (assump.answer.equalsIgnoreCase("[\"true\"]")) {
                      result += "* --- TESTED\n"
                    } else {
                      result += "* --- NOT TESTED\n"
                    }
                  })
                }
              })
            }
          }
        } else if (answer.answer == "" && answer.is_method_used == false) {
          this.synchronized {
            logger.debug("Skip dataset because method is not used on the paper.")
            result += "\n** Dataset for statistical method: " + stat_method + " is not used on the paper.**\n"
          }
        } else if (answer.answer == "" && answer.is_method_used == true) {
          this.synchronized {
            logger.debug("Skip dataset because not found on the paper.")
            result += "\n** Dataset for statistical method: " + stat_method + " could not be identified in the paper.**\n"
          }
        }

      }

    })

    result += "\n\n**** End of recombination."
    logger.info(result)
    result

	}

	/**
	 * Assumption step
	 * @param assumption assumption under test
	 * @param e reference between statistical method and assumption
	 * @param paper_id id of the paper
	 * @param datasetConverged dataset identified in the previous step
	 * @param datasetAssumptionTested map to compute the end result of the variant
	 * @param v Recombination variant used to start the assumption process
	 * @param dataset_id id of the dataset
	 * @param statMethod statistical method under analysis
	 */
	def checkAssumption(assumption: String, e: StatMethod2Assumption,
						paper_id: Long, datasetConverged: Answer,
						datasetAssumptionTested: mutable.MutableList[(Long, String, String, mutable.MutableList[CrowdSAPatch])],
						v: RecombinationVariant, dataset_id: Long, statMethod: String) = {
		logger.debug("Analyzing paper for assumption: " + assumption)

		var pdfContainsAssumption = false
		// Find if in the text the assumption is present, otherwise ask only a single general question.
		Assumption2QuestionsDAO.findByAssumptionId(e.assumption_id).mpar.foreach(assumptionQuestion => {
			// start the AssessmentProcess and wait for Answer for each question (this is also a collectDecide process)
			logger.debug("Check if there is a match for the assumption: " + assumption)
			val pdfToText = PdfUtils.getTextFromPdf(Main.pathPdf).get

      val termsList = assumptionQuestion.test_names.split(",").toList

			if (PdfUtils.findContextMatchMutipleMatches(pdfToText, termsList).length > 0) {
				logger.debug("Match found for assumption: " + assumption)
				this.synchronized {
					pdfContainsAssumption = true
				}

        var termsJson = "["
        termsList.foreach(term => {
          termsJson += ("\""+term+"\"")
          if(termsList.last != term){
            termsJson += ","
          }
        })
        termsJson += "]"

				val booleanQuery = new CrowdSAPatch(assumptionQuestion.question, "Boolean",
          termsJson, paper_id, "DatasetWithAssumptionTest")

        booleanQuery.dataset = datasetConverged.answer//.replaceAll("’", "'") //.replaceAll("'","\'"),


				// If a match is found for the assumption ask the question!
				val converged = v.createProcess[CrowdSAPatch, List[CrowdSAPatch]]("assessmentProcess").process(booleanQuery)

				// Add converged answer to the assumption to test
				this.synchronized {

					var found = false
					datasetAssumptionTested.foreach(a => {
						if (a._1 == dataset_id && a._2 == statMethod && a._3 == assumption) {
							found = true
							converged.foreach(ans => a._4 += ans)
						}
					})
					if (!found) {
						val list = new mutable.MutableList[CrowdSAPatch]
            converged.foreach(ans => list += ans)
						datasetAssumptionTested.+=:((dataset_id, statMethod, assumption, list))
					}
				}

				//logger.debug("Assessment step for question: " + assumptionQuestion.question + " converged to answer: " + converged.answer)
			}
		})

		var allFalse = true
		this.synchronized {
			datasetAssumptionTested.foreach(a => {
				if (a._1 == dataset_id && a._2 == statMethod && a._3 == assumption) {
					a._4.foreach(ans => {
						if (ans.answer.equalsIgnoreCase("[\"true\"]")) {
							allFalse = false
						}
					})
				}
			})
		}

		// Ask general question if all the previous questions are false
		// or if there is no match in the paper
		if (!pdfContainsAssumption || allFalse) {
			logger.debug("No match found for assumption: " + assumption)

			logger.debug("Asking general question for assumption: " + assumption)

			val pp = new CrowdSAPatch("Is the dataset highlighted in the paper tested for the assumption: <i>" + assumption + "</i>?",
      "Boolean", "[\""+assumption+"\"]", paper_id, "DatasetWithGeneralAssumption")

      pp.dataset = datasetConverged.answer

			val converged = v.createProcess[CrowdSAPatch, List[CrowdSAPatch]]("assessmentProcess").process(pp)

			//Update the list of assumption to test with the generic converged answer
			this.synchronized {
				var found = false
				datasetAssumptionTested.foreach(a => {
					if (a._1 == dataset_id && a._2 == statMethod && a._3 == assumption) {
						found = true
            converged.foreach(ans => a._4 += ans)
					}
				})
				if (!found) {
					val list = new mutable.MutableList[CrowdSAPatch]
          converged.foreach(ans => list += ans)
					datasetAssumptionTested.+=:((dataset_id, statMethod, assumption, list))
				}
			}
			//logger.debug("Assessment step for general question about: " + assumption + " converged to answer: " + converged.answer)
		}
	}

	def allRecombinationKeys: List[String] = List("discoveryProcess")

	private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
		def createDiscovery = decorated.createProcess[CrowdSAPatch, CrowdSAPatch]("discoveryProcess")
	}

  override def requiredProcessDefinitions: Map[String, Class[_ <: ProcessStub[_, _]]] = Map.empty
}