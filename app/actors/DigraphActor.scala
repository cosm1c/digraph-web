package actors

import javax.inject.Singleton

import actors.DigraphActor._
import akka.actor.{Actor, ActorLogging}
import domain.digraph._
import play.api.libs.json._

@Singleton
class DigraphActor() extends Actor with ActorLogging {

  // TODO: Implementation to obtain initial diGraph
  private val initialNodes: Set[Node] = Set(
    Node("root", Seq("a", "g"), Unknown),
    Node("a", Seq("b", "d", "h"), Unknown),
    Node("b", Seq("c"), Unknown),
    Node("c", Nil, Unknown),
    Node("d", Seq("e", "f"), Unknown),
    Node("e", Nil, Unknown),
    Node("f", Nil, Unknown),
    Node("g", Seq("h"), Unknown),
    Node("h", Nil, Unknown)
  )

  private val eventBus = new DigraphEventBus

  private var digraph: Map[String, Node] = initialNodes.map(node => node.id -> node).toMap

  override def receive = {

    case Subscribe =>
      log.debug("Subscribe {}", sender())
      sender ! currentSnapshot()
      eventBus.subscribe(sender(), self)

    case Unsubscribe =>
      log.debug("Unsubscribe {}", sender())
      eventBus.unsubscribe(sender())

    case addNodes: AddNodes =>
      log.debug("AddNodes {}", addNodes)
      eventBus.publish(DigraphEventEnvelope(self, addNodes))
      digraph ++= addNodes.nodes.map(node => node.id -> node).toMap

    case updates: UpdateNodes =>
      log.debug("NodeUpdates {}", updates)
      eventBus.publish(DigraphEventEnvelope(self, updates))
      updates.updates.foreach(updateDigraph)

    case StartNode(nodeId) =>
      log.info("StartNode {}", nodeId)

    case StopNode(nodeId) =>
      log.info("StopNode {}", nodeId)
  }

  def currentSnapshot(): AddNodes = AddNodes(digraph.values.toSeq)

  private def updateDigraph(update: NodeUpdate): Unit = digraph += update.id -> digraph(update.id).copy(state = update.state)
}

object DigraphActor {

  case object Subscribe

  case object Unsubscribe

  case class StartNode(nodeId: String)

  case class StopNode(nodeId: String)


  sealed abstract class DigraphEvent

  case class AddNodes(nodes: Seq[Node]) extends DigraphEvent

  case class UpdateNodes(updates: Seq[NodeUpdate]) extends DigraphEvent


  implicit val addNodesWrites = new Writes[AddNodes] {
    override def writes(addNodes: AddNodes): JsValue = JsArray(Seq(
      JsString("add"),
      JsArray(
        addNodes.nodes.map(Json.toJson(_))
      )
    ))
  }

  implicit val stateChangeWrites = new Writes[UpdateNodes] {
    override def writes(updateNodes: UpdateNodes): JsValue = JsArray(Seq(
      JsString("update"),
      JsArray(
        updateNodes.updates.map(Json.toJson(_))
      )
    ))
  }

}
