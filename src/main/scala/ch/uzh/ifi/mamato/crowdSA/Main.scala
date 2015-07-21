package ch.uzh.ifi.mamato.crowdSA

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAPortalAdapter
import ch.uzh.ifi.mamato.crowdSA.persistence._
import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
import ch.uzh.ifi.mamato.crowdSA.process.{ExtractStatisticsProcess, ExtractStatisticsRecombination}
import ch.uzh.ifi.mamato.crowdSA.util.{LazyLogger, PdfUtils}
import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationVariant, SimpleRecombinationVariantXMLExporter}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random

/**
 * Created by Mattia on 18.12.2014.
 */

object Main extends App with LazyLogger {
  logger.info("**** Mattia Amato - CrowdSA Client ****")
  if(args.length != 2){
    logger.info("Please use two arguments when starting the client. " +
      "The first is the path to the PDF file, the second the title of the PDF.")
    System.exit(0)
  }

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

    var statMethod2ContextStatMethod = new mutable.MutableList[(String, String)]
    try {
      // open pdf and convert it to text
      val pdfToText = PdfUtils.getTextFromPdf(pathPdf).get

      // get context of statistical methods that correspond to the ones present in the database
      statMethod2ContextStatMethod = PdfUtils.findContextMatch(pdfToText, toMatch.map(_.stat_method).toList)
      logger.debug("Found " + statMethod2ContextStatMethod.length + " matches in the paper")
    } catch {
      case e: Exception => e.printStackTrace()
    }

    // upload paper
    val remote_id = CrowdSAPortalAdapter.service.uploadPaper(pathPdf, titlePdf, 15000, true)
    if(remote_id < 0){
      logger.error("Error while uploading the paper")
    }
    else {

      val discoveryQuestions = new mutable.MutableList[CrowdSAPatch]

      val allMethods = new mutable.HashMap[String, mutable.MutableList[String]]

      // create DISCOVERY questions for each statistical method that matched
      statMethod2ContextStatMethod.foreach {
        m =>

          if(!allMethods.get(m._1).isDefined){
            allMethods += m._1 -> new mutable.MutableList[String]
          }

          allMethods.get(m._1).get += m._2

          logger.debug("Creating DISCOVERY question for match: " + m._1)
          // Add to the data structure: the question as well as the properties
          discoveryQuestions += new CrowdSAPatch("Method: <u><i> " + m._1 + " </i></u>", "Discovery",
            "[\""+m._2+"\"]", remote_id, "Discovery")
      }

      // create MISSING question
      //val findMissingMethods = new CrowdSAPatch("Please find all the methods which are not highlighted on the paper", "Missing", "[]", remote_id, "Missing", allMethods)
      //discoveryQuestions += findMissingMethods

      def getXML(r: RecombinationVariant) = new SimpleRecombinationVariantXMLExporter(r).xml.toString
      val recombinations: List[RecombinationVariant] = ExtractStatisticsRecombination.recombinations

      // populate DB if it hasn't been done yet
      // To run multiple time the client at the same time, the candidate are based on the uploaded paper id.
      if (CandidateESDAO.candidates(remote_id).length == 0) {
        recombinations.zipWithIndex.map(r => ProcessCandidate(r._2, getXML(r._1)))
          .foreach(c => CandidateESDAO.insertProcessCandidate(c, remote_id))
      }

      //Create the process for the extraction of statistical means
      val extractStatisticsProcess = new ExtractStatisticsProcess(crowdSA, discoveryQuestions.toList)

      //For each candidate process execute the process
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

            val res = extractStatisticsProcess.runRecombinedVariant(recombinations.head)
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
        val c = CandidateESDAO.candidates(remote_id)
          .filter(c => c.endTime.isEmpty && (c.startTime.isEmpty || c.startTime.get.plusHours(10).isBeforeNow))
        if (c.length > 0) {
          val targetIndex = (c.length * Random.nextDouble()).toInt
          Some(c(targetIndex))
        } else {
          logger.info("No more candidate to execute..")
          logger.info("Please delete the database or run the client with a new paper.")
          None
        }
      }
    }

}