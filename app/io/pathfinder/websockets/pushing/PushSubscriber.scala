package io.pathfinder.websockets.pushing

import akka.actor.ActorRef

trait PushSubscriber {
  def subscribeByClusterPath(path: String, client: ActorRef): Unit

  def subscribeById(id: Long, client: ActorRef): Unit

  def unsubscribeById(id: Long, client: ActorRef): Unit

  def unsubscribeByClusterPath(path: String, client: ActorRef): Unit

  def unsubscribe(client: ActorRef): Unit
}
