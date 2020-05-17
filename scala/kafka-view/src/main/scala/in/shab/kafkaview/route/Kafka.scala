

package in.shab.kafkaview.route

import java.io.ByteArrayOutputStream
import java.time.Duration

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.{Directives, Route}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import com.github.racc.tscg.TypesafeConfig
import com.typesafe.config.Config
import io.circe.parser.parse
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import javax.inject.Inject
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.{SpecificDatumWriter, SpecificRecord}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.TopicPartition
import cats.implicits._

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait Kafka extends Directives {
  val system: ActorSystem
  implicit val logger: LoggingAdapter = Logging(system, classOf[KafkaImp])
  private val maxPollMillis = 100
  private val maxFetchRecord = 100

  val kafkaBootstrap: String
  val src: SchemaRegistryClient
  val consumerConfig: Config
  private val settings = ConsumerSettings(consumerConfig, new KafkaAvroDeserializer(src), new KafkaAvroDeserializer(src))
    .withBootstrapServers(kafkaBootstrap)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
  val route: Route = pathPrefix("api") {
    path("topic" / Segment) { topic =>
      get {
        parameter("search".?) { search =>
          complete {
            Consumer.plainPartitionedSource(settings.withGroupId(), Subscriptions.topics(topic))
              .flatMapConcat { case (tp, source) => source.map(tp -> _) }
              .map { case (tp, r) =>
                r -> getRecordValueAsString(tp, r)
              }
              .filter { case (_, event) => search.fold(true)(event.contains) }
              .map {
                case (r, event) =>
                  val jsonStr = toResponseJson(r.topic(), r.partition(), r.offset(), r.timestamp(), event)
                  parse(jsonStr)
              }
              .take(maxFetchRecord)
          }
        }
      } ~
        path("partition" / Segment / "offset" / Segment) { (partitionStr, offsetStr) =>
          get {
            complete {
              (Try(partitionStr.toInt), Try(offsetStr.toLong)) mapN {
                case (partition, offset) =>
                  val consumer = settings
                    .createKafkaConsumer()
                  val topicPartition = new TopicPartition(topic, partition)
                  consumer.assign(List(topicPartition).asJava)
                  consumer.seek(topicPartition, offset)
                  consumer.poll(Duration.ofMillis(maxPollMillis))
                    .asScala
                    .map(getRecordValueAsString(topicPartition, _))

              }
            }
          }
        }

    }
  }

  private def getRecordValueAsString(tp: TopicPartition, record: ConsumerRecord[AnyRef, AnyRef]) = {
    val recordValue = record.value()
    val triedString: Try[String] = recordValue match {
      case str: String => Success(str)
      case record: GenericRecord => convertToJsonStream(record)
        .map(_.toString("UTF-8"))
        .recoverWith { case t => Failure(new Throwable(s"Failed to parse the object $recordValue at $tp", t)) }
      case _ => Failure(new Throwable(s"Can't convert kafka $recordValue to sting for $tp"))
    }
    triedString fold(_.getMessage, identity)
  }

  private def convertToJsonStream(record: GenericRecord): Try[ByteArrayOutputStream] =
    Try {
      val outputStream = new ByteArrayOutputStream

      val jsonEncoder = new EncoderFactory().jsonEncoder(record.getSchema, outputStream)
      val writer = if (record.isInstanceOf[SpecificRecord]) {
        new SpecificDatumWriter[GenericRecord](record.getSchema)
      }
      else {
        new GenericDatumWriter[GenericRecord](record.getSchema)
      }
      writer.write(record, jsonEncoder)
      jsonEncoder.flush()

      outputStream
    }

  private def toResponseJson(topic: String, partition: Int, offset: Long, timestamp: Long, event: String): String = {
      s"""
         |{
         | "topic": "$topic",
         | "partition": "$partition",
         | "offset": "$offset",
         | "timestamp": $timestamp,
         | "event": $event
         |}
         |""".stripMargin.stripLineEnd
  }
}

class KafkaImp @Inject()(@TypesafeConfig("kafka.bootstrap") val kafkaBootstrap: String,
                         @TypesafeConfig("akka.kafka.consumer") val consumerConfig: Config,
                         val src: SchemaRegistryClient,
                         val system: ActorSystem) extends Kafka
