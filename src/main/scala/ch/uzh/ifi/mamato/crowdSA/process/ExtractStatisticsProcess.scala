package ch.uzh.ifi.mamato.crowdSA.process

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer, Dataset}
import ch.uzh.ifi.mamato.crowdSA.persistence.{StatMethod2AssumptionDAO, Assumption2QuestionsDAO, StatMethodsDAO, AssumptionsDAO}
import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{IndexedPatch, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{SimpleRecombinationVariantXMLExporter, RecombinationVariant, Recombinable}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithStatisticalReductionProcess, FixPatchProcess}
import ch.uzh.ifi.pdeboer.pplib.util.U

/**
 * Created by mattia on 27.02.15.
 */

class ExtractStatisticsProcess(crowdSA: CrowdSAPortalAdapter, val discoveryQuestion: List[CrowdSAQuery])
  extends Recombinable[String] with LazyLogger {

  override def runRecombinedVariant(v: RecombinationVariant): String = {
    logger.debug("running variant " + new SimpleRecombinationVariantXMLExporter(v).xml)

    val processes = new ProcessVariant(v)

    // From each discovery question we will get at the end only one answer.
    // This answers is the convergence of all the answers
    discoveryQuestion.foreach(d => {

          val paper_id = d.getProperties().paper_id
          val convergedAnswer = v.createProcess[CrowdSAQuery, Answer]("discoveryProcess").process(d)
          logger.debug(convergedAnswer.answer)
          logger.debug("***** result DISCOVERY STEP")
          //Create the dataset!
          CrowdSAPortalAdapter.service.createDataset(convergedAnswer.id)
          // Start the second phase of the process
          // Once we have defined a dataset we can start to ask the questions for the assumptions.
          //get the questions to be asked (assumption for each statistical method that has a match)
          val stat_method = d.query.question.substring(d.query.question.indexOf(": ") + 2, d.query.question.indexOf(" highlighted"))
          val statMethod = StatMethodsDAO.findByStatMethod(stat_method)

          //TODO: find in text if the assumption is present, otherwise ask only a single general question.
          StatMethod2AssumptionDAO.findByStatMethodId(statMethod.get.id).foreach(e => {

            //start the AssessmentProcess and wait for Answer for each question (this is also a collectDecide process)
            Assumption2QuestionsDAO.findByAssumptionId(e.assumption_id).foreach(b => {


                  val converged = v.createProcess[CrowdSAQuery, Answer]("assessmentProcess").process(
                    new CrowdSAQuery(new HCompQuery {
                      override def question: String = b.question
                      override def title: String = b.id.toString
                      override def suggestedPaymentCents: Int = 10
                    }, new CrowdSAQueryProperties(paper_id, "Boolean",
                      new Highlight("Dataset", convergedAnswer.answer.replaceAll("#", ",")), 10, ((new Date().getTime()/1000) + 1000*60*60*24*365), 100)))
                  logger.debug("Assessment step for question: " + b.question + " converged to answer: " + converged.answer)

            })
          })
    })

    "End recombination variant"
  }

  override def allRecombinationKeys: List[String] = List("discoveryProcess")

  private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
    def createDiscovery = decorated.createProcess[CrowdSAQuery, Answer]("discoveryProcess")
  }

}