
import ch.uzh.ifi.mamato.crowdSA.util.PdfUtils
import org.junit.{Assert, Test}

import scala.collection.mutable


/**
 * Created by mattia on 08.06.15.
 */
class PdfUtilsTest {
  @Test
  def testMultipleMatch(): Unit = {
    val toMatch = new mutable.MutableList[String]
    toMatch += "test"
    val res = PdfUtils.findContextMatch("This is a test. This test is a test.",toMatch.toList)
    Assert.assertTrue(res.length == 3)

    // Element is present only 1 time
    res.foreach(r => {
      Assert.assertTrue(res.filter(e => e._1 == r._1 && e._2 == r._2).length==1)
    })

  }

}