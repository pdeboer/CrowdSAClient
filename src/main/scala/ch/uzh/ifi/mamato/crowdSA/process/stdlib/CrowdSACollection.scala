package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAManager, CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process.CreateProcess
import ch.uzh.ifi.pdeboer.pplib.process.HCompPortalAccess
import ch.uzh.ifi.pdeboer.pplib.process.InstructionHandler
import ch.uzh.ifi.pdeboer.pplib.process.NoProcessMemoizer
import ch.uzh.ifi.pdeboer.pplib.process.PPLibProcess
import ch.uzh.ifi.pdeboer.pplib.process.ProcessMemoizer
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{Patch, ProcessParameter}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._

import scala.util.Random


@PPLibProcess
class CrowdSACollection(params: Map[String, Any] = Map.empty) extends CreateProcess[CrowdSAQuery, List[Answer]](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._

  override protected def run(query: CrowdSAQuery): List[Answer] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())

    val answers: List[Answer] = memoizer.mem("answer_line_" + query) {
      val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
        portal.sendQueryAndAwaitResult(query.getQuery(), query.getProperties()).get.is[Answer]
      }).toList

      answers.map(_.is[Answer]).toSet.toList
    }
    answers
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(WORKER_COUNT) ::: super.optionalParameters
}
