

package in.shab.kafkaview

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import in.shab.kafkaview.healthcheck.{HealthCheckActor, HealthCheckModule}
import in.shab.kafkaview.route.{Kafka, KafkaModule}
import in.shab.kafkaview.setup.{AkkaModule, ConfigModule, GuiceAkkaExtension}
import com.google.inject.Guice
import com.github.racc.tscg.TypesafeConfigModule
import akka.http.scaladsl.server.Directives.concat

object WebBootstrapper extends App {
  // $COVERAGE-OFF$
  Server.run()
  // $COVERAGE-ON$
}

object Server {

  def run(): () => Unit = {
    import com.typesafe.config.ConfigFactory

    val config = ConfigFactory.load()
    val injector = Guice.createInjector(
      new ConfigModule(config),
      TypesafeConfigModule.fromConfigWithPackage(config, getClass.getPackage.getName),
      new AkkaModule(),
      new HealthCheckModule(),
      KafkaModule
    )

    implicit val system = injector.getInstance(classOf[ActorSystem])
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val logger = Logging(system, "Server")

    val port = config.getInt("application.port")
    val healthCheckActor = system.actorOf(GuiceAkkaExtension(system).props(HealthCheckActor.name))

    val healthCheckRoute: Route = healthcheck.Routes(healthCheckActor).routes
    val kafkaRoute: Route = injector.getInstance(classOf[Kafka]).route
    val routes = concat(healthCheckRoute, kafkaRoute)
    val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", port)

    bindingFuture.map(binding => logger.info(s"Starting server on ${binding.localAddress}"))
    () =>
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
  }
}
