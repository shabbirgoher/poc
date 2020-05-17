package in.shab.kafkastream

import java.util.concurrent.atomic.AtomicReference

import akka.NotUsed
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep, RestartSource, RunnableGraph, Sink, Source}
import in.shab.kafka.AvroSerialization.valueDeserializer
import cats.implicits._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.sksamuel.avro4s.Decoder
import com.typesafe.config.Config
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.scanamo.Scanamo
import org.scanamo.ops.ScanamoOps

import scala.concurrent.duration._
import scala.util.Try

case class KafkaConsumerConfig(kafkaEndpoint: String, consumerConfig: Config, groupId: String)

object KafkaToDynamo {

  import WithKafkaCommit._

  val control = new AtomicReference[Consumer.Control](Consumer.NoopControl)

  /*
  * Creates a kafka consumer and executes provided db operation and commits offset on success
  */
  def sourceToDbToCommit[IN: Decoder, OUT](kafkaConfig: KafkaConsumerConfig, topics: Set[String], committerConfig: Config)
                                          (client: AmazonDynamoDBAsync, tableName: String, eventToDbEntity: IN => ScanamoOps[Either[PipelineError, OUT]])
                                          (implicit src: SchemaRegistryClient): RunnableGraph[Consumer.Control] = {
    kafkaSource[IN](kafkaConfig, topics)
      .via(dbFlow(client, tableName)(eventToDbEntity))
      .collect { case Right(value) => value.commitable }
      .via(Committer.flow(committerSettings(committerConfig)))
      .toMat(Sink.ignore)(Keep.left)
  }

  /*
  * Creates a kafka consumer source which writes to db and provides both deserialize and db update objects
  */
  def sourceWithDb[IN: Decoder, OUT](kafkaConfig: KafkaConsumerConfig, topics: Set[String])
                                    (client: AmazonDynamoDBAsync, tableName: String, eventToDbEntity: IN => ScanamoOps[Either[PipelineError, OUT]])
                                    (implicit src: SchemaRegistryClient)
  : Source[Either[WithKafkaCommit[PipelineError], WithKafkaCommit[Wrapper[IN, OUT]]], Consumer.Control] =
    kafkaSource[IN](kafkaConfig, topics)
      .via(dbFlow(client, tableName)(eventToDbEntity))

  /*
  * Creates a kafka consumer source with deserialization
  */
  def kafkaSource[IN: Decoder](kafkaConfig: KafkaConsumerConfig, topics: Set[String])
                              (implicit src: SchemaRegistryClient): Source[Either[WithKafkaCommit[PipelineError], WithKafkaCommit[IN]], Consumer.Control] =
    RestartSource.onFailuresWithBackoff(
      minBackoff = 1 second,
      maxBackoff = 30 seconds,
      randomFactor = 0.2
    )(() => {
      Consumer.committableSource(consumerSettings(kafkaConfig), Subscriptions.topics(topics))
        .via(deserializationFlow[IN])
        .mapMaterializedValue(c => control.set(c))
    }).mapMaterializedValue(_ => control.get())

  def dbFlow[IN, OUT](client: AmazonDynamoDBAsync, tableName: String)
                     (f: IN => ScanamoOps[Either[PipelineError, OUT]]):
  Flow[Either[WithKafkaCommit[PipelineError], WithKafkaCommit[IN]], Either[WithKafkaCommit[PipelineError], WithKafkaCommit[Wrapper[IN, OUT]]], NotUsed] =
    Flow[Either[WithKafkaCommit[PipelineError], WithKafkaCommit[IN]]]
      .map(_.flatMap(t => {
        t.traverse(executeDbOps(client, f, _)).leftMap(WithKafkaCommit(_, t.commitable))
      }))

  def deserializationFlow[V: Decoder](implicit schemaRegistryClient: SchemaRegistryClient):
  Flow[ConsumerMessage.CommittableMessage[Array[Byte], Array[Byte]], Either[WithKafkaCommit[PipelineError], WithKafkaCommit[V]], NotUsed] = {
    Flow[ConsumerMessage.CommittableMessage[Array[Byte], Array[Byte]]]
      .map(implicit m => {
        Try(valueDeserializer[V].deserialize(m.record.topic(), m.record.value())).toEither.bimap(t =>
          KafkaDeserializationError(s"Value deserialization failed for ${m.record.topic()} ${m.record.partition()} ${m.record.offset()}", t)
            .asInstanceOf[PipelineError].withKafkaMessage,
          _.withKafkaMessage)
      })
  }

  private def consumerSettings(kafkaConfig: KafkaConsumerConfig) =
    ConsumerSettings(kafkaConfig.consumerConfig, new ByteArrayDeserializer(), new ByteArrayDeserializer()).withGroupId(kafkaConfig.groupId)
      .withBootstrapServers(kafkaConfig.kafkaEndpoint).withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private def committerSettings(committerConfig: Config) = CommitterSettings(committerConfig)

  private def executeDbOps[OUT, IN](client: AmazonDynamoDBAsync, f: IN => ScanamoOps[Either[PipelineError, OUT]], in: IN): Either[PipelineError, Wrapper[IN, OUT]] =
    Try(Scanamo.exec(client)(f(in))).toEither
      .leftMap(t => DbError("dynamo exception", t).asInstanceOf[PipelineError])
      .flatMap(out => out.map(Wrapper(in, _)))
}
