package in.shab.example.kafka.recordnamestrategy

import com.sksamuel.avro4s.{AvroName, AvroNamespace}

sealed trait ConsumerEvent

@AvroNamespace("com.shab.poc")
@AvroName("Event1")
case class ConsumerEvent1(type1: String) extends ConsumerEvent

@AvroNamespace("com.shab.poc")
@AvroName("Event2")
case class ConsumerEvent2(type3: String) extends ConsumerEvent

