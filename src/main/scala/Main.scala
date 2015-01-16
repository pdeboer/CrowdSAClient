
import java.io._
import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}
import _root_.hcomp.crowdpdf.CrowdPdfPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import org.apache.http.client.methods.{HttpPost, CloseableHttpResponse}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{StringBody, FileBody}
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.impl.client.HttpClients
import scala.util.Try

import scala.concurrent.ExecutionContext.Implicits.global
import ch.uzh.ifi.pdeboer.pplib._



/**
 * Created by Mattia on 18.12.2014.
 */

object Main {

  lazy val httpClient = {
    val connManager = new PoolingHttpClientConnectionManager()
    HttpClients.custom().setConnectionManager(connManager).build()
  }

  def main(args: Array[String]) = {
    println("**** Mattia Amato CrowdPdf Client ****")

    val crowdPdf = new CrowdPdfPortalAdapter("MattiaTest", sandbox = false)
    HComp.addPortal(crowdPdf)

    val pdfByteArray = pdfToByteArray(args(0))

    val uri = "http://localhost:9000/paper"

    Try({
      // Create the entity
      val reqEntity = MultipartEntityBuilder.create()

      // Attach the file
      reqEntity.addPart("source", new FileBody(new File(args(0))))

      // Attach the pdfTitle and budget as plain text
      val tokenBody = new StringBody("Awesome Title", ContentType.TEXT_PLAIN)
      reqEntity.addPart("pdfTitle", tokenBody)
      reqEntity.addPart("budget", new StringBody("10", ContentType.TEXT_PLAIN))

      // Create POST request
      val httpPost = new HttpPost(uri)
      httpPost.setEntity(reqEntity.build())

      // Execute the request in a new HttpContext
      val ctx = HttpClientContext.create()
      val response: CloseableHttpResponse = httpClient.execute(httpPost, ctx)

      // Read the response
      val entity = response.getEntity
      val result = EntityUtils.toString(entity)

      // Close the response
      if(response != null) response.close()

      println(result)
    })
  }

  def pdfToByteArray(path: String): Array[Byte] ={

    val p = Paths.get(path)
    val data = Files.readAllBytes(p)

    data
  }

}