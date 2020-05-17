package in.shab.example.kafka.recordnamestrategy

import java.nio.ByteBuffer

import com.sksamuel.avro4s.SchemaFor
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.avro.Schema
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer

class RecordNameValueDeserializer[T](deserializer: Deserializer[T])(implicit schemaFor: SchemaFor[T], schemaRegistry: SchemaRegistryClient)
  extends Deserializer[T] {
  private val schema: Schema = schemaFor.schema

  override def deserialize(topic: String, data: Array[Byte]): T = {
    if (schema.getType != Schema.Type.UNION || schema.getIndexNamed(writerSchemaName(data)) != null) {
      deserializer.deserialize(topic, data)
    }
    else {
      throw NoSchemaFoundForTheMessage
    }
  }

  def writerSchemaName(data: Array[Byte]): String = {
    val buffer = this.getByteBuffer(data)
    val id = buffer.getInt
    this.schemaRegistry.getById(id).getFullName
  }

  private def getByteBuffer(payload: Array[Byte]) = {
    val buffer = ByteBuffer.wrap(payload)
    if (buffer.get != 0) throw new SerializationException("Unknown magic byte!")
    else buffer
  }
}
