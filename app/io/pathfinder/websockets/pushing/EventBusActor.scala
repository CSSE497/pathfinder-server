package io.pathfinder.websockets.pushing

import akka.actor.{Actor, ActorRef}
import akka.event.{LookupClassification, ActorEventBus}
import io.pathfinder.websockets.pushing.EventBusActor.EventBusMessage.{Publish, UnSubscribeAll, UnSubscribe, Subscribe}

object EventBusActor {
    abstract sealed class EventBusMessage

    object EventBusMessage {
        case class Subscribe(subscriber: ActorRef, to: Any) extends EventBusMessage
        case class UnSubscribe(subscriber: ActorRef, from: Any) extends EventBusMessage
        case class UnSubscribeAll(subscriber: ActorRef) extends EventBusMessage
        case class Publish(event: Any) extends EventBusMessage
    }
}

abstract class EventBusActor extends Actor with ActorEventBus {

    override def receive: Receive = super.receive.orElse {
        case Subscribe(sub, to)     => subscribe(sub, to.asInstanceOf[Classifier])
        case UnSubscribe(sub, from) => unsubscribe(sub, from.asInstanceOf[Classifier])
        case UnSubscribeAll(sub)    => unsubscribe(sub)
        case Publish(event)         => publish(event.asInstanceOf[Event])
    }
}
