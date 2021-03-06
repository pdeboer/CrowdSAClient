package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
 * Created by mattia on 06.03.15.
 */

@PPLibProcess
class DiscoveryProcess(_params: Map[String, Any] = Map.empty)
  extends CreateProcess[CrowdSAPatch, CrowdSAPatch](_params) with HCompPortalAccess with InstructionHandler{

  /**
   * Run the Discovery Process which ask to identify the dataset of a statistical method.
   * @param data
   * @return The converged answer
   */
  override protected def run(data: CrowdSAPatch): CrowdSAPatch = {

    val processType = DiscoveryProcess.DISCOVERY_PROCESS.get

    val lowerPriorityParams = params

    val process = processType.create(lowerPriorityParams)
    process.process(data)
  }

  override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(DiscoveryProcess.DISCOVERY_PROCESS)
}

object DiscoveryProcess {
  val DISCOVERY_PROCESS = new ProcessParameter[PassableProcessParam[CreateProcess[CrowdSAPatch, CrowdSAPatch]]]("discoveryProcess", None)
}
