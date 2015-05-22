package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
 * Created by mattia on 06.03.15.
 */

@PPLibProcess
class MissingProcess(_params: Map[String, Any] = Map.empty)
  extends CreateProcess[Patch, Patch](_params) with HCompPortalAccess with InstructionHandler{

  /**
   * Run the Missing Process which ask to identify the non-automatically identified methods.
   * @param data
   * @return The converged answer
   */
  override protected def run(data: Patch): Patch = {

    val processType = MissingProcess.MISSING_PROCESS.get

    val lowerPriorityParams = params

    val process = processType.create(lowerPriorityParams)
    process.process(data)
  }

  override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(MissingProcess.MISSING_PROCESS)
}

object MissingProcess {
  val MISSING_PROCESS = new ProcessParameter[PassableProcessParam[CreateProcess[Patch, Patch]]]("MissingProcess", None)
}
