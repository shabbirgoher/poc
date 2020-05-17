

package in.shab.kafkaview.setup

import com.google.inject.{AbstractModule, Provider}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Binds the application configuration to the [[Config]] interface.
  *
  * The config is bound as an eager singleton so that errors in the config are detected
  * as early as possible.
  */
class ConfigModule(config: Config) extends AbstractModule {
  override def configure() {
    bind(classOf[Config]).toInstance(config)
  }
}
