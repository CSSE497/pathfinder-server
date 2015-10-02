package io.pathfinder.websockets.controllers

import io.pathfinder.websockets.WebSocketMessage

/**
 * The websocket anologue to the MVC controllers, for now it only supports Long keys since that seems
 * to be the only thing we use
 */
trait WebSocketController{
  def receive: PartialFunction[WebSocketMessage,Option[WebSocketMessage]]
}
