

package in.shab.kafkaview.healthcheck

import akka.actor.{Actor, Props}
import in.shab.kafkaview.healthcheck.HealthCheckActor.{HealthCheckRequest, HealthCheckResponse}
import com.github.racc.tscg.TypesafeConfig
import javax.inject.Inject

object HealthCheckActor {

  val name: String = "HealthCheckActor"

  def props(appVersion: String): Props = Props(new HealthCheckActor(appVersion))

  case object HealthCheckRequest
  case class HealthCheckResponse(version: String, ok: Boolean)
}

class HealthCheckActor @Inject()(@TypesafeConfig("application.version")appVersion: String) extends Actor {
  override def receive: Receive = {
    case HealthCheckRequest => sender() ! HealthCheckResponse(appVersion, true)
  }
}
