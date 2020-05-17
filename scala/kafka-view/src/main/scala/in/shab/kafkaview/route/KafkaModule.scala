

package in.shab.kafkaview.route

import com.github.racc.tscg.TypesafeConfig
import com.google.inject.{AbstractModule, Provides}
import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClient}

object KafkaModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Kafka]).to(classOf[KafkaImp])
  }

  //noinspection ScalaStyle
  @Provides
  def src(@TypesafeConfig("kafka.schema-reg-url") schemaRegUrl: String): SchemaRegistryClient =
    new CachedSchemaRegistryClient(schemaRegUrl, 5)
}
