package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAPortalAdapter, CrowdSAQuery}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.patterns.{CrowdSAIRDefaultHCompDriver, CrowdSAIterativeRefinementExecutor}
import ch.uzh.ifi.pdeboer.pplib.patterns.{IterativeRefinementExecutor, IRDefaultHCompDriver}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{GenericPassableProcessParam, ProcessParameter}
import org.joda.time.DateTime

/**
 * Created by mattia on 23.03.15.
 */
@PPLibProcess
class CrowdSAIterativeRefinementProcess(params: Map[String, Any] = Map.empty) extends CreateProcess[CrowdSAQuery, Answer](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._
  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.IterativeRefinementProcess._

  override protected def run(query: CrowdSAQuery): Answer = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())

    logger.info("started refinement process for query " + query)
    VOTING_PROCESS_TYPE.get.setParams(params, replace = false)

    val driver = new CrowdSAIRDefaultHCompDriver(portal, query.getQuery().question, query.getQuery().title, query.getProperties().paper_id, CrowdSAIterativeRefinementProcess.VOTING_PROCESS_TYPE.get, 10, query.hashCode.toString)

    //TODO: Create a valid empty answer (will be used to get the paper Id)
    val initAnswer = new Answer(-1, "", new Date().getTime, None, None, None, -1)
    initAnswer.receivedTime = new DateTime()

    val exec = new CrowdSAIterativeRefinementExecutor(initAnswer, query, driver, 20, memoizer, query.hashCode.toString)

    exec.refinedAnswer
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(VOTING_PROCESS_TYPE, STRING_DIFFERENCE_THRESHOLD, TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD)
}

object CrowdSAIterativeRefinementProcess {
  val VOTING_PROCESS_TYPE = new ProcessParameter[GenericPassableProcessParam[List[Answer], Answer, CreateProcess[List[Answer], Answer]]]("votingProcess", None)
  val STRING_DIFFERENCE_THRESHOLD = new ProcessParameter[Int]("iterationStringDifferenceThreshold", Some(List(1)))
  val TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = new ProcessParameter[Int]("toleratedNumberOfIterationsBelowThreshold", Some(List(2)))
}

