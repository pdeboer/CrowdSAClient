package ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime
import ch.uzh.ifi.mamato.crowdSA.util.CollectionUtils._

import scala.collection.mutable

/**
 * Created by Mattia on 14.01.2015.
 */

//case class CrowdPDfQueryProperties(reward: Int = 0, questionType: String = "", paperId: Long = 0)

@HCompPortal(builder = classOf[CrowdSAPortalBuilder], autoInit = true)
class CrowdSAPortalAdapter extends HCompPortalAdapter with LazyLogger {

  override def getDefaultPortalKey: String = "crowdSA"

  val serviceURL = "http://localhost:9000"

  var map = mutable.HashMap.empty[Int, CrowdPdfQueries]

  val service = new CrowdSAService(new Server(serviceURL))

  /**
   * Hack to solve the properties problem
   * @param query
   * @param properties
   * @return
   */
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
    try {
      return processQuery(query, properties.asInstanceOf[CrowdSAQueryProperties])
    } catch {
      case e: Exception => e.printStackTrace()
    }
    return null
  }

  //TODO: add parameters to set the question_type, the reward per answer and the remote paper_id
  def processQuery(query: HCompQuery, properties: CrowdSAQueryProperties): Option[HCompAnswer] = {
    if (properties.qualifications.length > 0)
      logger.error("CrowdPDF implementation doesn't support Worker Qualifications yet. Executing query without them..")

    val manager: CrowdSAManager = new CrowdSAManager(service, query, properties)
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

  protected[CrowdSAPortalAdapter] class CrowdPdfQueries() {
    private var sent: List[(DateTime, CrowdSAManager)] = Nil

    def list = sent

    def add(manager: CrowdSAManager) = {
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

  object CrowdSAPortalAdapter {
    val CONFIG_ACCESS_ID_KEY = "hcomp.crowdsa.accessKeyID"
    val CONFIG_SECRET_ACCESS_KEY = "hcomp.crowdsa.secretAccessKey"
    val CONFIG_SANDBOX_KEY = "hcomp.crowdsa.sandbox"
    val PORTAL_KEY = "crowdSA"
  }

  class CrowdSAPortalBuilder extends HCompPortalBuilder {
    val ACCESS_ID_KEY: String = "accessIdKey"
    val SECRET_ACCESS_KEY: String = "secretAccessKey"
    val SANDBOX: String = "sandbox"

    val parameterToConfigPath = Map(
      ACCESS_ID_KEY -> CrowdSAPortalAdapter.CONFIG_ACCESS_ID_KEY,
      SECRET_ACCESS_KEY -> CrowdSAPortalAdapter.CONFIG_SECRET_ACCESS_KEY,
      SANDBOX -> CrowdSAPortalAdapter.CONFIG_SANDBOX_KEY
    )

    override def build: HCompPortalAdapter = new CrowdSAPortalAdapter()

    override def expectedParameters: List[String] = null
  }

