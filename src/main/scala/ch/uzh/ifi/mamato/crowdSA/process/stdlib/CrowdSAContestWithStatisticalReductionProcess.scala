package ch.uzh.ifi.mamato.crowdSA.process.stdlib


import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQueryProperties, CrowdSAQuery}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer}
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.ProcessParameter
import ch.uzh.ifi.pdeboer.pplib.util.{U, MonteCarlo}

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
    else if (data.size == 1) data(0)
    else {
      //All the answers of the same question asked
      val stringData = data.map(_.is[Answer].answer)
      val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
      var iteration: Int = 0
      //Init votes to 0 for all answers
      data.foreach(d => votesCast += (d.is[Answer].answer -> 0))

      do {
        iteration += 1
        val choice: String = memoizer.mem("it" + iteration)(castVote(stringData.distinct, iteration, data(0).id))
        choice.split("#").foreach(c => {
          votesCast += c -> (votesCast.getOrElse(c, 0) + 1)
        })
      } while (minVotesForAgreement(stringData).getOrElse(Integer.MAX_VALUE) > itemWithMostVotes._2 && votesCast.values.sum < MAX_ITERATIONS.get)

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

  def castVote(choices: List[String], iteration: Int, answerId: Long): String = {
    val alternatives = if (SHUFFLE_CHOICES.get) Random.shuffle(choices) else choices
    val crowdSA = HComp.apply("crowdSA")
    val service = crowdSA.asInstanceOf[CrowdSAPortalAdapter].service
    val paperId = service.getPaperIdFromAnswerId(answerId)
    val query = new CrowdSAQuery(
      new HCompQuery {
        override def question: String = "Please select the answers that best represent the dataset"

        override def title: String = "Voting"

        override def suggestedPaymentCents: Int = 10
      },
      new CrowdSAQueryProperties(paperId, "Voting",new Highlight("Dataset", alternatives.distinct.mkString(",")), 10, 1000*60*60*24*365, 5, Some(alternatives.distinct.mkString("#")))
    )

    U.retry(3) {
      portal.sendQueryAndAwaitResult(query.getQuery(),
        query.getProperties()
      )
      match {
        case Some(a: Answer) => a.answer
        case _ => {
          logger.info(getClass.getSimpleName + " didnt get a vote when asked for it.")
          throw new IllegalStateException("didnt get any response")
        }
     }
    }
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
}