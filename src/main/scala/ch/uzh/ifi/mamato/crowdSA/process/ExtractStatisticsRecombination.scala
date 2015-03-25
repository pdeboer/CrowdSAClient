package ch.uzh.ifi.mamato.crowdSA.process

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAQuery
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.process.stdlib._
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTupleStringified, HCompQueryProperties, HCompInstructionsWithTuple}
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{InstructionData, DefaultParameters, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationVariantGenerator, TypedParameterVariantGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithBeatByKVotingProcess, Contest}

/**
 * Created by mattia on 27.02.15.
 */
object ExtractStatisticsRecombination {

  def createVotingProcesses(): List[PassableProcessParam[_, _]] = new TypedParameterVariantGenerator[CrowdSAContest]()
    .generatePassableProcesses[List[Answer], Answer] :::
    new TypedParameterVariantGenerator[CrowdSAContestWithBeatByKVotingProcess]()
      .generatePassableProcesses[List[Answer], Answer] :::
    new TypedParameterVariantGenerator[CrowdSAContestWithStatisticalReductionProcess]()
      .addVariation(DefaultParameters.SHUFFLE_CHOICES, List(false))
      .addVariation(CrowdSAContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER, List(0.85))
      .generatePassableProcesses[List[Answer], Answer]


  def createCollectionProcesses() = new TypedParameterVariantGenerator[CrowdSACollectionWithSigmaPruning]()
    .addVariation(CrowdSACollectionWithSigmaPruning.PRUNE_TEXT_LENGTH, List(false))
    .addVariation(DefaultParameters.WORKER_COUNT, List(5,7))
    .generatePassableProcesses[CrowdSAQuery, List[Answer]] ::: new TypedParameterVariantGenerator[CrowdSACollection]()
    .addVariation(DefaultParameters.WORKER_COUNT, List(5,7))
    .generatePassableProcesses[CrowdSAQuery, List[Answer]]


  def recombinations = {

    val collectDecide: List[PassableProcessParam[_, _]] = new TypedParameterVariantGenerator[CrowdSACollectDecideProcess]()
      .addVariation(CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_COLLECT, List(false))
      .addVariation(CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_DECIDE, List(false))
      .addVariation(CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_PARAMETER, List(None))
      .addVariation(CrowdSACollectDecideProcess.COLLECT, createCollectionProcesses())
      .addVariation(CrowdSACollectDecideProcess.DECIDE, createVotingProcesses())
      .generatePassableProcesses[CrowdSAQuery, Answer]

    val iterativeRefinement: List[PassableProcessParam[_, _]] = new TypedParameterVariantGenerator[CrowdSAIterativeRefinementProcess]()
      .addVariation(CrowdSAIterativeRefinementProcess.VOTING_PROCESS_TYPE, createVotingProcesses())
      .generatePassableProcesses[CrowdSAQuery, Answer]

    /*
    new TypedParameterVariantGenerator[ListScaleProcess]()
					.addVariation(ListScaleProcess.CHILD_PROCESS, collectDecide ::: iterativeRefinement)
					,
					new TypedParameterVariantGenerator[FindFixPatchProcess]()
						.addVariation(FindFixPatchProcess.FIND_PROCESS,
							new TypedParameterVariantGenerator[ContestWithMultipleEqualWinnersProcess]()
								.addVariation(ContestWithMultipleEqualWinnersProcess.QUESTION, List(HCompInstructionsWithTupleStringified("Please select the paragraphs in the following list that are erroneous, have spelling / punctuation / grammar mistakes and should be improved.")))
								.addVariation(ContestWithMultipleEqualWinnersProcess.PRICE_PER_VOTE, List(HCompQueryProperties(10)))
								.addVariation(ContestWithMultipleEqualWinnersProcess.WORKERS_TO_ASK_PER_ITEM, List(5))
								.addVariation(ContestWithMultipleEqualWinnersProcess.THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM, List(2)).generatePassableProcesses[List[Patch], List[Patch]])
						.addVariation(FindFixPatchProcess.FIX_PROCESS,

							////// IR
							new TypedParameterVariantGenerator[FixPatchProcess]()
								.addVariation(FixPatchProcess.FIXER_PROCESS, iterativeRefinement).generatePassableProcesses[List[Patch], List[Patch]]
								:::
								/////CONTEST
								new TypedParameterVariantGenerator[FixPatchProcess]()
									.addVariation(FixPatchProcess.FIXER_PROCESS, collectDecide).generatePassableProcesses[List[Patch], List[Patch]]
						),


					//DP
					new TypedParameterVariantGenerator[DualPathwayProcess]()
						.addVariation(DualPathwayProcess.CHUNK_COUNT_TO_INCLUDE, List(1))
						.addVariation(DualPathwayProcess.QUESTION_NEW_PROCESSED_ELEMENT, List(HCompInstructionsWithTupleStringified("Please fix any problems you see in the paragraph below", questionAfterTuples = "Your refinement should incorporate grammar, punctuation, coherence and text-sophistication")))
						.addVariation(DualPathwayProcess.QUESTION_OLD_PROCESSED_ELEMENT, List(HCompInstructionsWithTupleStringified("Please check if the paragraph below is fixed correctly in terms of grammar, punctuation, coherence and text-sophistication. If it's not, please fix it yourself", questionBetweenTuples = "Candidate answer")))
						.addVariation(DualPathwayProcess.QUESTION_PER_COMPARISON_TASK, List(new DPHCompDriverDefaultComparisonInstructionsConfig(preText = "Please compare Paragraph 1 and Paragraph 2 and check if they are of equal content as well as equal in terms of grammar, coherence and text-sophistication", leftTitle = "Paragraph 1", rightTitle = "Paragraph 2")))
     */

    val candidateProcessParameters = Map(
      "discoveryProcess" -> List(
        new TypedParameterVariantGenerator[DiscoveryProcess]()
          .addVariation(DiscoveryProcess.DISCOVERY_PROCESS, collectDecide ::: iterativeRefinement)
      ),
      "assessmentProcess" -> List(
        new TypedParameterVariantGenerator[AssessmentProcess]()
        .addVariation(AssessmentProcess.ASSESSMENT_PROCESS, createCollectionProcesses())
      )
    )

    val candidateProcesses = candidateProcessParameters.map {
      case (key, generators) => (key, generators.map(_.generatePassableProcesses()).flatten)
    }

    new RecombinationVariantGenerator(candidateProcesses).variants
  }

}