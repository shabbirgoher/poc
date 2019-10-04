package in.shab.example.kafka.recordnamestrategy

import com.sksamuel.avro4s.{AvroName, AvroNamespace}

sealed trait ProducerEvent

@AvroNamespace("com.shab.poc")
@AvroName("Event1")
case class ProducerEvent1(type1: String) extends ProducerEvent


@AvroNamespace("com.shab.poc")
@AvroName("Event2")
case class ProducerEvent2(type2: String) extends ProducerEvent

@AvroNamespace("com.shab.poc")
@AvroName("Event3")
case class ProducerEvent3(type3: String) extends ProducerEvent
