import sbt.{Credentials, _}

object Dependencies {


  var resolvers = Seq(
    Resolver.bintrayRepo("hseeberger", "maven"),
    "confluent" at "http://packages.confluent.io/maven/"
  )

  val akkaVersion = "2.5.26"
  val akkaHttpVersion = "10.1.10"
  val circeVersion = "0.9.3"
  val kafkaVersion = "2.3.1"

  val appDependencies: Seq[ModuleID] = Seq(
    "com.github.racc" % "typesafeconfig-guice" % "0.1.0" exclude("com.google.inject", "guice") exclude("com.google.code.findbugs", "annotations"),
    "net.logstash.logback" % "logstash-logback-encoder" % "5.1",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "de.heikoseeberger" %% "akka-http-circe" % "1.21.1",
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "com.sandinh" %% "akka-guice" % "3.2.0",
    "com.typesafe" % "config" % "1.3.4",
    "com.github.racc" % "typesafeconfig-guice" % "0.1.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.2",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.apache.kafka" % "kafka-clients" % kafkaVersion,
    "io.confluent" % "kafka-avro-serializer" % "5.3.0"
  )

  val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % "test,it",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test,it",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test,it",
    "org.mockito" % "mockito-core" % "2.20.0" % "test,it",
    "com.itv" %% "scalapact-scalatest" % "2.3.2" % "contest",
    "com.itv" %% "scalapact-http4s-0-18" % "2.3.2" % "contest",
    "com.itv" %% "scalapact-circe-0-9" % "2.3.2" % "contest"
  )

  def apply() = appDependencies ++ testDependencies
}
