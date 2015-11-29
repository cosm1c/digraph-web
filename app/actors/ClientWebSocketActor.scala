package actors

import java.time.Instant

import actors.DigraphActor._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import domain.digraph._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class ClientWebSocketActor(out: ActorRef,
                           digraphActorRef: ActorRef) extends Actor with ActorLogging {

  val clientName = s"Client ${self.path.elements.drop(2).head}"
  log.info(s"[$clientName] Started")

  digraphActorRef ! Subscribe

  override def receive = {
    case DigraphEventEnvelope(_, updateNodes: UpdateNodes) =>
      log.debug("updateNodes {}", updateNodes)
      out ! Json.toJson(updateNodes)

    case addNodes: AddNodes =>
      log.debug("addNodes {}", addNodes)
      out ! Json.toJson(addNodes)

    case LogEntryEnvelope(nodeId, logLine) =>
      val logEntry: JsValue = JsArray(Seq(
        JsString("log"),
        JsObject(Seq(
          "nodeId" -> JsString(nodeId),
          "line" -> JsString(logLine)
        ))
      ))
      out ! logEntry

    case msg: JsValue =>
      // TODO: reduce this log level
      log.info("Message received: {}", msg)
      val array: JsArray = msg.as[JsArray]
      val eventName = array(0).as[JsString]
      val eventData: JsLookupResult = array(1)
      eventName.value match {
        case "startNode" =>
          digraphActorRef ! StartNode(eventData.get.as[JsString].value)

        case "stopNode" =>
          digraphActorRef ! StopNode(eventData.get.as[JsString].value)

        case "subscribeNode" =>
          log.info("subscribeNode")
          nodeLogEventBus.subscribe(self, eventData.get.as[JsString].value)

        case "unsubscribeNode" =>
          log.info("unsubscribeNode")
          nodeLogEventBus.unsubscribe(self, eventData.get.as[JsString].value)
      }

    case "tick" =>
      val entry: LogEntryEnvelope = randomLogEntry()
      nodeLogEventBus.publish(entry)
  }

  /*
   * TODO: Replace with actual log entries being posted to EventBus that lives elsewhere
   */
  private val nodeLogEventBus = new NodeLogEventBus
  context.system.scheduler.schedule(0.millis, 100.millis, self, "tick")

  private def randomLogEntry(): LogEntryEnvelope = {
    val nextChar: Char = ('a'.toInt + Random.nextInt(8)).toChar
    LogEntryEnvelope(nextChar.toString, s"[${Instant.now().toString}] Log from $nextChar")
  }

  override def postStop(): Unit = digraphActorRef ! Unsubscribe
}

object ClientWebSocketActor {
  def props(out: ActorRef, digraphActorRef: ActorRef) = Props(new ClientWebSocketActor(out, digraphActorRef))
}
