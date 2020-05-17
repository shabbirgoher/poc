package in.shab.kafkastream

sealed trait PipelineError {
  def message: String

  def throwable: Option[Throwable]
}

case class KafkaDeserializationError(message: String, t: Throwable) extends PipelineError {
  override def throwable: Option[Throwable] = Some(t)
}

case class DbReadError(message: String) extends PipelineError {
  override def throwable: Option[Throwable] = None
}

case class DbError(message: String, t: Throwable) extends PipelineError {
  override def throwable: Option[Throwable] = Some(t)
}
