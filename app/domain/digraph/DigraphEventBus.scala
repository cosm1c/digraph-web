package domain.digraph

import actors.DigraphActor.DigraphEvent
import akka.actor.ActorRef
import akka.event.{ActorClassifier, ActorEventBus, LookupClassification}

class DigraphEventBus extends ActorEventBus with ActorClassifier with LookupClassification {
  type Event = DigraphEventEnvelope

  override protected def classify(event: Event): Classifier = event.digraphActor

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  override protected val mapSize: Int = 128

}

case class DigraphEventEnvelope(digraphActor: ActorRef, digraphEvent: DigraphEvent)
