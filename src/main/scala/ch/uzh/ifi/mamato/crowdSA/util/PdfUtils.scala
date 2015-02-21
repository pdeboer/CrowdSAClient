package ch.uzh.ifi.mamato.crowdSA.util

import java.io._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper

import scala.collection.mutable

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

  def findContextMatch(source: String, toMatch: String): List[String] = {
    val mut = new mutable.MutableList[String]
    try {
      for(m <- toMatch.r.findAllMatchIn(source)){


        //val contextBefore = m.before.toString
        val contextAfter = m.after.toString

        //val bLength = contextBefore.length
        //val aLength = contextAfter.length


        //println(contextBefore.substring(bLength-10, bLength)+toMatch+ contextAfter.substring(0, 10))
        mut.+=:(toMatch+ contextAfter.substring(0, 20))
      }
      mut.toList
    }catch {
      case e: Exception => e.getStackTrace
        null
    }
  }

}

