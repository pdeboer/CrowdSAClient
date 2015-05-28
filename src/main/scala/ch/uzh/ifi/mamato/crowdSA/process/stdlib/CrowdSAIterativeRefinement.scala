package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAQueryProperties
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.patterns.{CrowdSAIRDefaultHCompDriver, CrowdSAIterativeRefinementExecutor}
import ch.uzh.ifi.mamato.crowdSA.persistence.HighlightDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.FreetextQuery
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import org.joda.time.DateTime

/**
 * Created by mattia on 23.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */
@PPLibProcess
class CrowdSAIterativeRefinementProcess(params: Map[String, Any] = Map.empty)
  extends CreateProcess[Patch, Patch](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.IterativeRefinementProcess._

  override protected def run(query: Patch): Patch = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())

    logger.info("started refinement process for query " + query)
    VOTING_PROCESS_TYPE.get.setParams(params, replace = false)

    val q = FreetextQuery(query.auxiliaryInformation("question").asInstanceOf[String],
      query.auxiliaryInformation.getOrElse("possibleAnswers", Some("")).asInstanceOf[Option[String]].get
    )
    val prop = new CrowdSAQueryProperties(
      query.auxiliaryInformation("paperId").asInstanceOf[Long],
      query.auxiliaryInformation("type").asInstanceOf[String],
      HighlightDAO.create(query.auxiliaryInformation("assumption").asInstanceOf[String],
        query.auxiliaryInformation("terms").asInstanceOf[String],
        query.auxiliaryInformation.getOrElse("dataset", "").asInstanceOf[String],
        query.auxiliaryInformation.getOrElse("remoteQuestionId", "-1".toLong).asInstanceOf[Long]),
      query.auxiliaryInformation("rewardCts").asInstanceOf[Int],
      query.auxiliaryInformation("expirationTimeSec").asInstanceOf[Long],
      query.auxiliaryInformation("maxAssignments").asInstanceOf[Int],
      query.auxiliaryInformation.getOrElse("possibleAnswers", Some("")).asInstanceOf[Option[String]],
      query.auxiliaryInformation.getOrElse("deniedTurkers", null).asInstanceOf[Option[List[Long]]]
    )

    val driver = new CrowdSAIRDefaultHCompDriver(portal, q.question, q.title, prop.paper_id,
      CrowdSAIterativeRefinementProcess.VOTING_PROCESS_TYPE.get, 10, query.hashCode.toString)

    // Create an empty answer (will be used to get the paper Id)
    val initAnswer = new Patch("")
    initAnswer.auxiliaryInformation += ("receivedTime" -> new DateTime())

    val exec = new CrowdSAIterativeRefinementExecutor(initAnswer, query, driver, 20, memoizer, query.hashCode.toString)

    exec.refinedAnswer
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(VOTING_PROCESS_TYPE, STRING_DIFFERENCE_THRESHOLD, TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD)
}

object CrowdSAIterativeRefinementProcess {
  val VOTING_PROCESS_TYPE = new ProcessParameter[PassableProcessParam[DecideProcess[List[Patch], Patch]]]("votingProcess", None)
  val STRING_DIFFERENCE_THRESHOLD = new ProcessParameter[Int]("iterationStringDifferenceThreshold", Some(List(1)))
  val TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = new ProcessParameter[Int]("toleratedNumberOfIterationsBelowThreshold", Some(List(2)))
}

