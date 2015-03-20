package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.{Highlight, Answer}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQuery, MultipleChoiceQuery, MultipleChoiceAnswer}
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{ProcessParameter, Patch}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.collection.mutable
import scala.util.Random

/**
 * Created by mattia on 10.03.15.
 */
@PPLibProcess
class CrowdSAContest(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Answer], Answer](params) with HCompPortalAccess with InstructionHandler {

    import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._

    override def run(alternatives: List[Answer]): Answer = {
      if (alternatives.size == 0) null
      else if (alternatives.size == 1) alternatives(0)
      else {
        val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

        val crowdSA = HComp.apply("crowdSA")
        val service = crowdSA.asInstanceOf[CrowdSAPortalAdapter].service
        val paperId = service.getPaperIdFromAnswerId(alternatives(0).id)

        //TODO: useless?
        var ans = new mutable.MutableList[String]
        alternatives.foreach(a => {
          if(!ans.contains(a.answer)){
            ans += a.answer
          }
        })

        val termsHighlight = new mutable.MutableList[String]
        ans.foreach(a => termsHighlight += a.replace("#", ","))

        val query = new CrowdSAQuery(
          new HCompQuery {
            override def question: String = "Please select the answers that best represent the dataset"

            override def title: String = "Voting"

            override def suggestedPaymentCents: Int = 10
          },
          new CrowdSAQueryProperties(paperId, "Voting",new Highlight("Dataset", termsHighlight.mkString(",")), 10, 1000*60*60*24*365, 5, Some(ans.mkString("$$")))
        )

        val answers = new mutable.MutableList[String]
        getCrowdWorkers(WORKER_COUNT.get).map(w =>
          memoizer.mem("it" + w)(
            //U.retry(2) {
              portal.sendQueryAndAwaitResult(query.getQuery(),
                query.getProperties())
              match {
                case Some(a: Answer) => {
                  a.answer.split("$$").foreach(b => answers += b)
                  a.answer
                }
                case _ => {
                  logger.info(s"${getClass.getSimpleName} didn't get answer for query. retrying..")
                  throw new IllegalStateException("didnt get any response")
                }
              }
            //}
          ))

        val valueOfAnswer: String = answers.groupBy(s => s).maxBy(s => s._2.size)._1
        logger.info("got answer " + valueOfAnswer)
        alternatives.find(_.answer == valueOfAnswer).get
      }

    }

    override def optionalParameters: List[ProcessParameter[_]] =
      List(WORKER_COUNT) ::: super.optionalParameters
  }