package io.pathfinder.controllers

import io.pathfinder.websockets.{WebSocketActor, WebSocketMessage}
import play.api.mvc.{WebSocket, Controller}

class Application extends Controller {
  import io.pathfinder.websockets.WebSocketMessage.frameFormat
  import play.api.Play.current

  def socket = WebSocket.acceptWithActor[WebSocketMessage,WebSocketMessage] {
    request => out =>
      WebSocketActor.props(out)
  }
}
