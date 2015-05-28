package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.{HighlightDAO, AnswersDAO}
import ch.uzh.ifi.pdeboer.pplib.hcomp.FreetextQuery
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */

@PPLibProcess
class CrowdSACollection(params: Map[String, Any] = Map.empty)
  extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler {

  override protected def run(query: Patch): List[Patch] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode()+"").getOrElse(new NoProcessMemoizer())

    val answers: List[Patch] = memoizer.mem("answer_line_" + query) {

      val q = FreetextQuery(query.auxiliaryInformation("question").asInstanceOf[String],
        query.auxiliaryInformation.getOrElse("possibleAnswers", Some("")).asInstanceOf[Option[String]].get
      )

      val prop = new CrowdSAQueryProperties(
        query.auxiliaryInformation("paperId").asInstanceOf[Long],
        query.auxiliaryInformation("type").asInstanceOf[String],
        HighlightDAO.create(query.auxiliaryInformation("assumption").asInstanceOf[String],
          query.auxiliaryInformation("terms").asInstanceOf[String],
          query.auxiliaryInformation.getOrElse("dataset", "").asInstanceOf[String],
          query.auxiliaryInformation.getOrElse("remoteQuestionId", "-1".toLong).asInstanceOf[Long]),
        query.auxiliaryInformation("rewardCts").asInstanceOf[Int],
        query.auxiliaryInformation("expirationTimeSec").asInstanceOf[Long],
        query.auxiliaryInformation("maxAssignments").asInstanceOf[Int],
        query.auxiliaryInformation.getOrElse("possibleAnswers", Some("")).asInstanceOf[Option[String]],
        query.auxiliaryInformation.getOrElse("deniedTurkers", null).asInstanceOf[Option[List[Long]]]
      )

      val question_id = CrowdSAPortalAdapter.service.CreateQuestion(q, prop)
      val postTime = new DateTime()
      var answerSoFar = new mutable.MutableList[Patch]

      //FIXME: This should be done by the PORTAL ADAPTER & manager
      while (CrowdSACollection.WORKER_COUNT.get > answerSoFar.length){

        Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))
        val allAnswers = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)

        if(allAnswers.size == CrowdSACollection.WORKER_COUNT.get){
          allAnswers.foreach(e => {
            if(CrowdSACollection.WORKER_COUNT.get >= answerSoFar.length+1){
              logger.debug("Adding answer: " + e)
              e.postTime = postTime
              e.receivedTime = new DateTime()
              val id = AnswersDAO.create(e).id

              val p = new Patch(e.answer)
              p.auxiliaryInformation += ("answer" -> e.answer, "id" -> id)
              answerSoFar += p

              // Accept all the answers which are not empty
              if(e.answer!= null && e.answer != ""){
                CrowdSAPortalAdapter.service.ApproveAnswer(e)
              } else if(e.answer != null && e.answer == "" && !e.is_method_used) {
                // Approve answers which have "no method used" marked
                CrowdSAPortalAdapter.service.ApproveAnswer(e)
              } else {
                // Reject only if the answer is empty and the method is known to be used
                CrowdSAPortalAdapter.service.RejectAnswer(e)
              }
              Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get -
                query.auxiliaryInformation("rewardCts").asInstanceOf[Int]))
            }
          })
        }

        logger.debug("Needed answers: " + CrowdSACollection.WORKER_COUNT.get + " - Got so far: " + answerSoFar.length)
      }

      logger.debug("Disabling question because got enough answers")
      CrowdSAPortalAdapter.service.DisableQuestion(question_id)
      answerSoFar.toList
    }
    answers
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(CrowdSACollection.WORKER_COUNT) :::
    super.optionalParameters
}
object CrowdSACollection {
  val WORKER_COUNT = new ProcessParameter[Int]("worker_count", Some(List(3)))
}
