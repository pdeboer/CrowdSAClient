package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAPortalAdapter, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.{Answer, Dataset}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{IndexedPatch, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{SimpleRecombinationVariantXMLExporter, RecombinationVariant, Recombinable}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithStatisticalReductionProcess, FixPatchProcess}
import ch.uzh.ifi.pdeboer.pplib.util.U

/**
 * Created by mattia on 27.02.15.
 */

class ExtractStatisticsProcess(crowdSA: CrowdSAPortalAdapter, val discoveryQuestion:List[CrowdSAQuery])
  extends Recombinable[String] {

  override def runRecombinedVariant(v: RecombinationVariant): String = {
    println("running variant " + new SimpleRecombinationVariantXMLExporter(v).xml)

    val processes = new ProcessVariant(v)

    discoveryQuestion.foreach{
      d =>

        val discovery: ProcessStub[CrowdSAQuery, Answer] = processes.createDiscovery

        val listPatch = discovery.process(d)

        val res2 = v.createProcess[CrowdSAQuery, Answer]("discoveryProcess").process(d)


        println("***** result Discovery process")
        //Create the dataset!
        crowdSA.service.createDataset(res2.id)

        //Start the second phase of the process

    }
    "End recombination variant"
  }

  override def allRecombinationKeys: List[String] = List("discoveryProcess")

  private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
    def createDiscovery = decorated.createProcess[CrowdSAQuery, Answer]("discoveryProcess")
  }

}