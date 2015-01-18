package ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf

import ch.uzh.ifi.pdeboer.pplib.hcomp._

/**
 * Created by Mattia on 14.01.2015.
 */

@HCompPortal(builder = classOf[CrowdPdfPortalBuilder], autoInit = true)
class CrowdPdfPortalAdapter(val applicationName: String, val apiKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {

  def this(applicationName: String, sandbox: Boolean) =
    this(applicationName,
      "",
      sandbox)

  def this(applicationName: String) = this(applicationName, false)

  override def getDefaultPortalKey: String = CrowdPdfPortalAdapter.PORTAL_KEY

  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
    if (properties.qualifications.length > 0)
      logger.error("CrowdPDF implementation doesn't support Worker Qualifications yet. Executing query without them..")


    Option.empty[HCompAnswer]
  }


  override def cancelQuery(query: HCompQuery): Unit = {
    println("Removing question:" + query.question)
  }
}

object CrowdPdfPortalAdapter {
  val CONFIG_API_KEY = "hcomp.crowdpdf.apikey"
  val CONFIG_APPLICATION_NAME = "hcomp.crowdpdf.applicationName"
  val CONFIG_SANDBOX = "hcomp.crowdpdf.sandbox"

  val PORTAL_KEY = "crowdPdf"
}

class CrowdPdfPortalBuilder extends HCompPortalBuilder {
  val API_KEY: String = "apiKey"
  val APPLICATION_NAME: String = "appName"
  val SANDBOX: String = "sandbox"

  val parameterToConfigPath = Map(
    API_KEY -> CrowdPdfPortalAdapter.CONFIG_API_KEY,
    APPLICATION_NAME -> CrowdPdfPortalAdapter.CONFIG_APPLICATION_NAME,
    SANDBOX -> CrowdPdfPortalAdapter.CONFIG_SANDBOX
  )

  override def build: HCompPortalAdapter = new CrowdPdfPortalAdapter(
    params.getOrElse(APPLICATION_NAME, "PPLib Application"),
    params(API_KEY), params.getOrElse(SANDBOX, "false") == "true"
  )

  override def expectedParameters: List[String] = List(API_KEY)
}