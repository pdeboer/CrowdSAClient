package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAQueryProperties, CrowdSAPortalAdapter}
import ch.uzh.ifi.mamato.crowdSA.process.stdlib.{CrowdSACollectionWithSigmaPruning, CrowdSACollectDecideProcess}
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{InstructionData, DefaultParameters, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationVariantGenerator, TypedParameterVariantGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithBeatByKVotingProcess, Contest}

/**
 * Created by mattia on 27.02.15.
 */
object ExtractStatisticsRecombination {

  def createVotingProcesses(): List[PassableProcessParam[_, _]] = new TypedParameterVariantGenerator[Contest]()
    .generatePassableProcesses[List[HCompAnswer], HCompAnswer] :::
    new TypedParameterVariantGenerator[ContestWithBeatByKVotingProcess]()
      .addVariation(ContestWithBeatByKVotingProcess.K, List(2))
      .generatePassableProcesses[List[HCompAnswer], HCompAnswer]

  def createCollectionProcesses(question: HCompInstructionsWithTuple, title: String, payment: Int = 10) = new TypedParameterVariantGenerator[CrowdSACollectionWithSigmaPruning]()
    .addVariation(CrowdSACollectionWithSigmaPruning.NUM_SIGMAS, List(3))
    .addVariation(CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH, List(false))
    .addVariation(DefaultParameters.WORKER_COUNT, List(2))
    .addVariation(DefaultParameters.INSTRUCTIONS, List(new InstructionData()))
    .addVariation(DefaultParameters.QUESTION_PRICE, List(HCompQueryProperties(payment)))
    .generatePassableProcesses[CrowdSAQuery, List[HCompAnswer]]


  def recombinations = {
    val collectDecide: List[PassableProcessParam[_, _]] = new TypedParameterVariantGenerator[CrowdSACollectDecideProcess]()
      .addVariation(CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_COLLECT, List(false))
      .addVariation(CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_DECIDE, List(false))
      .addVariation(CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_PARAMETER, List(None))
      .addVariation(CrowdSACollectDecideProcess.COLLECT, createCollectionProcesses(HCompInstructionsWithTupleStringified("Please identify the dataset of the statistical method highlighted in the paper.", questionAfterTuples = ""), "Discovery Question"))
      .addVariation(CrowdSACollectDecideProcess.DECIDE, createVotingProcesses())
      .generatePassableProcesses[CrowdSAQuery, HCompAnswer]

    val candidateProcessParameters = Map(
      "discoveryProcess" -> List(
        new TypedParameterVariantGenerator[DiscoveryProcess]()
          .addVariation(DiscoveryProcess.DISCOVERY_PROCESS, collectDecide)
      )
    )

    val candidateProcesses = candidateProcessParameters.map {
      case (key, generators) => (key, generators.map(_.generatePassableProcesses()).flatten)
    }

    new RecombinationVariantGenerator(candidateProcesses).variants
  }

}