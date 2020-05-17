import sbt.Keys._
import sbt._

val libName = "kafka-dynamo-connector"

val minorVersion = SettingKey[String]("minor version")
minorVersion := "0"

lazy val root = Project(libName, file("."))
  .settings(
    scalacOptions += "-Ypartial-unification",
    scalaVersion := "2.12.6",
    crossScalaVersions := Seq("2.11.11", "2.12.6"),
    organization := "in.shab",
    name := libName,
    version := s"1.0.${minorVersion.value}",
    libraryDependencies ++= Dependencies(),
    resolvers ++= Dependencies.resolvers,
    coverageMinimum := 100.00,
    coverageFailOnMinimum := true,
    credentials += Dependencies.credentials,
    BintraySettings()
  )
  .settings(Dynamo.settings)
