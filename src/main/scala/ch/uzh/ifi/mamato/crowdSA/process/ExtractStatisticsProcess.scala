package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer, Dataset}
import ch.uzh.ifi.mamato.crowdSA.persistence.{StatMethod2AssumptionDAO, Assumption2QuestionsDAO, StatMethodsDAO, AssumptionsDAO}
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
  extends Recombinable[String] {

  override def runRecombinedVariant(v: RecombinationVariant): String = {
    println("running variant " + new SimpleRecombinationVariantXMLExporter(v).xml)

    val processes = new ProcessVariant(v)

    // From each discovery question we will get at the end only one answer.
    // This answers is the convergence of all the answers
    discoveryQuestion.foreach {
      d =>
        val paper_id = d.getProperties().paper_id
        val convergedAnswer = v.createProcess[CrowdSAQuery, Answer]("discoveryProcess").process(d)
        println("***** result DISCOVERY STEP")
        //Create the dataset!
        CrowdSAPortalAdapter.service.createDataset(convergedAnswer.id)
        // Start the second phase of the process
        // Once we have defined a dataset we can start to ask the questions for the assumptions.

        //get the questions to be asked (assumption for each statistical method)
        val stat_method = d.query.question.substring(d.query.question.indexOf(": ")+2, d.query.question.indexOf(" highlighted"))
        val statMethod = StatMethodsDAO.findByStatMethod(stat_method)
        StatMethod2AssumptionDAO.findByStatMethodId(statMethod.get.id).foreach(e => {
          val questions = Assumption2QuestionsDAO.findByAssumptionId(e.assumption_id)
          //start the AssessmentProcess and wait for Answer for each question (this is also a collectDecide process)
          questions.foreach(b => {
            val c = new CrowdSAQuery(new HCompQuery {
              override def question: String = b.question
              override def title: String = b.id.toString
              override def suggestedPaymentCents: Int = 10
            }, new CrowdSAQueryProperties(paper_id, "Boolean",
              new Highlight("Dataset", convergedAnswer.answer.replaceAll("#", ",")), 10, 365*24*60*60*1000, 3))

            val res1 = v.createProcess[CrowdSAQuery, List[Answer]]("assessmentProcess").process(c)
            println("***** result ASSESSMENT STEP")
            res1.foreach(r =>
              println("Assessment step for question: "+ b.question +" converged to answer: " + r.answer)
            )
          })
        })
    }

    "End recombination variant"
  }

  override def allRecombinationKeys: List[String] = List("discoveryProcess")

  private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
    def createDiscovery = decorated.createProcess[CrowdSAQuery, Answer]("discoveryProcess")
  }

}