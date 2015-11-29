package controllers

import javax.inject.{Inject, Named, Singleton}

import actors.ClientWebSocketActor
import actors.DigraphActor.UpdateNodes
import akka.actor.ActorRef
import akka.util.Timeout
import domain.digraph.{NodeUpdate, Running, Stopped}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.duration._
import scala.util.Random

@Singleton
class WebSocketController @Inject()(@Named("DigraphActor") digraphActor: ActorRef) extends Controller {

  implicit val timeout = Timeout(5.seconds)

  // TODO: Replace with real event handling
  Akka.system.scheduler.schedule(0.microsecond, 333.millisecond) {
    def randomStateChange: UpdateNodes = {
      val nextChar: Char = ('a'.toInt + Random.nextInt(8)).toChar
      UpdateNodes(Seq(NodeUpdate(nextChar.toString, if (Random.nextBoolean()) Running else Stopped)))
    }
    digraphActor ! randomStateChange
  }

  def socket =
    WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
      ClientWebSocketActor.props(out, digraphActor)
    }
}
