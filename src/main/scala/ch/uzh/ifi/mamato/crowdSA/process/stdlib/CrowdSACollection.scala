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
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())

    val tmpAnswers = new mutable.MutableList[Answer]

    val answers: List[Answer] = memoizer.mem("answer_line_" + query) {

      val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query)

      val postTime = new DateTime()
      val sleep = ConfigFactory.load("application.conf").getLong("pollTimeMS")

      while (CrowdSACollection.WORKER_COUNT.get > tmpAnswers.length){
        logger.debug("Needed answers: " + CrowdSACollection.WORKER_COUNT.get + " - Got so far: " + tmpAnswers.length)

        Thread.sleep(sleep)

        val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
        answerzz.foreach(e => {
          if(tmpAnswers.filter(_.id == e.id).length == 0 && CrowdSACollection.WORKER_COUNT.get >= tmpAnswers.length+1){
            logger.debug("Adding answer: " + e)
            e.postTime = postTime
            e.receivedTime = new DateTime()
            AnswersDAO.create(e)
            tmpAnswers += e
            val budget = Main.crowdSA.budget
            Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get-query.getQuery().suggestedPaymentCents))
          }
        })
      }
      logger.debug("Disabling question because got enough answers")
      CrowdSAPortalAdapter.service.DisableQuestion(question_id)
      tmpAnswers.toList
    }
    tmpAnswers.toList
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(CrowdSACollection.WORKER_COUNT) ::: super.optionalParameters
}
object CrowdSACollection {
  val WORKER_COUNT = new ProcessParameter[Int]("worker_count", Some(List(3)))
}
