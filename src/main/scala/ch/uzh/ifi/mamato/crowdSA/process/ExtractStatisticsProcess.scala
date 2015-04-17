package ch.uzh.ifi.mamato.crowdSA.process

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model._
import ch.uzh.ifi.mamato.crowdSA.persistence._
import ch.uzh.ifi.mamato.crowdSA.util.{PdfUtils, LazyLogger}
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{SimpleRecombinationVariantXMLExporter, RecombinationVariant, Recombinable}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

import scala.collection.mutable

/**
 * Main process which start the two phases: Discovery and Assessment.
 * For each discovery question created by the Main class the discovery process is started. Once the answers converges to
 * one, the second phase starts.
 * Created by mattia on 27.02.15.
 */
class ExtractStatisticsProcess(crowdSA: CrowdSAPortalAdapter, val discoveryQuestion: List[CrowdSAQuery])
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

    val datasetAssumptionTested = new mutable.MutableList[(Long, String, String, mutable.MutableList[Answer])]
    var result = ""
    this.synchronized {
      result = "Result for paper: " + Main.titlePdf + "\n"
    }

    //Store all the statistical methods already asked for each dataset
    var datasetStatMethod = new mutable.MutableList[(Long, String)]
    datasetStatMethod.+=:(-1, "")

    // From each discovery question we will get at the end only one answer.
    // This answers is the convergence of all the answers
    discoveryQuestion.mpar.foreach(d => {
      val paper_id = d.getProperties().paper_id
      val datasetConverged = v.createProcess[CrowdSAQuery, Answer]("discoveryProcess").process(d)

      // FIXME: ugly!
      var stat_method = ""
      var statMethod: Option[StatMethod] = None
      this.synchronized{
        stat_method = d.query.question.substring(d.query.question.indexOf("<i> ") + 4, d.query.question.indexOf(" </i>"))
        statMethod = StatMethodsDAO.findByStatMethod(stat_method)
      }

      // If the dataset exists and the match was not a false positive statistical method:
      if (datasetConverged.answer != "") {
        logger.debug("***** result DISCOVERY STEP")
        // Try to create the dataset!
        var datasetId: Long = -1
        this.synchronized {
          datasetId = CrowdSAPortalAdapter.service.createDataset(datasetConverged.id)
        }

        // **** Start the second phase of the process ****
        // Once we have defined a dataset we can start to ask the questions for the assumptions.
        // get the questions to be asked (assumption for each statistical method that has a match)

        // Get text of pdf
        val pdfToText = PdfUtils.getTextFromPdf(Main.pathPdf).get
        var foundd = false
        datasetStatMethod.mpar.foreach(a => {
          if(a._1 == datasetId && a._2 == stat_method){
            this.synchronized {
              foundd = true
            }
          }
        })

        if(!foundd) {
          this.synchronized {
            datasetStatMethod.+=:(datasetId, stat_method)
          }
          logger.debug("Getting assumptions for statistical method: " + statMethod.get.stat_method)
          StatMethod2AssumptionDAO.findByStatMethodId(statMethod.get.id).mpar.foreach(e => {
            // Check if the assumption test name is present in the pdf.
            val assumption = AssumptionsDAO.find(e.assumption_id).get.assumption
            logger.info("Running assumption step")
            checkAssumption(assumption, e, pdfToText, paper_id, datasetConverged, datasetAssumptionTested, v, datasetId, stat_method)
          })

          this.synchronized {
          var allFalse = true
            result += "\n* Dataset id: " + datasetId +"\n"
            result += "* - Method: " + stat_method + "\n"
            datasetAssumptionTested.foreach(elem => {
              if(elem._1 == datasetId && elem._2 == stat_method){
                result += "* -- Assumption: " + elem._3 + "\n"
                elem._4.foreach(assump => {
                  if(assump.answer == "true"){
                    allFalse = false
                  }
                })
                if(allFalse){
                  result += "* --- FAIL\n"
                }else{
                  result += "* --- SUCCESS\n"
                }
              }
            })
          }
        } else {
          // Do nothing, the statistical method was already tested (or is already under test)
        }
      } else {
        this.synchronized {
          logger.debug("Skip dataset because was a false match.")
          result += "\n** Dataset for statistical method: " + stat_method + " was a false positive.**\n"
        }
      }

    })

    //return if paper is valid or not
    result += "\n\n**** End of recombination."
    logger.info(result)
    result
  }

  def checkAssumption(assumption: String, e: StatMethod2Assumption,
                      pdfToText: String, paper_id: Long, datasetConverged: Answer,
                      datasetAssumptionTested: mutable.MutableList[(Long, String, String, mutable.MutableList[Answer])],
                       v: RecombinationVariant, dataset_id: Long, statMethod: String) = {
    logger.debug("Analyzing paper for assumption: " + assumption)
    // Find if in the text the assumption is present, otherwise ask only a single general question.
    var found: Boolean = false
    Assumption2QuestionsDAO.findByAssumptionId(e.assumption_id).mpar.foreach(b => {
      // start the AssessmentProcess and wait for Answer for each question (this is also a collectDecide process)
      logger.debug("Checking if there is a match for the assumption: " + assumption)

      if (PdfUtils.findContextMatchMutipleMatches(pdfToText, b.test_names.split(",").toList).length > 0) {
        logger.debug("Match found for assumption: " + assumption)

        this.synchronized {
          found = true
        }

        // If a match is found for the assumption ask the question!
        val converged = v.createProcess[CrowdSAQuery, Answer]("assessmentProcess").process(
          new CrowdSAQuery(new HCompQuery {
            override def question: String = b.question

            override def title: String = b.id.toString

            override def suggestedPaymentCents: Int = 10
          }, new CrowdSAQueryProperties(paper_id, "Boolean",
            HighlightDAO.create("DatasetWithAssumptionTest",
              datasetConverged.answer + "#" + b.test_names.replaceAll(",", "#") + "#", -1),
            10, ((new Date().getTime() / 1000) + 60 * 60 * 24 * 365),
            100, Some(""), null)))

        // Add converged answer to the assumption to test
        this.synchronized {
          var found = false
          datasetAssumptionTested.foreach(a => {
            if(a._1 == dataset_id && a._2 == statMethod && a._3 == assumption){
              found = true
              a._4.+=:(converged)
            }
          })

          if(!found){
            val list = new mutable.MutableList[Answer]
            list.+=(converged)
            datasetAssumptionTested.+=:((dataset_id, statMethod, assumption, list))
          }
        }

        logger.debug("Assessment step for question: " + b.question + " converged to answer: " + converged.answer)
      }
    })

    var allFalse = true
    this.synchronized {
      datasetAssumptionTested.foreach(a => {
        if(a._1 == dataset_id && a._2 == statMethod && a._3 == assumption){
          a._4.foreach(ans =>{
            if(ans.answer == "true"){
              allFalse = false
            }
          })
        }
      })
    }

    // Ask general question if all the previous questions are false
    // or if there is no match in the paper
      if (!found || allFalse) {
        logger.debug("No match found for assumption: " + assumption)

        logger.debug("Asking general question for assumption: " + assumption)
        val converged = v.createProcess[CrowdSAQuery, Answer]("assessmentProcess").process(
          new CrowdSAQuery(new HCompQuery {
            override def question: String = "Is the dataset highlighted in the paper tested for the assumption: <i>" + assumption + "</i>?"

            override def title: String = assumption

            override def suggestedPaymentCents: Int = 10
          }, new CrowdSAQueryProperties(paper_id, "Boolean",
            HighlightDAO.create("DatasetWithGeneralAssumption",
              datasetConverged.answer + "#" + assumption, -1), 10,
            ((new Date().getTime() / 1000) + 60 * 60 * 24 * 365), 100, Some(""), null)))

        //Update the list of assumption to test with the generic converged answer
        this.synchronized {
          var found = false
          datasetAssumptionTested.foreach(a => {
            if(a._1 == dataset_id && a._2 == statMethod && a._3 == assumption){
              found = true
              a._4.+=:(converged)
            }
          })

          if(!found){
            val list = new mutable.MutableList[Answer]
            list.+=(converged)
            datasetAssumptionTested.+=:((dataset_id, statMethod, assumption, list))
          }
        }
        logger.debug("Assessment step for general question about: " + assumption + " converged to answer: " + converged.answer)

      }

  }

  override def allRecombinationKeys: List[String] = List("discoveryProcess")

  private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
    def createDiscovery = decorated.createProcess[CrowdSAQuery, Answer]("discoveryProcess")
  }
}