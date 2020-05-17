package in.shab.example.kafka.recordnamestrategy

import com.typesafe.config.{Config, ConfigFactory}
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient

object Constants {

  val topicName = "topic.v1"
  val schemaRegistryUrl = "http://localhost:8081"
  val kafkaEndpoint = "localhost:9092"
  val src = new CachedSchemaRegistryClient(schemaRegistryUrl, 1)
  val schemaConfig: Map[String, String] = Map("value.subject.name.strategy" -> "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy")
  val appConfig: Config = ConfigFactory.load()
  val consumerGroupId = "consumer.v1"

}
