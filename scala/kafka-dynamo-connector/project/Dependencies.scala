import sbt.{Credentials, _}

object Dependencies {
  var resolvers = Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.bintrayRepo("ovotech", "maven"),
    "confluent" at "http://packages.confluent.io/maven/"
  )

  val appDependencies: Seq[ModuleID] = Seq(
    "org.scanamo" %% "scanamo" % "1.0.0-M9",
    "com.chuusai" %% "shapeless" % "2.3.3",
    "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-RC1"
  )

  val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "org.apache.kafka" %% "kafka" % "2.1.0" % "test",
    "org.apache.kafka" %% "kafka" % "2.1.0" % "test" classifier "test",
    "org.apache.kafka" % "kafka-clients" % "2.1.0" % "test",
    "org.apache.kafka" % "kafka-clients" % "2.1.0" % "test" classifier "test",
    "com.landoop" %% "kafka-testing" % "2.1" % "test"
  )

  def apply() = appDependencies ++ testDependencies
}
