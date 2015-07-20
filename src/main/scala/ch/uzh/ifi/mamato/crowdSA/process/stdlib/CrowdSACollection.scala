package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.persistence.{HighlightDAO, AnswersDAO}
import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
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
  extends CreateProcess[CrowdSAPatch, List[CrowdSAPatch]](params) with HCompPortalAccess with InstructionHandler {

  override protected def run(query: CrowdSAPatch): List[CrowdSAPatch] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode()+"").getOrElse(new NoProcessMemoizer())

    val answers: List[CrowdSAPatch] = memoizer.mem("answer_line_" + query) {

      val q = FreetextQuery(query.question)

      var terms = ""
      if(query.questionType.equalsIgnoreCase("Missing")){
        val mmm = query.methodList
        terms += "["
        mmm.foreach(m => {
          terms += "{\"method\":\""+m._1+"\",\"matches\":[\""+m._2.mkString("\",\"")+"\"]}"
          if(mmm.last != m){
            terms += ","
          }
        })
        terms += "]"
      } else {
        terms += query.terms
      }

      val prop = new CrowdSAQueryProperties(
        query.paperId,
        query.questionType,
        HighlightDAO.create(query.assumption,
          terms,
          query.dataset,
          query.remoteQuestionId),
        query.rewardCts,
        query.expirationTimeSec,
        CrowdSACollection.WORKER_COUNT.get,
        query.possibleAnswers,
        query.deniedTurkers
      )

      val question_id = CrowdSAPortalAdapter.service.CreateQuestion(q, prop)
      val postTime = new DateTime()
      var answerSoFar = new mutable.MutableList[CrowdSAPatch]

      //FIXME: This should be done by the PORTAL ADAPTER & manager
      while (CrowdSACollection.WORKER_COUNT.get > answerSoFar.length){

        Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))
        val allAnswers = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)

        // If we got enough answers we can start adding them to the client database.
        if(allAnswers.size >= CrowdSACollection.WORKER_COUNT.get){
          allAnswers.foreach(e => {
            if(CrowdSACollection.WORKER_COUNT.get >= answerSoFar.length+1){
              logger.debug("Adding answer: " + e)
              e.postTime = postTime
              e.receivedTime = new DateTime()
              val id = AnswersDAO.create(e).id

              val p = query.duplicate("")
              p.answer = e.answer
              p.answerId = id
              answerSoFar += p

              // Accept all the answers which are not empty
              if(e.answer!= null && e.answer != "[]"){
                CrowdSAPortalAdapter.service.ApproveAnswer(e)
              } else if(e.answer != null && e.answer == "[]" && !e.is_method_used) {
                // Approve answers which have "no method used" marked
                CrowdSAPortalAdapter.service.ApproveAnswer(e)
              } else {
                // Reject only if the answer is empty and the method is known to be used
                CrowdSAPortalAdapter.service.RejectAnswer(e)
              }
              Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get -
                query.rewardCts))
            }
          })
          logger.debug("COMPLETED: All answers to question: "+query.value+" correctly stored")
        }
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
