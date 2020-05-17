package in.shab.kafkastream

import in.shab.dynamodb.DbKey
import org.scanamo.{DynamoFormat, Table}
import org.scanamo.semiauto._
import in.shab.dynamodb.ScanamoUpdateOpsGenerator._
import org.scanamo.error.DynamoReadError
import org.scanamo.ops.ScanamoOps
import cats.implicits._

sealed trait Events

object Events {
  def eventToDbEntity(tableName: String)(event: Events): ScanamoOps[Either[PipelineError, EventsEntity]] = {
    event match {
      case ev: Event1 => Event1.eventToDbEntity(tableName)(ev)
    }
  }
}

case class Event1(id: String, value: String) extends Events

case class NonEvent(id: String, value: String)

object Event1 {
  def eventToDbEntity(tableName: String)(event: Event1): ScanamoOps[Either[PipelineError, EventsEntity]] = {
    import event._
    Table[EventsEntity](tableName).updateProduct(EventsEntity(DbKey(id), value))
      .map(_.leftMap(error => DbReadError(DynamoReadError.describe(error))))
  }
}


case class EventsEntity(key: DbKey, value: String)

object EventsEntity {
  implicit val format: DynamoFormat[EventsEntity] = deriveDynamoFormat[EventsEntity]
}
