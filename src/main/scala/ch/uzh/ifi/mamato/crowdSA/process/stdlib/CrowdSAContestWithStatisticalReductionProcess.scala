package ch.uzh.ifi.mamato.crowdSA.process.stdlib


import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQueryProperties, CrowdSAQuery}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer}
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.ProcessParameter
import ch.uzh.ifi.pdeboer.pplib.util.{U, MonteCarlo}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random


/**
 * Created by mattia on 10.03.15.
 */

@PPLibProcess
class CrowdSAContestWithStatisticalReductionProcess(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Answer], Answer](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._
  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess._

  protected val MONTECARLO_ITERATIONS: Int = 100000
  protected var votesCast = scala.collection.mutable.Map.empty[String, Int]

  override protected def run(data: List[Answer]): Answer = {
    if (data.size == 0) null
    else if (data.size == 1) data.head
    else {

      var answerId: Long = -1
      if(data.head.answer != ""){
        answerId = data.head.id
      } else {
        answerId = data(1).id
      }

      //All the answers of the same question asked
      val stringData = data.map(_.is[Answer].answer)
      val answers = new mutable.MutableList[String]
      data.foreach(a => {
        answers += a.is[Answer].answer
      })

      val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
      var iteration: Int = 0
      //Init votes to 0 for all answers
      data.foreach(d => votesCast += (d.answer -> 0))
      iteration += 1
      val choices: List[Answer] = memoizer.mem("it" + iteration)(castVote(answers.toList, iteration, answerId))

      val winner = itemWithMostVotes._1
      logger.info(s"contest with statistical reduction finished after $iteration rounds. Winner: $winner")
      data.find(d => (d.is[Answer].answer == winner)).get
    }
  }

  def itemWithMostVotes: (String, Int) = {
    votesCast.maxBy(_._2)
  }

  protected def minVotesForAgreement(data: List[String]): Option[Int] = {
    MonteCarlo.minAgreementRequired(data.size, votesCast.values.sum, confidence, MONTECARLO_ITERATIONS)
  }

  def castVote(choices: List[String], iteration: Int, answerId: Long): List[Answer] = {

    var ans = new mutable.MutableList[String]
    choices.foreach(a => {
      if (!ans.contains(a)) {
        ans += a
      }
    })

    val alternatives = if (SHUFFLE_CHOICES.get) Random.shuffle(ans.toList) else ans.toList

    val service = CrowdSAPortalAdapter.service
    val paperId = service.getPaperIdFromAnswerId(answerId)
    val query = new CrowdSAQuery(
      new HCompQuery {
        override def question: String = "Please select the answer that best represent the dataset"

        override def title: String = "Voting"

        override def suggestedPaymentCents: Int = 10
      },
      new CrowdSAQueryProperties(paperId, "Voting",
        null,
        10, 1000 * 60 * 60 * 24 * 365, 100, Some(alternatives.mkString("$$")))
    )

    val tmpAnswers = new mutable.MutableList[String]
    val tmpAnswers2 = new mutable.MutableList[Answer]
    var globalIteration: Int = 0

    val memoizer: ProcessMemoizer = getProcessMemoizer(choices.hashCode() + "").getOrElse(new NoProcessMemoizer())

    val answers: List[Answer] = memoizer.mem("voting_" + tmpAnswers2.hashCode()) {

      logger.info("started first iteration ")
      val firstAnswer = portal.sendQueryAndAwaitResult(query.getQuery(), query.getProperties()).get.is[Answer]
      firstAnswer.receivedTime = new DateTime()
      tmpAnswers2 += firstAnswer
      firstAnswer.answer.split("$$").foreach(b => {
        votesCast += b -> (votesCast.getOrElse(b, 0) + 1)
        tmpAnswers += b
      })
      val question_id = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(firstAnswer.id).remote_question_id


        while (minVotesForAgreement(choices).getOrElse(Integer.MAX_VALUE) > itemWithMostVotes._2 && votesCast.values.sum < MAX_ITERATIONS.get) {
          logger.info("started iteration " + globalIteration)
          Thread.sleep(5000)
          val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
          answerzz.foreach(e => {
            if (tmpAnswers2.filter(_.id == e.id).length == 0) {
              println("Adding answer: " + e)
              e.receivedTime = new DateTime()
              tmpAnswers2 += e
              e.answer.split("$$").foreach(b => {
                tmpAnswers += b
                votesCast += b -> (votesCast.getOrElse(b, 0) + 1)
              })
            }
          })
          globalIteration += 1
        }
      tmpAnswers2.toList
    }
    tmpAnswers2.toList
  }

  protected def confidence = CrowdSAContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER.get

  override val processCategoryNames: List[String] = List("selectbest.statistical")


  override def optionalParameters: List[ProcessParameter[_]] =
    List(CrowdSAContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER,
      SHUFFLE_CHOICES,
      MAX_ITERATIONS) ::: super.optionalParameters
}

object CrowdSAContestWithStatisticalReductionProcess {
  val CONFIDENCE_PARAMETER = new ProcessParameter[Double]("confidence", Some(List(0.9d, 0.95d, 0.99d)))
  val WORKER_COUNT = new ProcessParameter[List[Int]]("worker", Some(Iterable(List(2))))
}