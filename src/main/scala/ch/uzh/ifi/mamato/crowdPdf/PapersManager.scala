package ch.uzh.ifi.mamato.crowdPdf

import java.io.File

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

  def createPaperRequest(paperPath: String, paperTitle: String, budget:Int) = {
    val uri = "http://localhost:9000/paper"
    val httpClient = HttpRestClient.httpClient
    try {
      // Create the entity
      val reqEntity = MultipartEntityBuilder.create()

      // Attach the file
      logger.debug("Attach the pdf file to the request")
      reqEntity.addPart("source", new FileBody(new File(paperPath)))

      // Attach the pdfTitle and budget as plain text
      logger.debug("Attach the title and budget to the request")
      val tokenBody = new StringBody(paperTitle, ContentType.TEXT_PLAIN)
      reqEntity.addPart("pdfTitle", tokenBody)
      reqEntity.addPart("budget", new StringBody(String.valueOf(budget), ContentType.TEXT_PLAIN))

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

    } catch {
      case e: Exception => logger.error(e.getMessage)
    }
  }
}
