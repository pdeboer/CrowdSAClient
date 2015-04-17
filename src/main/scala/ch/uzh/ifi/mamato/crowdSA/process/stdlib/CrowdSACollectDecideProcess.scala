package ch.uzh.ifi.mamato.crowdSA.process.stdlib

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAQuery
import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.pdeboer.pplib.process.entities._

import scala.collection.mutable

/**
 * This class is based on the example (Summarize Application) provided by Patrick de Boer
 * @param _params
 */

@PPLibProcess
class CrowdSACollectDecideProcess(_params: Map[String, Any] = Map.empty)
  extends CreateProcess[CrowdSAQuery, Answer](_params) {

  import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess._

  override def expectedParametersBeforeRun: List[ProcessParameter[_]] =
    List(CrowdSACollectDecideProcess.COLLECT, CrowdSACollectDecideProcess.DECIDE).asInstanceOf[List[ProcessParameter[_]]]


  override protected def run(data: CrowdSAQuery): Answer = {
    val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

    logger.info("Running collect-phase for query: " + data)
    //Collection step
    val collection: List[Answer] = memoizer.mem("collectProcess")(CrowdSACollectDecideProcess.COLLECT.get.process(data))

    val collectionTmpDistinct = new mutable.MutableList[String]
    val collectionDistinct = new mutable.MutableList[Answer]
    collection.foreach(f => {
      if(!collectionTmpDistinct.contains(f.answer)){
        collectionTmpDistinct += f.answer
        collectionDistinct += f
      }
    })
    logger.info(s"got ${collection.length} results. ${collectionDistinct.length} after pruning. Running decide process")
    if (CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_PARAMETER.get.isDefined)
      DECIDE.get.setParams(Map(
        CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_PARAMETER.get.get.key
          -> CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_MESSAGE.get), replace = true)

    //Decide step
    val res: Answer = memoizer.mem(getClass.getSimpleName + "decideProcess")(CrowdSACollectDecideProcess.DECIDE.get.process(collectionDistinct.toList))
    logger.info(s"Collect/decide for $res has finished with result $res")
    res
  }

  override def optionalParameters: List[ProcessParameter[_]] = List(CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_MESSAGE, CrowdSACollectDecideProcess.FORWARD_ANSWER_TO_DECIDE_PARAMETER, CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_COLLECT, CrowdSACollectDecideProcess.FORWARD_PARAMS_TO_DECIDE)
}

object CrowdSACollectDecideProcess {
  val FORWARD_PARAMS_TO_COLLECT = new ProcessParameter[Boolean]("forwardParamsToCollect", Some(List(true)))
  val FORWARD_PARAMS_TO_DECIDE = new ProcessParameter[Boolean]("forwardParamsToDecide", Some(List(true)))
  val COLLECT = new ProcessParameter[CreateProcess[CrowdSAQuery, List[Answer]]]("collect", None)
  val DECIDE = new ProcessParameter[DecideProcess[List[Answer], Answer]]("decide", None)
  val FORWARD_ANSWER_TO_DECIDE_PARAMETER = new ProcessParameter[Option[ProcessParameter[String]]]("forwardAnswerToDecideParameter", Some(List(None)))
  val FORWARD_ANSWER_TO_DECIDE_MESSAGE = new ProcessParameter[String]("forwardAnswerToDecideMessage", Some(List("Is the dataset identified correct?")))
}
