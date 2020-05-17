import sbt.Keys.{baseDirectory, scalaSource}

scalacOptions ++= Seq("-deprecation", "-feature")

lazy val ContractTest = config("contest") extend(Test)
configs(ContractTest)

lazy val `kafkaview` = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "kafka-view",
    Common.settings,
    Defaults.itSettings ++ headerSettings(IntegrationTest) ++ automateHeaderSettings(IntegrationTest),
    resolvers ++= Dependencies.resolvers,
    libraryDependencies ++= Dependencies(),
    Assembly.settings,
    credentials += Dependencies.credentials,
    coverageExcludedPackages := "<empty>;controllers\\..*Reverse.*;router.Routes.*;",
    coverageMinimum := 95.00,
    coverageFailOnMinimum := true,
    scalastyleFailOnError := true,
    scalastyleFailOnWarning := true,
    Project.inConfig(ContractTest)(baseAssemblySettings),
    inConfig(ContractTest)(Defaults.testSettings ++ headerSettings(ContractTest) ++ automateHeaderSettings(ContractTest)),
    ContractTest / publishArtifact := true
  )
  .settings(configureTestDir(IntegrationTest, "it"):_*)
  .settings(configureTestDir(ContractTest, "contest"):_*)
  .enablePlugins(AutomateHeaderPlugin)

addCommandAlias("fullTest", ";coverage;test;it:test;coverageReport;coverageOff")
addCommandAlias("fullBuild", ";clean;headerCreate;test:headerCreate;it:headerCreate;scalastyle;coverage;test;it:test;coverageReport;coverageOff")

def configureTestDir(conf: Configuration, src: String): Seq[SettingsDefinition] = {
  Seq(
    conf / scalaSource := baseDirectory.value / "src" / src / "scala",
    conf / resourceDirectory := baseDirectory.value / "src" / src / "resources"
  )
}

