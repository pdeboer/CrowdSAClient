package ch.uzh.ifi.mamato.crowdSA

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQuery, CrowdSAQueryProperties, CrowdSAPortalAdapter}
import ch.uzh.ifi.mamato.crowdSA.model.Highlight
import ch.uzh.ifi.mamato.crowdSA.persistence.{ProcessCandidate, CandidateESDAO, StatMethodsDAO, DBSettings}
import ch.uzh.ifi.mamato.crowdSA.process.{ExtractStatisticsRecombination, ExtractStatisticsProcess}
import ch.uzh.ifi.mamato.crowdSA.util.{PdfUtils, LazyLogger}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HComp}
import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationVariantGenerator, TypedParameterVariantGenerator, SimpleRecombinationVariantXMLExporter, RecombinationVariant}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{CollectDecideProcess, Collection, ListScaleProcess}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random

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

  // create list of possible matches
  val toMatch = StatMethodsDAO.findAll()
  logger.debug("Found " + toMatch.length + " statistical methods in the database")

  // open pdf
  // convert it to text

  var mutableMatch = new mutable.MutableList[(String, String)]
  try {
    val text = PdfUtils.getTextFromPdf(pathPdf).get

    //TODO: remove me
    // maxMatches = 1

      // get statistical methods that correspond to the ones present in the database
      toMatch.foreach {
        sm =>
          val mapp = PdfUtils.findContextMatch(text.toUpperCase(), sm.stat_method.toUpperCase())

          mapp.foreach {
            p =>
              //TODO: remove me
              //if(mutableMatch.length < maxMatches) {
                mutableMatch.+=:(sm.stat_method, p)
              //}
          }
    }
  } catch {
    case e: Exception => e.printStackTrace()
  }

  // upload paper
  val remote_id = CrowdSAPortalAdapter.service.uploadPaper(pathPdf, titlePdf, 15000, true)
  if(remote_id < 0){
    logger.error("Error while uploading the paper")
  } else {

    val discoveryQuestions = new mutable.MutableList[CrowdSAQuery]

    // create DISCOVERY questions for each statistical method that matched

        mutableMatch.foreach {
          m =>
            logger.debug("Creating DISCOVERY question for match: " + m._1)
            val query = new HCompQuery {
              override def question: String = "Identify the dataset of the statistical method: " + m._1 + " highlighted in the paper"

              override def title: String = m._2

              override def suggestedPaymentCents: Int = 10
            }

            val highlight = new Highlight("Discovery", m._2)
            val properties = new CrowdSAQueryProperties(remote_id, "Discovery", highlight, 10, ((new Date().getTime()/1000) + 60*60*24*365), 100)
            discoveryQuestions += new CrowdSAQuery(query, properties)
        }

    def getXML(r: RecombinationVariant) = new SimpleRecombinationVariantXMLExporter(r).xml.toString

    val recombinations: List[RecombinationVariant] = ExtractStatisticsRecombination.recombinations

    //populate DB if it hasn't been done yet
    if (CandidateESDAO.candidates.length == 0) {
      recombinations.zipWithIndex.map(r => ProcessCandidate(r._2, getXML(r._1)))
        .foreach(c => CandidateESDAO.insertProcessCandidate(c))
    }

    //Create the process of extraction of statistical means
    val extractStatisticsProcess = new ExtractStatisticsProcess(crowdSA, discoveryQuestions.toList)

    //For each candidate process execute the process with the same input
    while (nextCandidate.isDefined) {
      val cOpt = nextCandidate
      if (cOpt.isDefined) {
        //ugly, but necessary since there may be no more candidate anymore meanwhile
        val c = cOpt.get
        //Set start time for this recombination variant (needed for comparison with other variants)
        c.startTime = Some(DateTime.now())
        CandidateESDAO.updateProcessCandidateTimes(c)

        try {
        //Execute the recombination variant
          val DEFAULT_BUDGET: Int = 15000
          logger.debug("Setting budget...")
          crowdSA.setBudget(Some(DEFAULT_BUDGET))

          val res = extractStatisticsProcess.runRecombinedVariant(recombinations(c.id-1))
          c.result = Some(res)
          c.cost = Some(DEFAULT_BUDGET - crowdSA.budget.get)
        }
        catch {
          case e: Throwable => {
            e.printStackTrace()
            c.error = Some(e.getMessage + "\n\n" + e.getStackTrace.mkString("\n"))
          }
        }

        c.endTime = Some(DateTime.now())
        CandidateESDAO.updateProcessCandidateTimes(c)
      }
    }


    logger.info("finished at " + new Date())

    def nextCandidate = {
      val c = CandidateESDAO.candidates
        .filter(c => c.endTime.isEmpty && (c.startTime.isEmpty || c.startTime.get.plusHours(10).isBeforeNow))
      if (c.length > 0) {
        val targetIndex = (c.length * Random.nextDouble()).toInt
        Some(c(targetIndex))
      } else {
        logger.info("No more candidate to execute..")
        None
      }
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