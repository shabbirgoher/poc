package in.shab.dynamodb

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import com.amazonaws.services.dynamodbv2.model._

object DynamoSupport {
  val dynamoEndpoint = "http://localhost:8000"
  val region = "ap-southeast-2"

  private val client: AmazonDynamoDBAsync = AmazonDynamoDBAsyncClientBuilder.standard().withEndpointConfiguration(
    new AwsClientBuilder
    .EndpointConfiguration(dynamoEndpoint, region))
    .build()

  case class DynamoTable(name: String, hashKey: String, rangeKey: Option[String])

  object DynamoTable {
    def apply(name: String, hashKey: String): DynamoTable = new DynamoTable(name, hashKey, None)
  }

  def withTable(tables: Seq[DynamoTable])(block: AmazonDynamoDBAsync => Unit): Unit = {
    try {
      tables.foreach { table =>
        val createTableRequest = new CreateTableRequest()
          .withTableName(table.name)
          .withKeySchema(
            new KeySchemaElement().withAttributeName(table.hashKey).withKeyType(KeyType.HASH))
          .withAttributeDefinitions(
            new AttributeDefinition().withAttributeName(table.hashKey).withAttributeType(ScalarAttributeType.S))
          .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
        table.rangeKey.foreach(key => createTableRequest.withKeySchema(
          new KeySchemaElement().withAttributeName(key).withKeyType(KeyType.RANGE))
          .withAttributeDefinitions(
            new AttributeDefinition().withAttributeName(key).withAttributeType(ScalarAttributeType.S)))
        client.createTable(createTableRequest)
      }
      block(client)
    } finally {
      tables.map(_.name).foreach(s => client.deleteTable(s))
    }
  }
}

