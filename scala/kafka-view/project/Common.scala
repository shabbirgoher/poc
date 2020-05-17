import sbt._
import Keys.{organizationHomepage, _}
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import java.util.Calendar

object Common {
  private val copyrightYear = Calendar.getInstance().get(Calendar.YEAR)
  val settings = Seq(
    organization := "in.shab",
    startYear := Some(2019),
    version := scala.sys.env.getOrElse("GIT_SHA1", "0.1.0-SNAPSHOT"),
    scalaVersion := "2.12.10",
    headerLicense := Some(HeaderLicense.Custom(
      s"".stripMargin
    ))
  )
}

