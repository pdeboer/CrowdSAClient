package ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa

import java.io.File
import java.nio.charset.Charset
import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.model.{Answer, Assignment, Dataset, Question}
import ch.uzh.ifi.mamato.crowdSA.persistence.{HighlightDAO, PaperDAO, QuestionDAO}
import ch.uzh.ifi.mamato.crowdSA.util.{HttpRestClient, LazyLogger}
import com.typesafe.config.ConfigFactory
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPost}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpEntity, NameValuePair}
import play.api.libs.json.{JsValue, Json}

/**
 * Created by Mattia on 20.01.2015.
 */
private[crowdSA] class Server(val url: String)

private[crowdSA] case object CrowdSAServer extends Server(ConfigFactory.load("application.conf").getString("crowdSAHost"))

private[crowdSA]class CrowdSAService (val server: Server) extends LazyLogger{

  val httpClient = HttpRestClient.httpClient
  val config1 = RequestConfig.custom().setSocketTimeout((30*1000).toInt).build()

  /**
   * Get REST method
   * @param path
   * @return
   */
  def get(path: String): String = {
    logger.debug("GET: " + path)
    val httpGet = new HttpGet(server.url + path)
    httpGet.setConfig(config1)
    val closeableHttp = httpClient.execute(httpGet)
    val res = getResultFromEntity(closeableHttp.getEntity)
    if(closeableHttp != null) closeableHttp.close()
    res
  }

  /**
   * Post REST method
   * @param path
   * @param params
   * @return
   */
  def post(path: String, params: List[NameValuePair]): String = {

    logger.debug("POST: " + path + " with parameters: " + params.toString())

    val httpPost = new HttpPost(server.url + path)
    httpPost.setConfig(config1)

    val reqEntity = MultipartEntityBuilder.create()
    for(n <- params){
      reqEntity.addPart(n.getName, new StringBody(n.getValue, "text/plain", Charset.forName("UTF-8")))
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

  /**
   * Upload a paper to the server
   *
   * @param pdf_path
   * @param pdf_title
   * @param budget_cts
   * @param highlight_enabled
   */
  def uploadPaper(pdf_path: String, pdf_title: String, budget_cts:Int, highlight_enabled: Boolean) : Long= {

    val paper = PaperDAO.findByTitle(pdf_title).getOrElse(null)
    var remotePaperCheck = false
    if(paper != null) {
      remotePaperCheck = get("/checkPaper/"+paper.remote_id).toBoolean
      if(remotePaperCheck){
        return paper.remote_id
      }
    }
      //Upload paper only if it is not already uploaded
      logger.info("Uploading paper: " + pdf_title)
      val uri = server.url + "/paper"
      val httpClient = HttpRestClient.httpClient
      try {
        // Create the entity
        val reqEntity = MultipartEntityBuilder.create()

        // Attach the file
        reqEntity.addPart("source", new FileBody(new File(pdf_path)))

        // Attach the pdfTitle and budget as plain text
        val tokenBody = new StringBody(pdf_title, ContentType.TEXT_PLAIN)
        reqEntity.addPart("pdf_title", tokenBody)
        reqEntity.addPart("highlight_enabled", new StringBody(String.valueOf(highlight_enabled), ContentType.TEXT_PLAIN))

        // Create POST request
        logger.debug("Create http post request")
        val httpPost = new HttpPost(uri)
        httpPost.setEntity(reqEntity.build())

        // Execute the request in a new HttpContext
        val ctx = HttpClientContext.create()
        logger.debug("Sending request...")
        val response: CloseableHttpResponse = httpClient.execute(httpPost, ctx)
        // Read the response
        val entity = response.getEntity
        val result = EntityUtils.toString(entity)
        // Close the response
        if(response != null) response.close()
        logger.debug(s"Request sent with response: $result")

        val id = PaperDAO.create(pdf_title, budget_cts,result.toLong)
        logger.debug(s"Paper stored in DB with id: $id")

        result.toLong
      } catch {
        case e: Exception => {
          logger.error(e.getMessage)
          -1
        }
      }
  }

  def GetAnswersForQuestion(question_id: Long) : Iterable[Answer] = {
    try {
      val jsonAnswer: JsValue = Json.parse(get("/answers/"+question_id))
      val a = jsonAnswer.validate[List[Answer]]
      a.get.toIterable
    } catch {
      case e: Exception =>
        logger.debug("No answer given yet")
        None
    }
  }

  /**
   * Create a question
   * @param query
   * @return -1 if error occurs, id of remote question otherwise
   */
  def CreateQuestion(query: CrowdSAQuery): Long = {

    val properties = query.getProperties()
    //check if paper exists
    val isUploaded = get("/checkPaper/"+properties.paper_id).toBoolean

    try {
      if(isUploaded) {
        // The paper exists
        //post the question and get remote id
        val date = (new Date()).getTime/1000
        val params = new collection.mutable.MutableList[NameValuePair]
        params += new BasicNameValuePair("question", query.getQuery().question)
        params += new BasicNameValuePair("question_type", properties.question_type)
        params += new BasicNameValuePair("reward_cts", properties.reward_cts.toString)
        params += new BasicNameValuePair("created_at", date.toString)
        params += new BasicNameValuePair("papers_id", properties.paper_id.toString)
        params += new BasicNameValuePair("expiration_time_sec", properties.expiration_time_sec.toString)
        params += new BasicNameValuePair("maximal_assignments", properties.maximal_assignments.toString)
        params += new BasicNameValuePair("possible_answers", properties.possible_answers.getOrElse(""))

        val resp = post("/addQuestion", params.toList)
        if(resp.startsWith("Error")){
          logger.error(resp)
          -1
        } else {
          val remote_question_id = resp.toLong
          logger.debug("Question posted with remote id: " + remote_question_id)

          //create question object and store it in the local DB
          val qId = QuestionDAO.create(query.getQuery().question, properties.question_type, properties.reward_cts, date,
            properties.paper_id, remote_question_id, properties.maximal_assignments, properties.expiration_time_sec, null)

          if(qId > 0) {
            logger.debug("Question stored in local DB with id: " + qId)
          } else {
            logger.error("Cannot store question in the local database.")
          }
          //Create highlight entry for the question if properties.highlightedTerms is not empty
          if(properties.highlight != null) {
            val params = new collection.mutable.MutableList[NameValuePair]
            params += new BasicNameValuePair("questionId", remote_question_id.toString)
            params += new BasicNameValuePair("assumption", properties.highlight.assumption)
            params += new BasicNameValuePair("terms", properties.highlight.terms)
            logger.debug("Adding highlight returned: " + post("/highlight", params.toList))
            HighlightDAO.save(properties.highlight.id, remote_question_id)
          }

          if(properties.deniedTurkers != null){
            val params = new collection.mutable.MutableList[NameValuePair]
            params += new BasicNameValuePair("question_id", remote_question_id.toString)
            params += new BasicNameValuePair("teams", properties.deniedTurkers.get.mkString(","))
            logger.debug("Adding qualifications: " + post("/qualification", params.toList))
          }

          //return the id of remote question
          remote_question_id
        }

      } else {
        //The paper doesn't exist in the server
        logger.error("There is no paper with id: " + properties.paper_id + " stored in the server")
        -1
      }
    } catch {
      case e: Exception => {
        logger.error("An error occurred while creating new question: " + Question)
        e.printStackTrace()
        -1
      }
    }
  }

  /**
   * Disable a question
   * @param qId id of the remote question
   */
  def DisableQuestion(qId: Long): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("question_id", qId.toString)
      val resp = post("/disablequestion", params.toList)
      if(resp.startsWith("Error")){
        logger.error(resp)
      }else {
        try{
          val question = QuestionDAO.findByRemoteId(qId).get
          QuestionDAO.save(question.id, true, question.expiration_time_sec,
            question.maximal_assignments)
        } catch {
          case e: Exception => e.printStackTrace()
        }
        logger.debug(resp)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def SearchQuestions(): Iterable[Question] = {
    QuestionDAO.findAllEnabled()
  }

  def GetAssignmentsForQuestion(question_id: Long): Iterable[Assignment] = {
    try {
      val jsonAssignment: JsValue = Json.parse(get("/assignments/"+question_id))
      val a = jsonAssignment.validate[List[Assignment]]
      a.get.toIterable
    } catch {
      case e: Exception =>
      //If no assignment is present we land always here
      None
    }
  }

  def ApproveAnswer(a: Answer): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("answer_id", a.id.toString)
      params += new BasicNameValuePair("accepted", "true")
      params += new BasicNameValuePair("bonus_cts", "0")
      logger.debug(post("/evaluateanswer", params.toList))
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def RejectAnswer(a: Answer): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("answer_id", a.id.toString)
      params += new BasicNameValuePair("accepted", "false")
      params += new BasicNameValuePair("bonus_cts", "0")
      logger.debug(post("/evaluateanswer", params.toList))
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  /**
   * Extend question's maximal assignments.
   * @param question_id the remote id of the question
   * @param MaxAssignmentsIncrement the increment of the maximal assignments limit
   */
  def ExtendQuestionMaxAssignments(question_id: Long, MaxAssignmentsIncrement: Int): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("question_id", question_id.toString)
      params += new BasicNameValuePair("maximal_assignments", MaxAssignmentsIncrement.toString)

      val resp = post("/extendmaxassignments", params.toList)
      if(resp.startsWith("Error")){
        logger.error(resp)
      }else {
        val question = QuestionDAO.find(question_id).get
        QuestionDAO.save(question_id, true, question.expiration_time_sec,
          question.maximal_assignments+MaxAssignmentsIncrement)
        logger.debug(resp)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def ExtendQuestionExpiration(question_id: Long, ExpirationIncrementInSeconds: Int): Unit = {
    try {
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("question_id", question_id.toString)
      params += new BasicNameValuePair("expiration_increment_sec", ExpirationIncrementInSeconds.toString)
      val resp = post("/extendexpiration", params.toList)
      if(resp.startsWith("Error")){
        logger.error(resp)
      }else {
        val question = QuestionDAO.find(question_id).get
        QuestionDAO.save(question_id, true, question.expiration_time_sec+ExpirationIncrementInSeconds,
          question.maximal_assignments)
        logger.debug(resp)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def getAssignmentForAnswerId(answerId: Long) : Assignment = {
    try {
      val resp = get("/assignmentbyanswerid/"+answerId.toString)
      if(resp.startsWith("Error")){
        logger.error(resp)
        null
      }else {
        val jsonAssignment: JsValue = Json.parse(resp)
        val assignment = jsonAssignment.validate[Assignment]
        logger.debug(resp)
        assignment.get
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        null
      }
    }
  }

  def getPaperIdFromAnswerId(answerId: Long): Long = {
    try {
      val resp = get("/paper/"+answerId.toString)
      if(resp.toLong == -1){
        logger.error(resp)
        -1
      }else {
        logger.debug("Answer " + answerId + " corresponds to paper with id: "+resp)
        resp.toLong
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        -1
      }
    }
  }

  def createDataset(answerId: Long): Long = {
    try{
      val params = new collection.mutable.MutableList[NameValuePair]
      params += new BasicNameValuePair("answer_id", answerId.toString)
      val resp = post("/dataset", params.toList)
      if(resp.toLong >= 1){
        logger.debug("Successfully created new dataset with id: " + resp)
      }
      else {
        logger.error("Cannot create new dataset")
      }
      resp.toLong
    } catch {
      case e: Exception => {
        e.printStackTrace()
        -1
      }
    }
  }

  def getDatasetById(datasetId: Long): Dataset = {
    try{
      val params = new collection.mutable.MutableList[NameValuePair]
      val resp = get("/dataset/"+datasetId.toString)
      val jsonAssignment: JsValue = Json.parse(resp)
      val dataset = jsonAssignment.validate[Dataset]
      dataset.get
    } catch {
      case e: Exception => {
        e.printStackTrace()
        null
      }
    }
  }

}