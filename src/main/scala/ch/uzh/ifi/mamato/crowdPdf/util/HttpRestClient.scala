package ch.uzh.ifi.mamato.crowdPdf.util

import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

/**
 * Created by Mattia on 18.01.2015.
 */
object HttpRestClient {
  lazy val httpClient = {
    val connManager = new PoolingHttpClientConnectionManager()
    HttpClients.custom().setConnectionManager(connManager).build()
  }
}
