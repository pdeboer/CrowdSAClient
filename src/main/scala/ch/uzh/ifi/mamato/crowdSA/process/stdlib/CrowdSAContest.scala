package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import ch.uzh.ifi.mamato.crowdSA.Main
import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer}
import ch.uzh.ifi.mamato.crowdSA.persistence.{AnswersDAO, QuestionDAO}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQuery, MultipleChoiceQuery, MultipleChoiceAnswer}
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{ProcessParameter, Patch}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.util.U
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random

/**
 * Created by mattia on 10.03.15.
 */
@PPLibProcess
class CrowdSAContest(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Answer], Answer](params) with HCompPortalAccess with InstructionHandler {

    override def run(alternatives: List[Answer]): Answer = {
      if (alternatives.size == 0) null
      else if (alternatives.size == 1) alternatives.head
      else {
        val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

        //TODO: useless?
        var paperId: Long = -1
        if(alternatives.head.answer != ""){
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(alternatives.head.id)
        } else {
          paperId = CrowdSAPortalAdapter.service.getPaperIdFromAnswerId(alternatives(1).id)
        }

        //TODO: useless?
        var ans = new mutable.MutableList[String]
        val teams = new mutable.MutableList[Long]
        alternatives.foreach(a => {
          if(!ans.contains(a.answer)){
            ans += a.answer
            teams += CrowdSAPortalAdapter.service.getAssignmentForAnswerId(a.id).remote_team_id
          }
        })

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

        val query = new CrowdSAQuery(
          new HCompQuery {
            override def question: String = "Please select the answer that you think best answers the question: '"+originalQuestion.get.question+"'"

            override def title: String = "Voting"

            override def suggestedPaymentCents: Int = 10
          },
          new CrowdSAQueryProperties(paperId, "Voting",
            new Highlight("Dataset", termsHighlight.mkString("#")),
            10, ((new Date().getTime()/1000) + 60*60*24*365),
            100, Some(ans.mkString("$$")), Some(teams.toList))
        )

        val tmpAnswers = new mutable.MutableList[String]
        val tmpAnswers2 = new mutable.MutableList[Answer]

        val answers: List[Answer] = memoizer.mem("voting_"+tmpAnswers2.hashCode()) {

          val question_id = CrowdSAPortalAdapter.service.CreateQuestion(query)

          val postTime = new DateTime()

          while (CrowdSAContest.WORKER_COUNT.get > tmpAnswers2.length){
            logger.debug("Needed answers: " + CrowdSAContest.WORKER_COUNT.get + " - Got so far: " + tmpAnswers.length)

            Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))

            val answerzz = CrowdSAPortalAdapter.service.GetAnswersForQuestion(question_id)
            answerzz.foreach(e => {
              if (tmpAnswers2.filter(_.id == e.id).length == 0 && CrowdSAContest.WORKER_COUNT.get >= tmpAnswers.length+1) {
                logger.debug("Adding answer: " + e)
                e.postTime = postTime
                e.receivedTime = new DateTime()
                AnswersDAO.create(e)
                tmpAnswers2 += e
                tmpAnswers += e.answer
                val budget = Main.crowdSA.budget
                Main.crowdSA.setBudget(Some(Main.crowdSA.budget.get-query.getQuery().suggestedPaymentCents))
              }
            })
          }

          logger.debug("Disabling question because got enough answers")
          CrowdSAPortalAdapter.service.DisableQuestion(question_id)

          tmpAnswers2.toList
        }

        val valueOfAnswer: String = tmpAnswers.groupBy(s => s).maxBy(s => s._2.size)._1
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
