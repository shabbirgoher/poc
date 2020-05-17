

package in.shab.kafkaview.setup

import akka.actor.ActorSystem
import in.shab.kafkaview.setup.AkkaModule.ActorSystemProvider
import com.google.inject.{AbstractModule, Injector, Provider}
import com.typesafe.config.Config
import javax.inject.Inject

object AkkaModule {
  class ActorSystemProvider @Inject() (val config: Config, val injector: Injector) extends Provider[ActorSystem] {
    override def get(): ActorSystem = {
      val system = ActorSystem("kafka-view", config)
      // add the GuiceAkkaExtension to the system, and initialize it with the Guice injector
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }
}

/**
  * A module providing an Akka ActorSystem.
  */
class AkkaModule extends AbstractModule {

  override def configure() {
    bind(classOf[ActorSystem]).toProvider(classOf[ActorSystemProvider]).asEagerSingleton()
  }
}
