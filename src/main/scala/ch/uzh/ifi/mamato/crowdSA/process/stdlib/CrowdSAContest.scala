package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.{HighlightDAO, AnswersDAO, QuestionDAO}
import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.collection.mutable

/**
 * Created by mattia on 10.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */
@PPLibProcess
class CrowdSAContest(params: Map[String, Any] = Map.empty[String, Any])
  extends DecideProcess[List[CrowdSAPatch], CrowdSAPatch](params) with HCompPortalAccess with InstructionHandler {

  protected var votes = mutable.HashMap.empty[String, Int]

  override def run(alternatives: List[CrowdSAPatch]): CrowdSAPatch = {

      if (alternatives.size == 0) null
      else if (alternatives.size == 1) alternatives.head
      else {
        val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

        var paperId: Long = -1

        // Special case for iterative refinement
        if(alternatives.head.answer != ""){
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(
            alternatives.head.answerId)
        } else {
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(
            alternatives(1).answerId)
        }

        // Get all distinct answers and the teams id which created this answer.
        // These teams will not be able to answer the voting question.
        var answersText = new mutable.MutableList[String]
        val teams = new mutable.MutableList[Long]

        alternatives.foreach(a => {
          //currentAns is a JSON object ["first sentence selected","second sentence selected"] or ["boolean value"] or []
          var currentAns = a.answer

          // Case: Method not used
          if(currentAns.isEmpty){
            val ans = AnswersDAO.find(a.answerId).get
            if(!ans.is_method_used){
              currentAns = "[\"Method is not used\"]"
            }
          }

          if(!answersText.contains(currentAns)){
            answersText += currentAns
            // Get the teams which participated to the create the answers
            // (used to exclude them from the voting process)
            teams += CrowdSAPortalAdapter.service.getAssignmentForAnswerId(
              a.answerId).remote_team_id

            // Init votes
            votes += currentAns -> 0
          }
        })

        // get assignment of the first alternative (All the alternatives belongs to the same question)
        val assignment = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(
          alternatives.head.answerId)

        // get question id of the first alternative
        val quest_id = assignment.remote_question_id
        // get original question to check if was a discovery or boolean question
        val originalQuestion = QuestionDAO.getByRemoteQuestionId(quest_id)
        // Find what was highlighted to help the user to answer the original question
        val originalQuestionHighlight = HighlightDAO.findByRemoteQuestionId(quest_id)

        // Ugly - check if the question which generates the alternatives was a discovery question
        val wasDiscoveryQuestion = originalQuestion.get.question contains "Method: "

        val originalTerms = originalQuestionHighlight.get.terms
        var possibleAnswers = ""
        var highlightToAdd = ""

        answersText.foreach(ans => {
          val p = Json.parse(ans).as[Seq[String]]
          possibleAnswers += ",["
          p.foreach(pp => {
            if(pp != "" && pp != " " && pp.length > 2){
              // If the answer should not be highlighted
              if(!pp.equalsIgnoreCase("[\"Method is not used\"]") ||
                !pp.equalsIgnoreCase("[\"There exists no dataset\"]") ){

                highlightToAdd += ",\""+pp+"\""
              }

              possibleAnswers += "\""+pp+"\""

              if(!p.last.equals(pp)){
                possibleAnswers += ","
              }else {
                possibleAnswers += "]"
              }
            }
          })
        })

        possibleAnswers += "]"
        possibleAnswers = possibleAnswers.substring(1,possibleAnswers.length)
        possibleAnswers = "["+possibleAnswers

        val toHighlight = originalTerms.substring(0, originalTerms.length-1) + highlightToAdd+"]"

        val question = if(wasDiscoveryQuestion){
          "The answers below were submitted by other crowd workers when asking to identify the dataset of '" + originalQuestion.get.question+ "'. Please chose the answer which you think best identifies the data used by this method."
        } else {
          "The answers below were submitted by other crowd workers when asking to answer the Yes/No question: '<i><u>"+ originalQuestion.get.question+ "</u></i>'. Please chose the answer which you think is the right one."
        }

        val query = FreetextQuery(question)

        val prop = new CrowdSAQueryProperties(paperId, "Voting",
          HighlightDAO.create("Dataset", toHighlight, "[]", -1),
          10,((new Date().getTime() / 1000) + 60 * 60 * 24 * 365), CrowdSAContest.WORKER_COUNT.get,
          Some(possibleAnswers), Some(teams.toList))


        val allAnswers = new mutable.MutableList[Answer]

        memoizer.mem("voting_"+allAnswers.hashCode()) {

          val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query, prop)
          val postTime = new DateTime()

          //FIXME: should be done by PortalAdapter and not here
          while (CrowdSAContest.WORKER_COUNT.get > allAnswers.length){
            Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))
            val answersSoFar = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
            answersSoFar.foreach(answer => {
              if (allAnswers.filter(_.id == answer.id).length == 0 && CrowdSAContest.WORKER_COUNT.get >= allAnswers.length+1) {
                logger.debug("Adding answer: " + answer)
                answer.postTime = postTime
                answer.receivedTime = new DateTime()
                AnswersDAO.create(answer)

                // Get the vote value for the answer choosen and add 1 vote
                votes += answer.answer -> (votes.getOrElse(answer.answer, 0)+1)

                allAnswers += answer
                // Accept all the answers which are not empty
                if(answer.answer!= null && answer.answer != "[]"){
                  CrowdSAPortalAdapter.service.ApproveAnswer(answer)
                } else {
                  CrowdSAPortalAdapter.service.RejectAnswer(answer)
                }
                // Keep track of the cost. This should be done by PPLib.
                // PDB: It's done by PPLib. Check out CostCountingEnabledHCompPortal
                Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get-query.suggestedPaymentCents))
              }
            })
          }
          logger.debug("COMPLETED: All answers to question: "+question_id+" correctly stored")


          logger.debug("Disabling question because got enough answers")
          CrowdSAPortalAdapter.service.DisableQuestion(question_id)
          allAnswers.toList
        }

        logger.info(votes.toList.toString())
        val valueOfAnswer: String = votes.maxBy(s => s._2)._1
        logger.info("***** WINNER CONTEST " + valueOfAnswer)
        // find empty alternative which is method used = false => Method is not used
        if(valueOfAnswer.equalsIgnoreCase("[\"Method is not used\"]")){
          alternatives.find(a => {
            a.answer.equalsIgnoreCase("[]") &&
              !AnswersDAO.find(a.answerId).get.is_method_used
          }).get
        }
        // find empty alternative with is method used = true => no dataset found
          // FIXME: can this really happen???
        else if(valueOfAnswer.equals("[]")){
          alternatives.find(a => {
            a.answer.equalsIgnoreCase("[]") &&
              AnswersDAO.find(a.answerId).get.is_method_used
          }).get
        }
        // default: Find the alternative with the same answer
        else {
          alternatives.find(_.answer.equalsIgnoreCase(valueOfAnswer)).get
        }
      }

    }

    override def optionalParameters: List[ProcessParameter[_]] =
      List(CrowdSAContest.WORKER_COUNT) ::: super.optionalParameters
  }

object CrowdSAContest {
  val WORKER_COUNT = new ProcessParameter[Int]("worker_count", Some(List(3)))
}
