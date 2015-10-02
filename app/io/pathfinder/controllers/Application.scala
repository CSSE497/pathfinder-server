package io.pathfinder.controllers

import io.pathfinder.models.Model
import io.pathfinder.websockets.{WebSocketActor, WebSocketMessage}
import play.api.mvc.{WebSocket, Controller, Action}

class ViewController extends Controller {
  import io.pathfinder.websockets.WebSocketMessage.frameFormat
  import play.api.Play.current

  def index = Action {
    Ok("Pathfinder Webservice")
  }

  def socket = WebSocket.acceptWithActor[WebSocketMessage,WebSocketMessage] {
    request => out =>
      WebSocketActor.props(out)
  }
}
