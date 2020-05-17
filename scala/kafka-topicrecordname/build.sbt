scalaVersion := "2.12.8"
name := "kafka-akka-poc"
organization := "com.shab"
version := "1.0"

resolvers += Resolver.bintrayRepo("ovotech", "maven")

libraryDependencies ++= {
  val kafkaSerializationV = "0.5.16"
  val akkaKafkaV = "1.0.5"
  Seq("org.typelevel" %% "cats-core" % "1.6.0",
    "com.typesafe.akka" %% "akka-stream-kafka" % akkaKafkaV,
//    "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.1",
    "com.ovoenergy" %% "kafka-serialization-core" % kafkaSerializationV,
    "com.ovoenergy" %% "kafka-serialization-avro4s2" % kafkaSerializationV)
}