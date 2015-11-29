package domain.digraph

import play.api.libs.json._


case class Node(id: String, depends: Seq[String], state: State)

object Node {
  implicit val nodeWrites = new Writes[Node] {
    override def writes(node: Node): JsValue = JsObject(Seq(
      "id" -> Json.toJson(node.id),
      "depends" -> Json.toJson(node.depends),
      "state" -> Json.toJson(node.state.name)
    ))
  }
}


case class NodeUpdate(id: String, state: State)

object NodeUpdate {
  implicit val nodeUpdateWrites = new Writes[NodeUpdate] {
    override def writes(update: NodeUpdate): JsValue = JsObject(Seq(
      "id" -> Json.toJson(update.id),
      "state" -> Json.toJson(update.state.name)
    ))
  }
}


sealed abstract class State(val name: String)

case object Unknown extends State("unknown")

case object Running extends State("running")

case object Stopped extends State("stopped")
