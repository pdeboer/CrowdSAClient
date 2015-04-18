package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.AnswersDAO
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */

@PPLibProcess
class CrowdSACollection(params: Map[String, Any] = Map.empty) extends CreateProcess[CrowdSAQuery, List[Answer]](params) with HCompPortalAccess with InstructionHandler {

  override protected def run(query: CrowdSAQuery): List[Answer] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode()+"").getOrElse(new NoProcessMemoizer())

    val answers: List[Answer] = memoizer.mem("answer_line_" + query) {

      val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query)
      val postTime = new DateTime()
      var answerSoFar = new mutable.MutableList[Answer]

      //FIXME: This should be done by PPLib
      while (CrowdSACollection.WORKER_COUNT.get > answerSoFar.length){
        Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))
        val allAnswers = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
        allAnswers.foreach(e => {
          if(answerSoFar.filter(_.id == e.id).length == 0 && CrowdSACollection.WORKER_COUNT.get >= answerSoFar.length+1){
            logger.debug("Adding answer: " + e)
            e.postTime = postTime
            e.receivedTime = new DateTime()
            AnswersDAO.create(e)
            answerSoFar.+=(e)

            // Accept all the answers which are not empty
            if(e.answer!= null && e.answer != ""){
              CrowdSAPortalAdapter.service.ApproveAnswer(e)
            } else {
              CrowdSAPortalAdapter.service.RejectAnswer(e)
            }
            Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get-query.getQuery().suggestedPaymentCents))
          }
        })
        logger.debug("Needed answers: " + CrowdSACollection.WORKER_COUNT.get + " - Got so far: " + answerSoFar.length)

      }
      logger.debug("Disabling question because got enough answers")
      CrowdSAPortalAdapter.service.DisableQuestion(question_id)
      answerSoFar.toList
    }
    answers
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(CrowdSACollection.WORKER_COUNT) ::: super.optionalParameters
}
object CrowdSACollection {
  val WORKER_COUNT = new ProcessParameter[Int]("worker_count", Some(List(3)))
}
