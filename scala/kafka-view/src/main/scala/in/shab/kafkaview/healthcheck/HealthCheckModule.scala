

package in.shab.kafkaview.healthcheck

import akka.actor.{Actor, ActorSystem}
import com.google.inject.AbstractModule
import com.google.inject.name.Names

/**
  * A Guice module for the counting actor and service.
  */
class HealthCheckModule extends AbstractModule {
  override def configure() {
    bind(classOf[Actor]).annotatedWith(Names.named(HealthCheckActor.name)).to(classOf[HealthCheckActor])
  }
}
