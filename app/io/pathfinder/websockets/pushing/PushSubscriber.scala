package io.pathfinder.websockets.pushing

import akka.actor.ActorRef

trait PushSubscriber {
  def subscribeByCluster(clusterId: Long, client: ActorRef): Unit

  def subscribeById(id: Long, client: ActorRef): Unit
}
