package ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf

import java.util.Date

import ch.uzh.ifi.mamato.crowdPdf.model.{Answer, Assignment, Question}
import ch.uzh.ifi.mamato.crowdPdf.persistence.QuestionDAO
import ch.uzh.ifi.mamato.crowdPdf.util.{HttpRestClient, LazyLogger}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpEntity, NameValuePair}
import org.apache.http.client.methods.{HttpPost, HttpGet}
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.util.parsing.json.JSON

/**
 * Created by Mattia on 20.01.2015.
 */
private[crowdPdf] class Server(val url: String)

private[crowdPdf] case object CrowdPDfServer extends Server("http://localhost:9000")

private[crowdPdf]class CrowdPdfService (val server: Server) extends LazyLogger{

  val httpClient = HttpRestClient.httpClient
  val config1 = RequestConfig.custom().setSocketTimeout((30*1000).toInt).build();

  /**
   * Get REST method in the server
   * @param path
   * @return
   */
  def get(path: String): String = {
    val httpGet = new HttpGet(server.url + path)
    httpGet.setConfig(config1)
    val closeableHttp = httpClient.execute(httpGet)
    val res = getResultFromEntity(closeableHttp.getEntity)
    if(closeableHttp != null) closeableHttp.close()
    res
  }

  /**
   * Post to a REST method in the server some parameters
   * @param path
   * @param params
   * @return
   */
  def post(path: String, params: List[NameValuePair]): String = {

    val httpPost = new HttpPost(server.url + path)
    httpPost.setConfig(config1);

    val reqEntity = MultipartEntityBuilder.create()
    for(n <- params){
      reqEntity.addPart(n.getName, new StringBody(n.getValue, ContentType.TEXT_PLAIN))
    }

    httpPost.setEntity(reqEntity.build())
    val ctx = HttpClientContext.create()
    val closeableHttp = httpClient.execute(httpPost, ctx)

    val res = getResultFromEntity(closeableHttp.getEntity)
    if(closeableHttp != null) closeableHttp.close()
    res
  }

  def getResultFromEntity(entity: HttpEntity): String = {
    val result = EntityUtils.toString(entity)
    result
  }

  def RegisterQuestionType(
                       Title: String,
                       Description: String,
                       Reward: Int,
                       AssignmentDurationInSeconds: Int,
                       Keywords: Seq[String],
                       AutoApprovalDelayInSeconds: Int,
                       QualificationRequirements: Seq[QualificationRequirement]): String = {

    ???
  }

  def GetAnswersForQuestion(qId: Long) : Iterable[Answer] = {
    try {

      val jsonAnswer: JsValue = Json.parse(get("/answers/"+qId))

      val a = jsonAnswer.validate[List[Answer]]
      return a.get.toIterable
    } catch {
      case e: Exception =>
        //If no answer is present we land always here
    }
    return None
  }

  def CreateQuestion(
                 Question: String,
                 QuestionType: String,
                 Reward: Int,
                 CreatedAt: Long,
                 Paper_fk: Long): Long = {

                 //LifetimeInSeconds: Int,
                 //MaxAssignments: Int,
                 //RequesterAnnotation: Option[String] = None): Long = {

    //check if paper exists
    val isUploaded = get("/checkPaper/"+Paper_fk).toBoolean

    try {
      if(isUploaded) {
        // The paper exists
        //post the question and get remote id
        val date = (new Date()).getTime
        val params = new collection.mutable.MutableList[NameValuePair]
        params += new BasicNameValuePair("question", Question)
        params += new BasicNameValuePair("questionType", QuestionType)
        params += new BasicNameValuePair("reward", Reward.toString)
        params += new BasicNameValuePair("created", date.toString)
        params += new BasicNameValuePair("paper_fk", Paper_fk.toString)


        val question_id = post("/addQuestion", params.toList).toLong
        logger.debug("Question posted with remote id: " + question_id)

        //create question object and store it in the local DB
        val qId = QuestionDAO.create(Question, QuestionType, Reward, date, Paper_fk, question_id)

        if(qId > 0) {
          logger.debug("Question stored in local DB with id: " + qId)
        } else {
          logger.error("Cannot store question in the local database.")
        }
        //return the id of remote question
        return question_id
      } else {
        //The paper doesn't exist in the server
        logger.error("There is no paper with id: " + Paper_fk + " stored in the server")
        -1
      }
    } catch {
      case e: Exception => {
        logger.error("An error occurred while creating new question: " + Question)
        -1
      }
    }
  }

  def DisableQuestion(qId: Long): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("questionId", qId.toString)

      logger.debug(post("/disablequestion", params.toList))
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def SearchQuestions(): Iterable[Question] = {
    ???
  }

  def GetAssignmentsForQuestion(questionId: Long): Iterable[Assignment] = {
    ???
  }

  def ApproveAnswer(a: Answer): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("answerId", a.id.toString)
      params += new BasicNameValuePair("accepted", "true")
      params += new BasicNameValuePair("bonus", "false")
      logger.debug(post("/evaluateanswer", params.toList))
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def RejectAnswer(a: Answer): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("answerId", a.id.toString)
      params += new BasicNameValuePair("accepted", "false")
      params += new BasicNameValuePair("bonus", "false")
      logger.debug(post("/evaluateanswer", params.toList))
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def BlockTurker(turkerId: String, reason: String): Unit = {
    ???
  }

  /*
  def ExtendQuestionMaxAssignments(hit: HIT, MaxAssignmentsIncrement: Int): Unit = {
    ???
  }


  def ExtendQuestionExpiration(hit: HIT, ExpirationIncrementInSeconds: Int): Unit = {
    ???
  }
  */

}

private[crowdPdf] case class QualificationRequirement(
                                                    QualificationTypeId: String,
                                                    Comparator: String,
                                                    RequiredToPreview: Boolean = false,
                                                    extraParameters: Seq[(String, String)] = Seq.empty) {
  ???
                                                    }

