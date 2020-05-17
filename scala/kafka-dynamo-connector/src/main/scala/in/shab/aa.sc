import shapeless._
import shapeless.ops.record.Keys
import shapeless.ops.hlist.Selector
import shapeless.tag._


class GetFieldName[A, F] {
  def get[L <: HList, KeyList <: HList](implicit
                                        // we need to be able to convert our case class `A` to b labelled hlist `L`
                                        ev: LabelledGeneric.Aux[A, L],
                                        // we grab the keys of the labelled `L`, into an hlist `KeyList`
                                        ev2: Keys.Aux[L, KeyList],
                                        // from the key list, we select the first field with b name matching `F`
                                        ev3: Selector[KeyList, Symbol with Tagged[F]]
                                       ): String = {
    val keys: KeyList = Keys[ev.Repr].apply()

    keys.select[Symbol with Tagged[F]].name
  }
}
def getFieldName[A, F]: GetFieldName[A, F] = new GetFieldName

case class C(b: Int)

getFieldName[C, Witness.`"b"`.T].get