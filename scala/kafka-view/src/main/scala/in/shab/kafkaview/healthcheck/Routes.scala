

package in.shab.kafkaview.healthcheck

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.event.Logging
import akka.http.scaladsl.server.Directives.{concat, pathEnd, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import in.shab.kafkaview.healthcheck.HealthCheckActor.{HealthCheckRequest, HealthCheckResponse}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

import scala.concurrent.duration._
import scala.language.postfixOps

object Routes {
  def apply(healthCheckActor: ActorRef)(implicit as: ActorSystem): Routes = new Routes() {
    override implicit def system: ActorSystem = as
    override val healthcheckActor: ActorRef = healthCheckActor
  }
}

trait Routes {

  import ErrorAccumulatingCirceSupport._
  import io.circe.generic.auto._

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  private implicit lazy val timeout = Timeout(1 second)

  val healthcheckActor: ActorRef

  lazy val routes: Route =
    pathPrefix("api") {
      concat(
        path("health") {
          pathEnd {
            concat(
              get {
                val response = (healthcheckActor ? HealthCheckRequest).mapTo[HealthCheckResponse]
                complete(response)
              }
            )
          }
        }
      )
    }
}
