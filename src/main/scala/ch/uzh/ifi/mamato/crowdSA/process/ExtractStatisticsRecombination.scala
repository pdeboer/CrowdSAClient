package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.process.stdlib._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationVariantGenerator, TypedParameterVariantGenerator}

/**
 * Created by mattia on 27.02.15.
 */
object ExtractStatisticsRecombination {

  def createVotingProcesses() =
    new TypedParameterVariantGenerator[CrowdSAContest]()
    .addVariation(CrowdSAContest.WORKER_COUNT, List(3))
    .generatePassableProcesses() /* :::
    new TypedParameterVariantGenerator[CrowdSAContestWithBeatByKVotingProcess]()
      .generatePassableProcesses[List[Answer], Answer] :::
      new TypedParameterVariantGenerator[CrowdSAContestWithStatisticalReductionProcess]()
      .addVariation(DefaultParameters.SHUFFLE_CHOICES, List(false))
      .addVariation(CrowdSAContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER, List(0.85))
      .generatePassableProcesses[List[Answer], Answer]
    */

  def createCollectionProcesses() = new TypedParameterVariantGenerator[CrowdSACollection]()
    .addVariation(CrowdSACollection.WORKER_COUNT, List(2))
    .generatePassableProcesses()/* ::: new TypedParameterVariantGenerator[CrowdSACollectionWithSigmaPruning]()
    .addVariation(CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH, List(false))
    .addVariation(DefaultParameters.WORKER_COUNT, List(2))
    .generatePassableProcesses[CrowdSAQuery, List[Answer]] */


  def recombinations = {

    val collectDecide =
      new TypedParameterVariantGenerator[CrowdSACollectDecideProcess]()
      .addVariation(CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_COLLECT, List(true))
      .addVariation(CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_DECIDE, List(true))
      .addVariation(CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_PARAMETER, List(None))
      .addVariation(CrowdSACollectDecideProcess.COLLECT, createCollectionProcesses())
      .addVariation(CrowdSACollectDecideProcess.DECIDE, createVotingProcesses())
      .generatePassableProcesses()

    /*
    val iterativeRefinement: List[PassableProcessParam[_, _]] = new TypedParameterVariantGenerator[CrowdSAIterativeRefinementProcess]()
      .addVariation(CrowdSAIterativeRefinementProcess.VOTING_PROCESS_TYPE, createVotingProcesses())
      .generatePassableProcesses[CrowdSAQuery, Answer]
    */

    // Hack to solve the .flatten problem
    val candidateProcessParameters = Map(
      (
        "discoveryProcess", new TypedParameterVariantGenerator[DiscoveryProcess]()
          .addVariation(DiscoveryProcess.DISCOVERY_PROCESS, collectDecide)
          .generatePassableProcesses()
      )
    ,
      (
        "assessmentProcess", new TypedParameterVariantGenerator[AssessmentProcess]()
          .addVariation(AssessmentProcess.ASSESSMENT_PROCESS, collectDecide)
          .generatePassableProcesses()
      )
    ,
      (
        "missingProcess", new TypedParameterVariantGenerator[MissingProcess]()
        .addVariation(MissingProcess.MISSING_PROCESS, createCollectionProcesses())
        .generatePassableProcesses()
      )
    )

    val candidateProcesses: Map[String, List[PassableProcessParam[_ <: ProcessStub[_, _]]]] =
      candidateProcessParameters.map {
        case (key, generators) => (key, generators)
    }

    new RecombinationVariantGenerator(candidateProcesses).variants
  }

}