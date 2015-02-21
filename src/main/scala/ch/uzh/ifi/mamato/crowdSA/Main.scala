package ch.uzh.ifi.mamato.crowdSA

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQueryProperties, CrowdSAPortalAdapter}
import ch.uzh.ifi.mamato.crowdSA.model.Highlight
import ch.uzh.ifi.mamato.crowdSA.persistence.{StatMethodsDAO, DBSettings}
import ch.uzh.ifi.mamato.crowdSA.util.{PdfUtils, LazyLogger}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HComp}

import scala.collection.mutable

/**
 * Created by Mattia on 18.12.2014.
 */


object Main extends App with LazyLogger {
  logger.info("**** Mattia Amato CrowdSA Client ****")
  logger.info("Initializing database...")

  DBSettings.initialize()

  logger.debug("Creating portal adapter...")
  val crowdSA = new CrowdSAPortalAdapter()
  HComp.addPortal(crowdSA)
  logger.debug("Portal added")

  val pathPdf = args(0)
  val titlePdf = args(1)

  logger.debug(pathPdf)
  logger.debug(titlePdf )

  // create list of possible matches
  val toMatch = StatMethodsDAO.findAll()
  logger.debug("Found " + toMatch.length + " statistical methods in the database")

  // open pdf
  // convert it to text
  var mutableMatch = new mutable.MutableList[(String, String)]
  try {
    val text = PdfUtils.getTextFromPdf(pathPdf).get
    // get statistical methods that correspond to the ones present in the database
    toMatch.foreach {
      sm =>
        val mapp = PdfUtils.findContextMatch(text.toUpperCase(), sm.stat_method.toUpperCase())
        mapp.foreach {
          p =>
          mutableMatch.+=:(sm.stat_method, p)
        }
    }
  } catch {
    case e: Exception => e.printStackTrace()
  }

  // upload paper
  val remote_id = crowdSA.service.uploadPaper(pathPdf, titlePdf, 5000, true)
  if(remote_id < 0){
    logger.error("Error while uploading the paper")
  }else {

    // create DISCOVERY questions for each statistical method that matched
    mutableMatch.foreach{
      m =>
        logger.debug("Creating DISCOVERY question for match: " + m._1)
        val query = new HCompQuery {
          override def question: String = "Identify the dataset of the statistical method: " + m._1 + " highlighted in the paper"

          override def title: String = "Dataset " + m._2

          override def suggestedPaymentCents: Int = 30
        }

        val processQuery = new Thread(new Runnable {
          def run(): Unit = {
            //Upload paper
            val highlight = new Highlight("Normality", m._2)
            val properties = new CrowdSAQueryProperties(remote_id, "Discovery", highlight, 10, 60*60*24*365, 5)
            //Process query
            crowdSA.processQuery(query, properties)
          }
        })

        processQuery.start()
    }
  }

  //TODO: 5) create BOOLEAN questions


  /*val cancelQuery = new Thread(new Runnable {
    def run(): Unit = {
      logger.debug("Try to cancel query")
      crowdSA.cancelQuery(query)
    }
  })*/

  //Thread.sleep(5000)

  //b.start()
}