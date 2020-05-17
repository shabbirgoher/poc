package in.shab.kafkastream

import akka.kafka.ConsumerMessage
import cats.implicits._
import cats.{Applicative, Eval, Traverse}

case class WithKafkaCommit[T](flowedObject: T, commitable: ConsumerMessage.CommittableOffset)

object WithKafkaCommit {
  implicit val functorForWithKafkaCommit: Traverse[WithKafkaCommit] = new Traverse[WithKafkaCommit] {
    override def map[A, B](fa: WithKafkaCommit[A])(f: A => B): WithKafkaCommit[B] = fa.copy(flowedObject = f(fa.flowedObject))

    override def traverse[G[_], A, B](fa: WithKafkaCommit[A])(f: A => G[B])(implicit evidence$1: Applicative[G]): G[WithKafkaCommit[B]] = {
      f(fa.flowedObject).map(b => fa.copy(flowedObject = b))
    }

    override def foldLeft[A, B](fa: WithKafkaCommit[A], b: B)(f: (B, A) => B): B = f(b, fa.flowedObject)

    override def foldRight[A, B](fa: WithKafkaCommit[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = f(fa.flowedObject, lb)
  }

  implicit class PimpedT[T](t: T) {
    implicit def withKafkaMessage[K, V](implicit kafkaMessage: ConsumerMessage.CommittableMessage[K, V]): WithKafkaCommit[T] =
      WithKafkaCommit(t, kafkaMessage.committableOffset)
  }
}
