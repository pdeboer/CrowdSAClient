package ch.uzh.ifi.mamato.crowdSA.util

import java.io._
import java.util.regex.{Pattern, Matcher}
import ch.uzh.ifi.mamato.crowdSA.model.StatMethod
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper

import scala.collection.mutable
import scala.util.matching.Regex

/**
 * Created by mattia on 20.02.15.
 */

object PdfUtils {


  def getTextFromPdf(filename: String): Option[String] = {
    try {
      val pdf = PDDocument.load(new File(filename))
      val stripper = new PDFTextStripper
      stripper.setLineSeparator(" ")
      //stripper.setStartPage(startPage)
      //stripper.setEndPage(endPage)
      Some(stripper.getText(pdf))
    } catch {
      case t: Throwable =>
        t.printStackTrace
        None
    }
  }

  def findContextMatchMutipleMatches(source: String, toMatch: List[String]): List[String] = {
    findContextMatch(source, toMatch).map(_._2).toList
  }

  /**
   * Find a match in the PDF file and extract the shortest unique string that identifies it.
   * @param source
   * @param toMatch
   * @return
   */
  def findContextMatch(source: String, toMatch: List[String]): mutable.MutableList[(String, String)] = {
    val mut = new mutable.MutableList[(String, String)]
    try {
      toMatch.foreach(m => {
        val pattern = ("(?i)((\\b"+m+"\\b))").r
        for(mm <- pattern.findAllMatchIn(source)){
          val contextAfter = mm.after.toString
          val contextBefore = mm.before.toString
          var start = contextBefore.length
          var end = 0
          // Iterate until a unique match is found
          var pattern1 = ("(?i)("+contextBefore.substring(start, contextBefore.length) + m +
            contextAfter.substring(0, end)+")").r
          do{
            if(start >0) {
              start -= 1
            }
            if(end < contextAfter.length) {
              end += 1
            }
            pattern1 = ("(?i)("+contextBefore.substring(start, contextBefore.length) + m +
              contextAfter.substring(0, end)+")").r
          }while(
            contextBefore.length >= start &&
              contextAfter.length >= end &&
              pattern1.findAllMatchIn(source).length>1)

          mut += m -> (contextBefore.substring(start, contextBefore.length)
            + m +
            contextAfter.substring(0, end))
        }
      })

      mut
    }catch {
      case e: Exception => {
        throw e
      }
    }
  }

}

