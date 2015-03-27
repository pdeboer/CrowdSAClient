package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQueryProperties, CrowdSAQuery, CrowdSAPortalAdapter}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.ProcessParameter
import ch.uzh.ifi.pdeboer.pplib.util.{U, MonteCarlo}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random

/**
 * Created by mattia on 23.03.15.
 */

@PPLibProcess
class CrowdSACollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends CreateProcess[CrowdSAQuery, List[Answer]](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._
  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
  override protected def run(query: CrowdSAQuery): List[Answer] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())
    logger.info("running contest with sigma pruning for patch " + query)

    val tmpAnswers = new mutable.MutableList[Answer]

    val answers: List[Answer] = memoizer.mem("answer_line_" + query) {
      val firstAnswer = portal.sendQueryAndAwaitResult(query.getQuery(), query.getProperties()).get.is[Answer]

      val question_id = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(firstAnswer.id).remote_question_id
      tmpAnswers += firstAnswer
      CrowdSACollectionWithSigmaPruning.WORKER_COUNT.get.foreach(w => {
        while (w > tmpAnswers.length) {
          println("Needed answers: " + w + " - Got so far: " + tmpAnswers.length)
          Thread.sleep(5000)
          val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
          answerzz.foreach(e => {
            if (tmpAnswers.filter(_.id == e.id).length == 0 && w >= tmpAnswers.length + 1) {
              println("Adding answer: " + e)
              e.receivedTime = new DateTime()
              tmpAnswers += e
            }
          })
        }
        tmpAnswers.toList
      })
      tmpAnswers.toList
    }

    val timeWithinSigma: List[Answer] = new SigmaPruner(CrowdSACollectionWithSigmaPruning.NUM_SIGMAS.get).prune(tmpAnswers.toList)
      logger.info(s"TIME MEASURE: pruned ${answers.size - timeWithinSigma.size} answers for patch " + query)

      val withinSigma = if (CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH.get) {
        val calc = new SigmaCalculator(timeWithinSigma.map(_.is[Answer].answer.length.toDouble), NUM_SIGMAS.get)
        timeWithinSigma.filter(a => {
          val fta = a.is[Answer].answer.length.toDouble
          fta >= calc.minAllowedValue && fta <= calc.maxAllowedValue
        })
      } else timeWithinSigma

      withinSigma.map(a => a.is[Answer]).toSet.toList
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(
    CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH,
    CrowdSACollectionWithSigmaPruning.NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CrowdSACollectionWithSigmaPruning {
  val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
  val PRUNE_TEXT_LENGTH = new ProcessParameter[Boolean]("pruneByTextLength", Some(List(true)))
  val WORKER_COUNT = new ProcessParameter[List[Int]]("worker_count", Some(Iterable(List(2))))
}