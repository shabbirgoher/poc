package in.shab.example.kafka.recordnamestrategy

import akka.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord
import com.ovoenergy.kafka.serialization.avro4s2.avroBinarySchemaIdSerializer
import org.apache.kafka.clients.producer.{Producer => KProducer}

object Producer extends App {
  val valueSerializer = avroBinarySchemaIdSerializer[ProducerEvent](Constants.src, isKey = false, includesFormatByte = true,
    Constants.schemaConfig)

  val keySerializer = avroBinarySchemaIdSerializer[String](Constants.src, isKey = true, includesFormatByte = true)
  private val producer: KProducer[String, ProducerEvent] = ProducerSettings(Constants.appConfig.getConfig("akka.kafka.producer"),
    keySerializer, valueSerializer)
    .withBootstrapServers(Constants.kafkaEndpoint)
    .createKafkaProducer()

  producer.send(new ProducerRecord(Constants.topicName, "1", ProducerEvent1("event1-1")))
  producer.send(new ProducerRecord(Constants.topicName, "2", ProducerEvent2("event2-1")))
  producer.send(new ProducerRecord(Constants.topicName, "3", ProducerEvent1("event1-2")))

  Thread.sleep(1000)
}
