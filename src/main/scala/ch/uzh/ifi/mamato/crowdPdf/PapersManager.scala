package ch.uzh.ifi.mamato.crowdPdf

import java.io.File
import ch.uzh.ifi.mamato.crowdPdf.model.Paper
import ch.uzh.ifi.mamato.crowdPdf.persistence.PaperDAO
import ch.uzh.ifi.mamato.crowdPdf.util.{HttpRestClient, LazyLogger}
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.http.util.EntityUtils

/**
 * Created by Mattia on 18.01.2015.
 */
object PapersManager extends LazyLogger {

  // connect to the database named "bachelorClient" on the localhost
  val driver = "com.mysql.jdbc.Driver"
  val url = "jdbc:mysql://localhost/bachelorClient"
  val username = "admin"
  val password = "admin"

  def closePaper(id: Long) = {
    val uri = ""
  }

  def uploadPaper(paperPath: String, paperTitle: String, budget:Int, enableHighlight: Boolean) = {
    logger.info("Uploading paper: " + paperTitle)
    val uri = "http://localhost:9000/paper"
    val httpClient = HttpRestClient.httpClient
    try {
      // Create the entity
      val reqEntity = MultipartEntityBuilder.create()
      
      // Attach the file
      reqEntity.addPart("source", new FileBody(new File(paperPath)))

      // Attach the pdfTitle and budget as plain text
      val tokenBody = new StringBody(paperTitle, ContentType.TEXT_PLAIN)
      reqEntity.addPart("pdfTitle", tokenBody)
      reqEntity.addPart("budget", new StringBody(String.valueOf(budget), ContentType.TEXT_PLAIN))
      reqEntity.addPart("highlight", new StringBody(String.valueOf(enableHighlight), ContentType.TEXT_PLAIN))

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

      val id = PaperDAO.create(result.toLong)
      logger.debug(s"Paper stored in DB with id: $id")

    } catch {
      case e: Exception => logger.error(e.getMessage)
    }
  }
}
