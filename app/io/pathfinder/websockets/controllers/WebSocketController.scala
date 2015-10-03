package io.pathfinder.websockets.controllers

import io.pathfinder.websockets.WebSocketMessage

/**
 * The websocket anologue to the MVC controllers
 */
trait WebSocketController{
    def receive(webSocketMessage: WebSocketMessage): Option[WebSocketMessage]
}
