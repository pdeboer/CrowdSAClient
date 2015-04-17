package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.{HighlightDAO, AnswersDAO, QuestionDAO}
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by mattia on 10.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */
@PPLibProcess
class CrowdSAContest(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Answer], Answer](params) with HCompPortalAccess with InstructionHandler {

  protected var votes = mutable.HashMap.empty[String, Int]

  override def run(alternatives: List[Answer]): Answer = {
      if (alternatives.size == 0) null
      else if (alternatives.size == 1) alternatives.head
      else {
        val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

        var paperId: Long = -1
        //TODO: useless?
        if(alternatives.head.answer != ""){
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(alternatives.head.id)
        } else {
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(alternatives(1).id)
        }

        // Get all distinct answers and the teams id which created this answer.
        // These teams will not be able to answer the voting question.
        var ans = new mutable.MutableList[String]
        val teams = new mutable.MutableList[Long]
        alternatives.foreach(a => {
          if(!ans.contains(a.answer)){
            ans += a.answer
            teams += CrowdSAPortalAdapter.service.getAssignmentForAnswerId(a.id).remote_team_id
          }
        })

        // Add each answer present in the voting list to the highlighting term list
        val termsHighlight = new mutable.MutableList[String]
        ans.foreach(a => {
          termsHighlight += a
        })

        //1 get assignment
        val assignment = CrowdSAPortalAdapter.service.getAssignmentForAnswerId(alternatives.head.id)
        //2 retrieve question id
        val quest_id = assignment.remote_question_id
        //3 get question
        val originalQuestion = QuestionDAO.getByRemoteQuestionId(quest_id)

        val originalQuestionHighlight = HighlightDAO.findByRemoteQuestionId(quest_id)

        val query = new CrowdSAQuery(
          new HCompQuery {
            override def question: String = if(originalQuestion.get.question contains "Method: "){
              "The answers below were submitted by other crowd workers when asking to identify the dataset of '" + originalQuestion.get.question+ "'. Please chose the answer which you think best identifies the dataset."
            } else {
              "The answers below were submitted by other crowd workers when asking to answer the Yes/No question: '<i><u>"+ originalQuestion.get.question+ "</u></i>'. Please chose the answer which you think is the right one."
            }

            override def title: String = "Voting"

            override def suggestedPaymentCents: Int = 10
          },
          new CrowdSAQueryProperties(paperId, "Voting",
            HighlightDAO.create("Dataset", originalQuestionHighlight.get.terms.replaceAll(",", "#")+"#"+termsHighlight.mkString("#"), -1),
            10, ((new Date().getTime()/1000) + 60*60*24*365),
            100, Some(ans.mkString("$$")), Some(teams.toList))
        )

        val answersValues = new mutable.MutableList[String]
        val allAnswers = new mutable.MutableList[Answer]

        val answers: List[Answer] = memoizer.mem("voting_"+allAnswers.hashCode()) {

          val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query)
          val postTime = new DateTime()

          //TODO: Fix me (should be done by PPLib)
          while (CrowdSAContest.WORKER_COUNT.get > allAnswers.length){
            Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))
            val answersSoFar = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
            answersSoFar.foreach(e => {
              if (allAnswers.filter(_.id == e.id).length == 0 && CrowdSAContest.WORKER_COUNT.get >= allAnswers.length+1) {
                logger.debug("Adding answer: " + e)
                e.postTime = postTime
                e.receivedTime = new DateTime()
                AnswersDAO.create(e)

                votes.+=(e.answer -> votes.getOrElse(e.answer, 0))

                allAnswers.+=(e)
                answersValues += e.answer
                if(e.answer!= null && e.answer != ""){
                  CrowdSAPortalAdapter.service.ApproveAnswer(e)
                } else {
                  CrowdSAPortalAdapter.service.RejectAnswer(e)
                }
                Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get-query.getQuery().suggestedPaymentCents))
              }
            })
            logger.debug("Needed answers: " + CrowdSAContest.WORKER_COUNT.get + " - Got so far: " + answersSoFar.toList.length)
          }

          logger.debug("Disabling question because got enough answers")
          CrowdSAPortalAdapter.service.DisableQuestion(question_id)

          allAnswers.toList
        }

        logger.debug("Votes: " + votes)
        val valueOfAnswer: String = votes.maxBy(s => s._2)._1
        logger.info("***** WINNER CONTEST " + valueOfAnswer)
        alternatives.find(_.answer == valueOfAnswer).get
      }

    }

    override def optionalParameters: List[ProcessParameter[_]] =
      List(CrowdSAContest.WORKER_COUNT) ::: super.optionalParameters
  }

object CrowdSAContest {
  val WORKER_COUNT = new ProcessParameter[Int]("worker_count", Some(List(3)))
}
