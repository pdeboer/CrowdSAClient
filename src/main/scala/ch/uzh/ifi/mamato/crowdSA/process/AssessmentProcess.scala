package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAQuery
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.process.{CreateProcess, PPLibProcess}
import ch.uzh.ifi.pdeboer.pplib.process.parameter._

/**
 * Created by mattia on 18.03.15.
 */

@PPLibProcess
class AssessmentProcess(_params: Map[String, Any] = Map.empty)
  extends CreateProcess[CrowdSAQuery, Answer](_params) {

  override protected def run(data: CrowdSAQuery): Answer = {
    val processType = AssessmentProcess.ASSESSMENT_PROCESS.get

    val lowerPriorityParams = params

    val process = processType.create(lowerPriorityParams)
    process.process(data)
  }

  override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(AssessmentProcess.ASSESSMENT_PROCESS)
}

object AssessmentProcess {
  val ASSESSMENT_PROCESS = new ProcessParameter[PassableProcessParam[CrowdSAQuery, Answer]]("assessmentProcess", None)
}
