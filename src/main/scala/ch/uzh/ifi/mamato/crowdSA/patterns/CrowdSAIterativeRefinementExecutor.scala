package ch.uzh.ifi.mamato.crowdSA.patterns

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQueryProperties, CrowdSAQuery}
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.HighlightDAO
import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns.IterationWatcher
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import org.joda.time.DateTime

/**
 * Created by mattia on 23.03.15.
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 */
class CrowdSAIterativeRefinementExecutor(val datasetToRefine: Patch,
                                        val query: Patch,
                                        val driver: CrowdSAIterativeRefinementDriver[Patch],
                                        val maxIterations: Int = 5,
                                        val memoizer: ProcessMemoizer = new NoProcessMemoizer(),
                                        val memoizerPrefix: String = "",
                                        val stringDifferenceThreshold: Int = 1,
                                        val toleratedNumberOfIterationsBelowThreshold: Int = 2) extends LazyLogger {
  assert(maxIterations > 0)

  var currentState: Patch = datasetToRefine
  protected val iterationWatcher = new IterationWatcher(datasetToRefine.auxiliaryInformation.get("answer").asInstanceOf[String],
    stringDifferenceThreshold, toleratedNumberOfIterationsBelowThreshold)

  def step(stepNumber: Int): Unit = {
    val newState : Patch = memoizer.mem(memoizerPrefix + "refinement" + stepNumber)(driver.refine(datasetToRefine, currentState, stepNumber))
    logger.info(s"asked crowd workers to refine '$currentState'. Answer was $newState <--")
    currentState = memoizer.mem(memoizerPrefix + "bestRefinement" + stepNumber)(driver.selectBestRefinement(List(currentState, newState)))
    iterationWatcher.addIteration(currentState.auxiliaryInformation.get("answer").asInstanceOf[String])
    logger.info("crowd workers determined the following state to be better: " + currentState)
  }

  lazy val refinedAnswer: Patch = {
    for (i <- 1 to maxIterations) {
      if (iterationWatcher.shouldRunAnotherIteration)
        step(i)
      else logger.info(s"ending IR early due to unchanging answer. Step $i")
    }
    currentState
  }
}

object CrowdSAIterativeRefinementExecutor {
  val DEFAULT_ITERATION_COUNT: Int = 5
  val DEFAULT_STRING_DIFFERENCE_THRESHOLD = 1
  val DEFAULT_TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = 2
}

trait CrowdSAIterativeRefinementDriver[Patch] {
  def refine(originalToRefine: Patch, refinementState: Patch, iterationId: Int): Patch

  def selectBestRefinement(candidates: List[Patch]): Patch
}

class CrowdSAIRDefaultHCompDriver(portal: HCompPortalAdapter, quest: String, stat_method: String, paperId: Long, votingProcessParam: PassableProcessParam[DecideProcess[List[Patch], Patch]], questionPricing: Int = 10, memoizerPrefix: String = "") extends CrowdSAIterativeRefinementDriver[Patch] {

  override def refine(originalTextToRefine: Patch, currentRefinementState: Patch, iterationId: Int): Patch = {

    var toHighlight = currentRefinementState.auxiliaryInformation.get("answer").asInstanceOf[String]
    toHighlight = toHighlight+"#"+stat_method

    val toRefine = currentRefinementState.auxiliaryInformation.get("answer").asInstanceOf[String]
    val query = new CrowdSAQuery(quest.replace("Identify", "Identify or refine"), 10,
      new CrowdSAQueryProperties(paperId, "Discovery", HighlightDAO.create("Dataset", toHighlight, "", -1), 10, 1000*60*60*24*365, 100, Some(toRefine), null)
    )

    val answer = portal.sendQueryAndAwaitResult(query, query.properties).get.is[Patch]
    answer.auxiliaryInformation += ("receivedTime" -> new DateTime())
    answer
  }

  override def selectBestRefinement(candidates: List[Patch]): Patch = {
    val candidatesDistinct = candidates.distinct

    val memPrefixInParams: String = votingProcessParam.getParam[Option[String]](
      DefaultParameters.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")

    val lowerPriorityParams = Map(DefaultParameters.PORTAL_PARAMETER.key -> portal)
    val higherPriorityParams = Map(DefaultParameters.MEMOIZER_NAME.key -> Some(memoizerPrefix.hashCode + "selectbest" + memPrefixInParams))

    val votingProcess = votingProcessParam.create(lowerPriorityParams, higherPriorityParams)
    votingProcess.process(candidates)
  }
}

object CrowdSAIRDefaultHCompDriver {
  val DEFAULT_TITLE_FOR_REFINEMENT: String = "Please refine the following dataset"
  val DEFAULT_QUESTION_FOR_REFINEMENT = "Other crowd workers have refined the dataset below.Please refine it further. We don't like REJECTIONS, but due to bad experiences we had to deploy a system that detects malicious / unchanged datasets and will reject your answer if it's deemed unhelpful by both, the software and afterwards a human - so please don't cheat."
  val DEFAULT_QUESTION_FOR_VOTING = "Other crowd workers have written the following refinements to the dataset below. Please select the one you like the best."
  val DEFAULT_TITLE_FOR_VOTING = "Choose the best representation for the dataset"
  val DEFAULT_WORKER_COUNT_FOR_VOTING = 3

  val DEFAULT_QUESTION_PRICE = 10
}
