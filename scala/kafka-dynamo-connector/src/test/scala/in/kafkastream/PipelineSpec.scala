package in.shab.kafkastream

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import in.shab.dynamodb.DynamoSupport.DynamoTable
import in.shab.dynamodb.{DbKey, DynamoSupport}
import in.shab.kafka.AvroSerialization._
import cats.implicits._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.landoop.kafka.testing.KCluster
import com.sksamuel.avro4s.{Encoder, SchemaFor}
import com.typesafe.config.ConfigFactory
import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClient}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Assertion, BeforeAndAfterAll, Matchers, WordSpec}
import org.scanamo.error.DynamoReadError
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._
import org.scanamo.{DynamoFormat, Table}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.Random

class PipelineSpec extends WordSpec with Matchers with BeforeAndAfterAll with Eventually {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(6, Seconds)), interval = scaled(Span(1, Seconds)))

  private val config = ConfigFactory.load()
  val kafka = new KCluster()
  private lazy val producer = ProducerSettings(config.getConfig("akka.kafka.producer"), new ByteArraySerializer(), new ByteArraySerializer())
    .withBootstrapServers(kafka.BrokersList).createKafkaProducer()
  implicit lazy val schemaRegistryClient: SchemaRegistryClient = new CachedSchemaRegistryClient(kafka.SchemaRegistryService.get.Endpoint, 5)

  override def afterAll(): Unit = {
    producer.close()
    kafka.close()
    super.afterAll()
  }


  "Pipeline" should {
    "kafka to db to commit offset" in new Setup {
      val event = Event1("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        val pipeline = KafkaToDynamo.sourceToDbToCommit(kafkaConfig, Set(topic), config.getConfig("akka.kafka.consumer.committer"))(client, tableName, Events.eventToDbEntity(tableName))
          .run()

        verifyDb(client, "key" -> event.id, EventsEntity(DbKey(event.id), event.value))
        pipeline.stop()
      }
    }

    "Source with Db" in new Setup {
      val event = Event1("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        var eventProcessed: Seq[Wrapper[Events, EventsEntity]] = Seq.empty
        val pipeline = KafkaToDynamo.sourceWithDb[Events, EventsEntity](kafkaConfig, Set(topic))(client, tableName, Events.eventToDbEntity(tableName))
          .collect { case Right(WithKafkaCommit(wrapper, _)) => wrapper }
          .map(t => eventProcessed = eventProcessed :+ t)
          .toMat(Sink.ignore)(Keep.left).run()

        verifyDb(client, "key" -> event.id, EventsEntity(DbKey(event.id), event.value))
        eventProcessed.length shouldBe 1
        pipeline.stop()
      }
    }

    "write simple case class to db" in new Setup {
      val event = Event1("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        var eventProcessed: Seq[Wrapper[Event1, EventsEntity]] = Seq.empty
        val pipeline = KafkaToDynamo.kafkaSource[Event1](kafkaConfig, Set(topic))
          .via(KafkaToDynamo.dbFlow(client, tableName)(Event1.eventToDbEntity(tableName)))
          .collect { case Right(WithKafkaCommit(wrapper, _)) => wrapper }
          .map(t => eventProcessed = eventProcessed :+ t)
          .toMat(Sink.ignore)(Keep.left).run()

        verifyDb(client, "key" -> event.id, EventsEntity(DbKey(event.id), event.value))
        eventProcessed.length shouldBe 1
        pipeline.stop()
      }
    }

    "with trait hierarchy" in new Setup {
      val event = Event1("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        var eventProcessed: Seq[Wrapper[Events, EventsEntity]] = Seq.empty
        val pipeline = KafkaToDynamo.kafkaSource[Events](kafkaConfig, Set(topic))
          .via(KafkaToDynamo.dbFlow(client, tableName)(Events.eventToDbEntity(tableName)))
          .collect { case Right(WithKafkaCommit(wrapper, _)) => wrapper }
          .map(t => eventProcessed = eventProcessed :+ t)
          .toMat(Sink.ignore)(Keep.left).run()

        verifyDb(client, "key" -> event.id, EventsEntity(DbKey(event.id), event.value))
        eventProcessed.length shouldBe 1
        pipeline.stop()
      }
    }

    "emit deserialization errors" in new Setup {
      val event = NonEvent("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        var eventProcessed: Seq[KafkaDeserializationError] = Seq.empty

        val pipeline = KafkaToDynamo.kafkaSource[Events](kafkaConfig, Set(topic))
          .via(KafkaToDynamo.dbFlow(client, tableName)(_ => throw new RuntimeException("should not be reach here")))
          .collect { case Left(WithKafkaCommit(error: KafkaDeserializationError, _)) => error }
          .map(t => eventProcessed = eventProcessed :+ t)
          .toMat(Sink.ignore)(Keep.left).run()

        eventually(eventProcessed.length shouldBe 1)
        pipeline.stop()
      }
    }

    "emit dynamo read error" in new Setup {
      val event = Event1("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        var eventProcessed: Seq[DbReadError] = Seq.empty
        client.putItem(tableName, Map("key" -> new AttributeValue().withS(event.id)).asJava)
        val table = Table[EventsEntity](tableName).get('key -> event.id)
        val scanamoOps: ScanamoOps[Either[PipelineError, EventsEntity]] = table.map(_.get.leftMap(error => DbReadError(DynamoReadError.describe(error))))

        val value = KafkaToDynamo.dbFlow(client, tableName)(_ => scanamoOps)
        val pipeline = KafkaToDynamo.kafkaSource[Events](kafkaConfig, Set(topic))
          .via(value)
          .collect { case Left(WithKafkaCommit(error: DbReadError, _)) => error }
          .map(t => eventProcessed = eventProcessed :+ t)
          .toMat(Sink.ignore)(Keep.left).run()

        eventually(eventProcessed.length shouldBe 1)
        pipeline.stop()
      }
    }

    "emit dynamo query errors" in new Setup {
      val event = Event1("1", "some-value")
      produceRecord(topic, event.id, event)

      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key"))) { client =>
        var eventProcessed: Seq[DbError] = Seq.empty

        val pipeline = KafkaToDynamo.kafkaSource[Events](kafkaConfig, Set(topic))
          .via(KafkaToDynamo.dbFlow(client, tableName)(_ => throw new Exception("failed while writing to db")))
          .collect { case Left(WithKafkaCommit(error: DbError, _)) => error }
          .map(t => eventProcessed = eventProcessed :+ t)
          .toMat(Sink.ignore)(Keep.left).run()

        eventually(eventProcessed.length shouldBe 1)
        pipeline.stop()
      }
    }
  }

  private def produceRecord[K: Encoder : SchemaFor, V: Encoder : SchemaFor](topic: String, key: K, value: V) =
    producer.send(new ProducerRecord(topic, keySerializer[K].serialize(topic, key), valueSerializer[V].serialize(topic, value)))

  trait Setup {
    val tableName: String = Random.alphanumeric.take(5).mkString
    val topic: String = Random.alphanumeric.take(5).mkString
    val kafkaConfig = KafkaConsumerConfig(kafka.BrokersList, config.getConfig("akka.kafka.consumer"), "group-1")

    def verifyDb[T](client: AmazonDynamoDBAsync, key: (String, String), value: T)(implicit format: DynamoFormat[T]): Assertion = {
      eventually {
        val item = client.getItem(tableName, Map(key._1 -> new AttributeValue().withS(key._2)).asJava).getItem
        Option(item) should not be None
        format.read(new AttributeValue().withM(item)) shouldBe Right(value)
      }
    }
  }

}
