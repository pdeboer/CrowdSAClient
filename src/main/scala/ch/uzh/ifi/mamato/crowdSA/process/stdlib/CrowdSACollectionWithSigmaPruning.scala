package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAPortalAdapter}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompAnswer
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by mattia on 23.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */

@PPLibProcess
class CrowdSACollectionWithSigmaPruning(params: Map[String, Any] = Map.empty)
  extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
  override protected def run(query: Patch): List[Patch] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())
    logger.info("running contest with sigma pruning for patch " + query)

    val tmpAnswers = new mutable.MutableList[Answer]

    val answers: List[Patch] = memoizer.mem("answer_line_" + query) {
      val q = query.auxiliaryInformation.get("CrowdSAQuery").asInstanceOf[CrowdSAQuery]
      val firstAnswer = portal.sendQueryAndAwaitResult(q, q.properties).get.is[Answer]

      val question_id = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(
        firstAnswer.id).remote_question_id
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
      var res = new mutable.MutableList[Patch]
      tmpAnswers.foreach(an => {
        val p = new Patch("")
        p.auxiliaryInformation += ("answer" -> an.answer)
        p.auxiliaryInformation += ("id" -> an.id)
        res += p
      })
      res.toList
    }

    val timeWithinSigma: List[Answer] = new SigmaPruner(CrowdSACollectionWithSigmaPruning.NUM_SIGMAS.get).prune(tmpAnswers.toList)
      logger.info(s"TIME MEASURE: pruned ${answers.size - timeWithinSigma.size} answers for patch " + query)

      val withinSigma = if (CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH.get) {
        val calc = new SigmaCalculator(timeWithinSigma.map(
          _.asInstanceOf[Patch].auxiliaryInformation.get("answer").asInstanceOf[String].length.toDouble),
          NUM_SIGMAS.get)
        timeWithinSigma.filter(a => {
          val fta = a.asInstanceOf[Patch].auxiliaryInformation.get("answer").asInstanceOf[String].length.toDouble
          fta >= calc.minAllowedValue && fta <= calc.maxAllowedValue
        })
      } else timeWithinSigma

      withinSigma.map(a => a.asInstanceOf[Patch]).toSet.toList
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(
    CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH,
    CrowdSACollectionWithSigmaPruning.NUM_SIGMAS, CrowdSACollectionWithSigmaPruning.WORKER_COUNT) ::: super.optionalParameters
}

object CrowdSACollectionWithSigmaPruning {
  val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
  val PRUNE_TEXT_LENGTH = new ProcessParameter[Boolean]("pruneByTextLength", Some(List(true)))
  val WORKER_COUNT = new ProcessParameter[List[Int]]("worker_count", Some(Iterable(List(3))))
}