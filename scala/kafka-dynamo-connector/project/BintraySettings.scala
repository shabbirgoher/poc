import sbt._
import Keys._
import bintray.BintrayKeys._

object BintraySettings {
  def apply() = Seq(
    bintrayRepository := {
      val candidateOrRelease = if(version.value.contains("-")) "release-candidates" else "releases"
      if (sbtPlugin.value) s"sbt-plugin-$candidateOrRelease" else candidateOrRelease
    },
    bintrayPackage := name.value,
    bintrayOmitLicense := true
  )
}
