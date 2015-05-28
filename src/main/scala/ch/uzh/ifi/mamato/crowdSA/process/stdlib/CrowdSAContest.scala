package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.{HighlightDAO, AnswersDAO, QuestionDAO}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by mattia on 10.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */
@PPLibProcess
class CrowdSAContest(params: Map[String, Any] = Map.empty[String, Any])
  extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler {

  protected var votes = mutable.HashMap.empty[String, Int]

  override def run(alternatives: List[Patch]): Patch = {

      if (alternatives.size == 0) null
      else if (alternatives.size == 1) alternatives.head
      else {
        val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

        var paperId: Long = -1

        // Special case for iterative refinement
        if(alternatives.head.auxiliaryInformation("answer").asInstanceOf[String] != ""){
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(
            alternatives.head.auxiliaryInformation("id").asInstanceOf[Long])
        } else {
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(
            alternatives(1).auxiliaryInformation("id").asInstanceOf[Long])
        }

        // Get all distinct answers and the teams id which created this answer.
        // These teams will not be able to answer the voting question.
        var answersText = new mutable.MutableList[String]
        val teams = new mutable.MutableList[Long]

        alternatives.foreach(a => {
          if(!answersText.contains(a.auxiliaryInformation("answer").asInstanceOf[String])){
            answersText += a.auxiliaryInformation("answer").asInstanceOf[String]
            // Get the teams which participated to the create the answers
            // (used to exclude them from the voting process)
            teams += CrowdSAPortalAdapter.service.getAssignmentForAnswerId(
              a.auxiliaryInformation("id").asInstanceOf[Long]).remote_team_id

          }
        })

        // get assignment of the first alternative (All the alternatives belongs to the same question)
        val assignment = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(
          alternatives.head.auxiliaryInformation("id").asInstanceOf[Long])
        // get question id of the first alternative
        val quest_id = assignment.remote_question_id
        // get original question to check if was a discovery or boolean question
        val originalQuestion = QuestionDAO.getByRemoteQuestionId(quest_id)
        // Find what was highlighted to help the user to answer the original question
        val originalQuestionHighlight = HighlightDAO.findByRemoteQuestionId(quest_id)

        // Ugly - check if the question which generates the alternatives was a discovery question
        val wasDiscoveryQuestion = originalQuestion.get.question contains "Method: "
        val toHighlight = originalQuestionHighlight.get.terms+"#"+answersText.mkString("#")

        val question = if(wasDiscoveryQuestion){
          "The answers below were submitted by other crowd workers when asking to identify the dataset of '" + originalQuestion.get.question+ "'. Please chose the answer which you think best identifies the data used by this method."
        } else {
          "The answers below were submitted by other crowd workers when asking to answer the Yes/No question: '<i><u>"+ originalQuestion.get.question+ "</u></i>'. Please chose the answer which you think is the right one."
        }

       /* val p = new Patch(question)
        p.auxiliaryInformation += (
          "question" -> question,
          "type" -> "Voting",
          "terms" -> toHighlight,
          "paperId" -> paperId,
          "rewardCts" -> 10,
          "expirationTimeSec" -> ((new Date().getTime() / 1000) + 60 * 60 * 24 * 365),
          "assumption" -> "Voting",
          "possibleAnswers" -> Some(answersText.mkString("$$")),
          "maxAssignments" -> 100,
          "maxAssignments" -> Some(teams.toList)
          )
          */

        val query = FreetextQuery(question, answersText.mkString("$$"))
        val prop = new CrowdSAQueryProperties(paperId, "Voting",
          HighlightDAO.create("Dataset", toHighlight, "", -1),
          10,((new Date().getTime() / 1000) + 60 * 60 * 24 * 365), 100,
          Some(answersText.mkString("$$")), Some(teams.toList))


        val allAnswers = new mutable.MutableList[Answer]

        memoizer.mem("voting_"+allAnswers.hashCode()) {

          val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query, prop)
          val postTime = new DateTime()

          //FIXME: should be done by PPLib and not here
          while (CrowdSAContest.WORKER_COUNT.get > allAnswers.length){
            Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))
            val answersSoFar = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
            answersSoFar.foreach(answer => {
              if (allAnswers.filter(_.id == answer.id).length == 0 && CrowdSAContest.WORKER_COUNT.get >= allAnswers.length+1) {
                logger.debug("Adding answer: " + answer)
                answer.postTime = postTime
                answer.receivedTime = new DateTime()
                AnswersDAO.create(answer)
                votes.+=(answer.answer -> votes.getOrElse(answer.answer, 0))

                allAnswers.+=(answer)
                // Accept all the answers which are not empty
                if(answer.answer!= null && answer.answer != ""){
                  CrowdSAPortalAdapter.service.ApproveAnswer(answer)
                } else {
                  CrowdSAPortalAdapter.service.RejectAnswer(answer)
                }
                // Keep track of the cost. This should be done by PPLib.
                // PDB: It's done by PPLib. Check out CostCountingEnabledHCompPortal
                Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get-query.suggestedPaymentCents))
              }
            })
            logger.debug("Needed answers: " + CrowdSAContest.WORKER_COUNT.get + " - Got so far: " + answersSoFar.toList.length)
          }

          logger.debug("Disabling question because got enough answers")
          CrowdSAPortalAdapter.service.DisableQuestion(question_id)
          allAnswers.toList
        }

        val valueOfAnswer: String = votes.maxBy(s => s._2)._1
        logger.info("***** WINNER CONTEST " + valueOfAnswer)
        alternatives.find(_.auxiliaryInformation("answer").asInstanceOf[String] == valueOfAnswer).get
      }

    }

    override def optionalParameters: List[ProcessParameter[_]] =
      List(CrowdSAContest.WORKER_COUNT) ::: super.optionalParameters
  }

object CrowdSAContest {
  val WORKER_COUNT = new ProcessParameter[Int]("worker_count", Some(List(3)))
}
