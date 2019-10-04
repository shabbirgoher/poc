package in.shab.example.kafka.recordnamestrategy

import java.time.Duration

import akka.kafka.ConsumerSettings
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import com.ovoenergy.kafka.serialization.avro4s2.avroBinarySchemaIdDeserializer

import scala.util.Try
import scala.collection.JavaConverters._

object Consumer extends App {
  //  val valueDes = avroBinarySchemaIdWithReaderSchemaDeserializer[ConsumerEvent](Constants.src, isKey = false, includesFormatByte = true, Constants.schemaConfig)
  implicit val src: SchemaRegistryClient = Constants.src
  val valueDes = new RecordNameValueDeserializer(avroBinarySchemaIdDeserializer[ConsumerEvent](Constants.src, isKey = false,
    includesFormatByte = true, Constants.schemaConfig))
  val keyDes = avroBinarySchemaIdDeserializer[String](Constants.src, isKey = true, includesFormatByte = true)
  val consumer = ConsumerSettings(Constants.appConfig.getConfig("akka.kafka.consumer"), keyDes, new ByteArrayDeserializer())
    .withBootstrapServers(Constants.kafkaEndpoint)
    .withGroupId(Constants.consumerGroupId)
    .withProperties(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest")
    .createKafkaConsumer()
  consumer.subscribe(Seq(Constants.topicName).asJava)

  private val records: Iterable[ConsumerRecord[String, Array[Byte]]] = consumer.poll(Duration.ofSeconds(2)).asScala
  println(s"Consumed records count ${records.size}")

  records.foreach(record => {
    val value = Try(valueDes.deserialize(Constants.topicName, record.value()))
    println(s"Consumed record key ${record.key()} with value $value")

  })
}
