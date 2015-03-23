package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQueryProperties, CrowdSAPortalAdapter, CrowdSAQuery}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer}
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.ProcessParameter
import org.joda.time.DateTime

import scala.util.Random

/**
 * Created by mattia on 23.03.15.
 */
@PPLibProcess
class CrowdSAContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Answer], Answer](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._
  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._
  import scala.collection.mutable

  protected var votes = mutable.HashMap.empty[String, Int]

  override protected def run(data: List[Answer]): Answer = {
    if (data.size == 1) data(0)
    else if (data.size == 0) null
    else {

      val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

      val paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(data(0).id)

      data.foreach(d => votes += (d.answer -> 0))

      //TODO: useless?
      var ans = new mutable.MutableList[String]
      data.foreach(a => {
        if (!ans.contains(a.answer)) {
          ans += a.answer
        }
      })
      val choices = if (SHUFFLE_CHOICES.get) Random.shuffle(ans) else ans

      val termsHighlight = new mutable.MutableList[String]
      choices.foreach(a => termsHighlight += a.replace("#", ","))

      val query = new CrowdSAQuery(
        new HCompQuery {
          override def question: String = "Please select the best answer"

          override def title: String = "Voting"

          override def suggestedPaymentCents: Int = 10
        },
        new CrowdSAQueryProperties(paperId, "Voting", new Highlight("Dataset", termsHighlight.mkString(",")), 10, 1000 * 60 * 60 * 24 * 365, 5, Some(ans.mkString("$$")))
      )

      val tmpAnswers = new mutable.MutableList[String]
      val tmpAnswers2 = new mutable.MutableList[Answer]
      var globalIteration: Int = 0

      val answers: List[Answer] = memoizer.mem("voting_" + tmpAnswers2.hashCode()) {

        logger.info("started first iteration ")
        val firstAnswer = portal.sendQueryAndAwaitResult(query.getQuery(), query.getProperties()).get.is[Answer]
        firstAnswer.receivedTime = new DateTime()
        tmpAnswers2 += firstAnswer
        firstAnswer.answer.split("$$").foreach(b => tmpAnswers += b)

        val question_id = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(firstAnswer.id).remote_question_id

        CrowdSAContestWithBeatByKVotingProcess.WORKER_COUNT.get.foreach( w =>
          while (shouldStartAnotherIteration) {
            logger.info("started iteration " + globalIteration)
            println("Needed answers: " + w + " - Got so far: " + tmpAnswers.length)
            Thread.sleep(5000)
            val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
            answerzz.foreach(e => {
              if (tmpAnswers2.filter(_.id == e.id).length == 0 && w >= tmpAnswers.length + 1) {
                println("Adding answer: " + e)
                e.receivedTime = new DateTime()
                tmpAnswers2 += e
                e.answer.split("$$").foreach(b => {
                  tmpAnswers += b
                  votes += b -> votes.getOrElse(b, 0)
                })
              }
            })
            globalIteration += 1
          }
        )

        tmpAnswers2.toList
      }

      val winner = bestAndSecondBest._1._1
      logger.info(s"beat-by-k finished after $globalIteration rounds. Winner: " + winner)
      data.find(_.answer == winner).get
    }
  }

  def bestAndSecondBest = {
    val sorted = votes.toList.sortBy(-_._2)
    (sorted(0), sorted(1))
  }

  def delta = if (votes.values.sum == 0) 3 else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

  def shouldStartAnotherIteration: Boolean = {
    delta < K.get && votes.values.sum + delta < MAX_ITERATIONS.get
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(SHUFFLE_CHOICES, MAX_ITERATIONS, K, INSTRUCTIONS_ITALIC)
}

object CrowdSAContestWithBeatByKVotingProcess {
  val K = new ProcessParameter[Int]("k", Some(List(2)))
  val WORKER_COUNT = new ProcessParameter[List[Int]]("worker_count", Some(Iterable(List(3, 5))))
}

