package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAQueryProperties}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process.CreateProcess
import ch.uzh.ifi.pdeboer.pplib.process.HCompPortalAccess
import ch.uzh.ifi.pdeboer.pplib.process.InstructionHandler
import ch.uzh.ifi.pdeboer.pplib.process.NoProcessMemoizer
import ch.uzh.ifi.pdeboer.pplib.process.PPLibProcess
import ch.uzh.ifi.pdeboer.pplib.process.ProcessMemoizer
import ch.uzh.ifi.pdeboer.pplib.process.parameter.ProcessParameter
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._


@PPLibProcess
class CrowdSACollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends CreateProcess[CrowdSAQuery, List[HCompAnswer]](params) with HCompPortalAccess with InstructionHandler {

  import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._

  override protected def run(query: CrowdSAQuery): List[HCompAnswer] = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(query.hashCode() + "").getOrElse(new NoProcessMemoizer())
    logger.info("running contest with sigma pruning for query " + query)

    val answerTextsWithinSigmas: List[HCompAnswer] = memoizer.mem("answer_line_" + query) {
      val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {

        portal.sendQueryAndAwaitResult(query.getQuery(), query.getProperties()).get.is[HCompAnswer]
      }).toList

      val timeWithinSigma: List[HCompAnswer] = new SigmaPruner(CrowdSACollectionWithSigmaPruning.NUM_SIGMAS.get).prune(answers)
      logger.info(s"TIME MEASURE: pruned ${answers.size - timeWithinSigma.size} answers for question: " + query.query.question)

      val withinSigma = if (CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH.get) {
        val calc = new SigmaCalculator(timeWithinSigma.map(_.is[Answer].answer.length.toDouble), CrowdSACollectionWithSigmaPruning.NUM_SIGMAS.get)
        timeWithinSigma.filter(a => {
          val fta = a.is[Answer].answer.length.toDouble
          fta >= calc.minAllowedValue && fta <= calc.maxAllowedValue
        })
      } else timeWithinSigma

      withinSigma.map(a => a.is[Answer]).toSet.toList
    }
    answerTextsWithinSigmas//.map(a => query.duplicate(a))
  }


  override def optionalParameters: List[ProcessParameter[_]] = List(CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH, CrowdSACollectionWithSigmaPruning.NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CrowdSACollectionWithSigmaPruning {
  val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
  val PRUNE_TEXT_LENGTH = new ProcessParameter[Boolean]("pruneByTextLength", Some(List(true)))
}
