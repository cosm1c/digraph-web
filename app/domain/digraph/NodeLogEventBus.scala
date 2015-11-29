package domain.digraph

import akka.event.{ActorEventBus, LookupClassification}

class NodeLogEventBus extends ActorEventBus with LookupClassification {
  type Event = LogEntryEnvelope
  type Classifier = String

  override protected def classify(event: Event): Classifier = event.nodeId

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  override protected val mapSize: Int = 128

}

case class LogEntryEnvelope(nodeId: String, logLine: String)
