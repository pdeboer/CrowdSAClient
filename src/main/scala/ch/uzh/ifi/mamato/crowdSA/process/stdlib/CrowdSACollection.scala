package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAManager, CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process.CreateProcess
import ch.uzh.ifi.pdeboer.pplib.process.HCompPortalAccess
import ch.uzh.ifi.pdeboer.pplib.process.InstructionHandler
import ch.uzh.ifi.pdeboer.pplib.process.NoProcessMemoizer
import ch.uzh.ifi.pdeboer.pplib.process.PPLibProcess
import ch.uzh.ifi.pdeboer.pplib.process.ProcessMemoizer
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{Patch, ProcessParameter}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random


@PPLibProcess
class CrowdSACollection(params: Map[String, Any] = Map.empty) extends CreateProcess[CrowdSAQuery, List[Answer]](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._

  override protected def run(query: CrowdSAQuery): List[Answer] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())

    val tmpAnswers = new mutable.MutableList[Answer]

    val answers: List[Answer] = memoizer.mem("answer_line_" + query) {

      val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query)
      val postTime = new DateTime()

      while (WORKER_COUNT.get > tmpAnswers.length){
        logger.debug("Needed answers: " + WORKER_COUNT.get + " - Got so far: " + tmpAnswers.length)
        Thread.sleep(5000)
        val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
        answerzz.foreach(e => {
          if(tmpAnswers.filter(_.id == e.id).length == 0 && WORKER_COUNT.get >= tmpAnswers.length+1){
            logger.debug("Adding answer: " + e)
            e.postTime = postTime
            e.receivedTime = new DateTime()
            tmpAnswers += e
          }
        })
      }
      logger.debug("Disabling question because got enough answers")
      CrowdSAPortalAdapter.service.DisableQuestion(question_id)
      tmpAnswers.toList
    }
    tmpAnswers.toList
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(WORKER_COUNT) ::: super.optionalParameters
}
