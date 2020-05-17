
package in.shab.dynamodb

import in.shab.dynamodb.DynamoSupport.DynamoTable
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.scalatest.{Matchers, WordSpec}
import org.scanamo.semiauto._
import org.scanamo.{DynamoFormat, Scanamo, Table}

import scala.collection.JavaConverters._
import scala.util.Random

case class AClass(key1: DbKey, key2: String, key3: BClass)

case class AClass2(key1: DbKey, key2: String, key3: Set[String])

case class AClass3(key1: DbKey, key2: String, key3: Option[String])

object AClass {
  implicit val format: DynamoFormat[AClass] = deriveDynamoFormat[AClass]
}

object AClass2 {
  implicit val format: DynamoFormat[AClass2] = deriveDynamoFormat[AClass2]
}

object AClass3 {
  implicit val format: DynamoFormat[AClass3] = deriveDynamoFormat[AClass3]
}

case class BClass(key1: String, key2: String)

object BClass {
  implicit val format: DynamoFormat[BClass] = deriveDynamoFormat[BClass]
}

class ScanamoUpdateOpsGeneratorSpec extends WordSpec with Matchers {

  "StatementGenerator" should {
    import ScanamoUpdateOpsGenerator._

    "generate keys and update expression for a case class" in {
      val tableName = Random.alphanumeric.take(5).mkString
      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key1"))) { client =>
        val a = AClass(DbKey("value1"), "value2", BClass("value3", "value4"))
        val scanamoOps = Table[AClass](tableName).updateProduct(a)

        Scanamo.exec(client)(scanamoOps) shouldBe Right(a)

        val item = client.getItem(tableName, Map("key1" -> new AttributeValue().withS(a.key1.value)).asJava).getItem

        item shouldBe AClass.format.write(a).getM
      }
    }

    "append for set type" in {
      val tableName = Random.alphanumeric.take(5).mkString
      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key1"))) { client =>
        val a1 = AClass2(DbKey("value1"), "value2", Set("hello1"))
        val a2 = AClass2(DbKey("value1"), "value2", Set("hello2"))
        val scanamoOps1 = Table[AClass2](tableName).updateProduct(a1)
        val scanamoOps2 = Table[AClass2](tableName).updateProduct(a2)

        Scanamo.exec(client)(scanamoOps1) shouldBe Right(a1)
        Scanamo.exec(client)(scanamoOps1) shouldBe Right(a1)
        Scanamo.exec(client)(scanamoOps2) shouldBe Right(a2.copy(key3 = Set[String]("hello1", "hello2")))

        val item = client.getItem(tableName, Map("key1" -> new AttributeValue().withS(a1.key1.value)).asJava).getItem
        item shouldBe AClass2.format.write(a1.copy(key3 = Set[String]("hello1", "hello2"))).getM
      }
    }

    "ignore None values" in {
      val tableName = Random.alphanumeric.take(5).mkString
      DynamoSupport.withTable(Seq(DynamoTable(tableName, "key1"))) { client =>
        val a = AClass3(DbKey("value1"), "value2", None)

        val scanamoOps = Table[AClass3](tableName).updateProduct(a)

        Scanamo.exec(client)(scanamoOps) shouldBe Right(a)

        val item = client.getItem(tableName, Map("key1" -> new AttributeValue().withS(a.key1.value)).asJava).getItem
        item shouldBe AClass3.format.write(a).getM
      }
    }
  }
}
