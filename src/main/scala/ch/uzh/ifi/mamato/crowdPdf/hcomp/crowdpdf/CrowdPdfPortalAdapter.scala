package ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime
import ch.uzh.ifi.mamato.crowdPdf.util.CollectionUtils._

import scala.collection.mutable

/**
 * Created by Mattia on 14.01.2015.
 */

//case class CrowdPDfQueryProperties(reward: Int = 0, questionType: String = "", paperId: Long = 0)

@HCompPortal(builder = classOf[CrowdPdfPortalBuilder], autoInit = true)
class CrowdPdfPortalAdapter extends HCompPortalAdapter with LazyLogger {

  override def getDefaultPortalKey: String = "crowdPdf"

  val serviceURL = "http://localhost:9000"

  var map = mutable.HashMap.empty[Int, CrowdPdfQueries]

  val service = new CrowdPdfService(new Server(serviceURL))

  //TODO: add parameters to set the questionType, the reward per answer and the paper_fk
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
    if (properties.qualifications.length > 0)
      logger.error("CrowdPDF implementation doesn't support Worker Qualifications yet. Executing query without them..")

    val manager: CrowdPdfManager = new CrowdPdfManager(service, query, "Boolean", 2, 10, properties)
    map += query.identifier -> map.getOrElse(query.identifier, new CrowdPdfQueries()).add(manager)

    val res = manager.createQuestion()
    logger.debug("CreateQuestion returned id: " + res)
    if(res > 0) {
      return manager.waitForResponse()
    } else {
      logger.error("Error while creating the question.")
      return null
    }
  }


  override def cancelQuery(query: HCompQuery): Unit = {
    // Remove a question from the server
    val manager = map.get(query.identifier)
    if(manager.isDefined) {
      manager.get.list.mpar.foreach(q => try {
        //naively cancel all previous queries just to make sure
        q._2.cancelQuestion()
      }
      catch {
        case e: Exception => {}
      })
      logger.info(s"cancelled '${query.title}'")
    } else {
      logger.info(s"could not find query with ID '${query.identifier}' when trying to cancel it")
    }
  }

  protected[CrowdPdfPortalAdapter] class CrowdPdfQueries() {
    private var sent: List[(DateTime, CrowdPdfManager)] = Nil

    def list = sent

    def add(manager: CrowdPdfManager) = {
      this.synchronized {
        sent = (DateTime.now(), manager) :: sent
      }
      this
    }
  }

  def findAllUnapprovedHitsAndApprove: Unit = {
    ???
  }

  def expireAllHits: Unit = {
    ???
  }

}

  object CrowdPdfPortalAdapter {
    val CONFIG_ACCESS_ID_KEY = "hcomp.crowdpdf.accessKeyID"
    val CONFIG_SECRET_ACCESS_KEY = "hcomp.crowdpdf.secretAccessKey"
    val CONFIG_SANDBOX_KEY = "hcomp.crowdpdf.sandbox"
    val PORTAL_KEY = "crowdPdf"
  }

  class CrowdPdfPortalBuilder extends HCompPortalBuilder {
    val ACCESS_ID_KEY: String = "accessIdKey"
    val SECRET_ACCESS_KEY: String = "secretAccessKey"
    val SANDBOX: String = "sandbox"

    val parameterToConfigPath = Map(
      ACCESS_ID_KEY -> CrowdPdfPortalAdapter.CONFIG_ACCESS_ID_KEY,
      SECRET_ACCESS_KEY -> CrowdPdfPortalAdapter.CONFIG_SECRET_ACCESS_KEY,
      SANDBOX -> CrowdPdfPortalAdapter.CONFIG_SANDBOX_KEY
    )

    override def build: HCompPortalAdapter = new CrowdPdfPortalAdapter()

    override def expectedParameters: List[String] = null
  }

