
package in.shab.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.scanamo.error.DynamoReadError
import org.scanamo.ops.ScanamoOps
import org.scanamo.query.UniqueKey
import org.scanamo.syntax.{set, _}
import org.scanamo.update.UpdateExpression
import org.scanamo.{DynamoFormat, Table}
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Lub, Witness}

case class UpdateQuery(key: UniqueKey[_], updateExpression: UpdateExpression)

case class DbKey(value: String)

object DbKey {
  implicit val format: DynamoFormat[DbKey] = new DynamoFormat[DbKey] {
    override def read(av: AttributeValue): Either[DynamoReadError, DbKey] = DynamoFormat.stringFormat.read(av).map(DbKey(_))

    override def write(t: DbKey): AttributeValue = DynamoFormat.stringFormat.write(t.value)
  }
}

trait FieldLister[A] {
  def updateExpression(a: A): List[UpdateExpression]

  def keys(a: A): List[(Symbol, String)]
}

trait FieldLister2LowPriority {

  implicit def primitiveFieldLister[K <: Symbol, H: DynamoFormat, T <: HList](implicit
                                                                              witnessKey: Witness.Aux[K],
                                                                              tLister: FieldLister[T],
                                                                              valueLub: Lub[H, H, H]
                                                                             ): FieldLister[FieldType[K, H] :: T] = new FieldLister[FieldType[K, H] :: T] {
    override def updateExpression(a: FieldType[K, H] :: T): List[UpdateExpression] = {
      val h = valueLub.left(a.head)

      set(witnessKey.value -> h) :: tLister.updateExpression(a.tail)
    }

    override def keys(a: FieldType[K, H] :: T): List[(Symbol, String)] = tLister.keys(a.tail)
  }
}

object FieldLister extends FieldLister2LowPriority {
  implicit val hNilLister: FieldLister[HNil] = new FieldLister[HNil] {
    override def updateExpression(a: HNil): List[UpdateExpression] = Nil

    override def keys(a: HNil): List[(Symbol, String)] = Nil
  }

  implicit def primitiveFieldListerKeys[K <: Symbol, T <: HList](implicit
                                                                 witnessKey: Witness.Aux[K],
                                                                 tLister: FieldLister[T],
                                                                 valueLub: Lub[DbKey, DbKey, DbKey]
                                                                ): FieldLister[FieldType[K, DbKey] :: T] = new FieldLister[FieldType[K, DbKey] :: T] {
    override def updateExpression(a: FieldType[K, DbKey] :: T): List[UpdateExpression] = tLister.updateExpression(a.tail)

    override def keys(a: FieldType[K, DbKey] :: T): List[(Symbol, String)] = (witnessKey.value -> valueLub.left(a.head).value) :: tLister.keys(a.tail)
  }

  implicit def primitiveFieldSetLister[K <: Symbol, T <: HList](implicit
                                                                witnessKey: Witness.Aux[K],
                                                                tLister: FieldLister[T],
                                                                valueLub: Lub[Set[String], Set[String], Set[String]]
                                                               ): FieldLister[FieldType[K, Set[String]] :: T] =
    new FieldLister[FieldType[K, Set[String]] :: T] {
      override def updateExpression(a: FieldType[K, Set[String]] :: T): List[UpdateExpression] =
        add(witnessKey.value -> valueLub.left(a.head)) :: tLister.updateExpression(a.tail)

      override def keys(a: FieldType[K, Set[String]] :: T): List[(Symbol, String)] = tLister.keys(a.tail)
    }

  implicit def primitiveFieldOptionLister[K <: Symbol, V: DynamoFormat, T <: HList](implicit
                                                                                    witnessKey: Witness.Aux[K],
                                                                                    tLister: FieldLister[T],
                                                                                    valueLub: Lub[Option[V], Option[V], Option[V]]
                                                                                   ): FieldLister[FieldType[K, Option[V]] :: T] =
    new FieldLister[FieldType[K, Option[V]] :: T] {
      override def updateExpression(a: FieldType[K, Option[V]] :: T): List[UpdateExpression] =
        valueLub.left(a.head).map(v => set(witnessKey.value -> v)).foldLeft(tLister.updateExpression(a.tail))((tail, thisV) => thisV :: tail)


      override def keys(a: FieldType[K, Option[V]] :: T): List[(Symbol, String)] = tLister.keys(a.tail)
    }

  implicit def genericLister[A, R](implicit
                                   gen: LabelledGeneric.Aux[A, R],
                                   lister: Lazy[FieldLister[R]]
                                  ): FieldLister[A] = new FieldLister[A] {
    override def updateExpression(a: A): List[UpdateExpression] = lister.value.updateExpression(gen.to(a))

    override def keys(a: A): List[(Symbol, String)] = lister.value.keys(gen.to(a))
  }
}

trait ScanamoUpdateOpsGenerator[A] {
  def updateOperation(a: A): UpdateQuery
}

object ScanamoUpdateOpsGenerator {
  def apply[A](implicit sg: ScanamoUpdateOpsGenerator[A]): ScanamoUpdateOpsGenerator[A] = sg

  implicit def genericGenerator[A: DynamoFormat](implicit
                                                 fieldLister: FieldLister[A]
                                                ): ScanamoUpdateOpsGenerator[A] = (value: A) => {
    UpdateQuery(fieldLister.keys(value).head, fieldLister.updateExpression(value).reduce(_ and _))
  }

  implicit class TableExtension[A](table: Table[A]) {
    def updateProduct(a: A)(implicit sg: ScanamoUpdateOpsGenerator[A]): ScanamoOps[Either[DynamoReadError, A]] = {
      val query = sg.updateOperation(a)
      table.update(query.key, query.updateExpression)
    }
  }

}
