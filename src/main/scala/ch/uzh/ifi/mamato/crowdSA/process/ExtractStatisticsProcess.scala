package ch.uzh.ifi.mamato.crowdSA.process

import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer, Dataset}
import ch.uzh.ifi.mamato.crowdSA.persistence.{StatMethod2AssumptionDAO, Assumption2QuestionsDAO, StatMethodsDAO, AssumptionsDAO}
import ch.uzh.ifi.mamato.crowdSA.util.{PdfUtils, LazyLogger}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{IndexedPatch, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{SimpleRecombinationVariantXMLExporter, RecombinationVariant, Recombinable}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithStatisticalReductionProcess, FixPatchProcess}
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.collection.mutable

/**
 * Created by mattia on 27.02.15.
 */

class ExtractStatisticsProcess(crowdSA: CrowdSAPortalAdapter, val discoveryQuestion: List[CrowdSAQuery])
  extends Recombinable[String] with LazyLogger {

  override def runRecombinedVariant(v: RecombinationVariant): String = {
    logger.debug("running variant " + new SimpleRecombinationVariantXMLExporter(v).xml)

    // Process single variant
    val processes = new ProcessVariant(v)

    val assumptionToTest = new mutable.MutableList[(String, Answer)]
    var result = "Result for paper: " + Main.titlePdf + " - "

    // From each discovery question we will get at the end only one answer.
    // This answers is the convergence of all the answers
    discoveryQuestion.foreach(d => {
      val paper_id = d.getProperties().paper_id
      val convergedAnswer = v.createProcess[CrowdSAQuery, Answer]("discoveryProcess").process(d)
      logger.debug(convergedAnswer.answer)
      logger.debug("***** result DISCOVERY STEP")
      //Create the dataset!
      val datasetId = CrowdSAPortalAdapter.service.createDataset(convergedAnswer.id)

      // **** Start the second phase of the process ****
      // Once we have defined a dataset we can start to ask the questions for the assumptions.
      // get the questions to be asked (assumption for each statistical method that has a match)

      // FIXME: ugly!
      val stat_method = d.query.question.substring(d.query.question.indexOf(": ") + 2, d.query.question.indexOf(" highlighted"))
      val statMethod = StatMethodsDAO.findByStatMethod(stat_method)

      // Get text of pdf
      val pdfToText = PdfUtils.getTextFromPdf(Main.pathPdf).get

      logger.debug("Getting assumptions for statistical method: " + statMethod.get.stat_method)
      StatMethod2AssumptionDAO.findByStatMethodId(statMethod.get.id).par.foreach(e => {

        // Check if the assumption test name is present in the pdf.
        val assumption = AssumptionsDAO.find(e.assumption_id).get.assumption

        logger.debug("Analyzing paper for assumption: " + assumption)
        // Find if in the text the assumption is present, otherwise ask only a single general question.
        val found = new AtomicReference[Boolean]
        found.set(false)
        Assumption2QuestionsDAO.findByAssumptionId(e.assumption_id).par.foreach(b => {
          // start the AssessmentProcess and wait for Answer for each question (this is also a collectDecide process)
          logger.debug("Checking if there is a match for the assumption: " + assumption)

          if(PdfUtils.findContextMatchMutipleMatches(pdfToText.toUpperCase(), b.test_names.toUpperCase().split(",").toList).length >0) {
            logger.debug("Match found for assumption: " + assumption)

            found.set(true)

            // If a match is found for the assumption ask the question!
            val converged = v.createProcess[CrowdSAQuery, Answer]("assessmentProcess").process(
              new CrowdSAQuery(new HCompQuery {
                override def question: String = b.question

                override def title: String = b.id.toString

                override def suggestedPaymentCents: Int = 10
              }, new CrowdSAQueryProperties(paper_id, "Boolean",
                new Highlight("DatasetWithAssumptionTest", convergedAnswer.answer.replaceAll("#", ",") + "," + b.test_names), 10, ((new Date().getTime() / 1000) + 1000 * 60 * 60 * 24 * 365), 100)))

            assumptionToTest.+=:(assumption, converged)

            logger.debug("Assessment step for question: " + b.question + " converged to answer: " + converged.answer)
          }
        })

        if(!found.get()) {
          logger.debug("No match found for assumption: "+ assumption)

          logger.debug("Asking general question for assumption: " + assumption)
          val converged = v.createProcess[CrowdSAQuery, Answer]("assessmentProcess").process(
            new CrowdSAQuery(new HCompQuery {
              override def question: String = "Is the dataset highlighted in the paper tested for " + assumption + "?"
              override def title: String = assumption
              override def suggestedPaymentCents: Int = 10
            }, new CrowdSAQueryProperties(paper_id, "Boolean",
              new Highlight("DatasetWithGeneralAssumption", convergedAnswer.answer.replaceAll("#", ",")+assumption), 10, ((new Date().getTime()/1000) + 1000*60*60*24*365), 100)))

          assumptionToTest.+=:(assumption, converged)
          logger.debug("Assessment step for general question about: " + assumption + " converged to answer: " + converged.answer)

        }

      })

      logger.debug("Checking if paper is valid or not")
      result += "Regarding dataset id: " + datasetId + " - "
      assumptionToTest.groupBy(_._1).foreach( att => {
        var valid = false
        att._2.foreach(tc => {
          if(tc._2.answer.equalsIgnoreCase("true")){
            valid = true
          }
        })
        if(valid){
          result += "SUCCESS: Assumption " + att._1 + " is respected. "
        }else {
          result += "FAIL: Assumption " + att._1 + " is not respected. "
        }

     })

    })

    //return if paper is valid or not
    result += "End of recombination."
    logger.info(result)
    result
  }

  override def allRecombinationKeys: List[String] = List("discoveryProcess")

  private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
    def createDiscovery = decorated.createProcess[CrowdSAQuery, Answer]("discoveryProcess")
  }

}