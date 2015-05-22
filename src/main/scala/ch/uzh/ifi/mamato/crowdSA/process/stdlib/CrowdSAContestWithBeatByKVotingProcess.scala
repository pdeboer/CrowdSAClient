package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQueryProperties, CrowdSAPortalAdapter, CrowdSAQuery}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.HighlightDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import org.joda.time.DateTime

import scala.util.Random

/**
 * Created by mattia on 23.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */
@PPLibProcess
class CrowdSAContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any])
  extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._
  import scala.collection.mutable

  protected var votes = mutable.HashMap.empty[String, Int]

  override protected def run(data: List[Patch]): Patch = {
    if (data.size == 1) data(0)
    else if (data.size == 0) null
    else {

      val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

      //TODO: Ugly need a fix
      var paperId: Long = -1
      if(data.head.auxiliaryInformation.get("answer").asInstanceOf[String] != "") {
        paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(data.head.auxiliaryInformation.get("id").asInstanceOf[Long])
      } else {
        paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(data(1).auxiliaryInformation.get("id").asInstanceOf[Long])
      }

      data.foreach(d => votes += (d.auxiliaryInformation.get("answer").asInstanceOf[String] -> 0))

      val choices = if (DefaultParameters.SHUFFLE_CHOICES.get) Random.shuffle(data.map{_.auxiliaryInformation.get("answer").asInstanceOf[String]}) else data.map{_.auxiliaryInformation.get("answer").asInstanceOf[String]}

      val termsHighlight = new mutable.MutableList[String]
      choices.foreach(a => termsHighlight += a)

      val query = new CrowdSAQuery("Please select the best answer", 10,
        new CrowdSAQueryProperties(paperId, "Voting",
          HighlightDAO.create("Dataset", termsHighlight.mkString("#"), "", -1), 10, 1000 * 60 * 60 * 24 * 365, 100,
          Some(data.map{_.auxiliaryInformation.get("answer").asInstanceOf[String]}.mkString("$$")), null)
      )

      val tmpAnswers = new mutable.MutableList[String]
      val tmpAnswers2 = new mutable.MutableList[Patch]
      var globalIteration: Int = 0

      val answers: List[Patch] = memoizer.mem("voting_" + tmpAnswers2.hashCode()) {

        logger.info("started first iteration ")
        val firstAnswer = portal.sendQueryAndAwaitResult(query, query.properties).get.is[Patch]
        firstAnswer.auxiliaryInformation += ("receivedTime" -> new DateTime())
        tmpAnswers2 += firstAnswer
        firstAnswer.auxiliaryInformation.get("answer").asInstanceOf[String].split("$$").foreach(b => {
          tmpAnswers += b
          votes += b -> votes.getOrElse(b, 0)
        })

        val question_id = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(firstAnswer.auxiliaryInformation.get("id").asInstanceOf[Long]).remote_question_id

        CrowdSAContestWithBeatByKVotingProcess.WORKER_COUNT.get.foreach( w =>
          while (shouldStartAnotherIteration) {
            logger.info("started iteration " + globalIteration)
            logger.debug("Needed answers: " + w + " - Got so far: " + tmpAnswers.length)
            Thread.sleep(5000)
            val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
            answerzz.foreach(e => {
              if (tmpAnswers2.filter(_.auxiliaryInformation.get("id").asInstanceOf[Long] == e.id).length == 0 && w >= tmpAnswers.length + 1) {
                println("Adding answer: " + e)
                e.receivedTime = new DateTime()
                val p = new Patch("")
                p.auxiliaryInformation += ("answer" -> e.answer)
                p.auxiliaryInformation += ("receivedTime" -> e.receivedTime)

                tmpAnswers2 += p
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

      //Adding answers that had 0 votes
      tmpAnswers2.foreach(d => votes += (d.auxiliaryInformation.get("answer").asInstanceOf[String] -> 0))

      val winner = bestAndSecondBest._1._1
      logger.info(s"beat-by-k finished after $globalIteration rounds. Winner: " + winner)
      data.find(_.auxiliaryInformation.get("answer").asInstanceOf[String] == winner).get
    }
  }

  def bestAndSecondBest = {
    logger.info("Votes: " + votes)
    val sorted = votes.toList.sortBy(-_._2)
    logger.info("Sorted: "+ sorted)
    (sorted(0), sorted(1))
  }

  def delta = if (votes.values.sum == 0) 3 else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

  def shouldStartAnotherIteration: Boolean = {
    delta < K.get && votes.values.sum + delta < DefaultParameters.MAX_ITERATIONS.get
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(DefaultParameters.SHUFFLE_CHOICES, DefaultParameters.MAX_ITERATIONS, K, DefaultParameters.INSTRUCTIONS_ITALIC)
}

object CrowdSAContestWithBeatByKVotingProcess {
  val K = new ProcessParameter[Int]("k", Some(List(2)))
  val WORKER_COUNT = new ProcessParameter[List[Int]]("worker_count", Some(Iterable(List(2))))
}

