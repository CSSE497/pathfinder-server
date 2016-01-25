package io.pathfinder.controllers

import akka.actor.{Props, ActorRef}
import io.pathfinder.websockets.{WebSocketActor, WebSocketMessage}
import play.api.mvc.{Results, RequestHeader, WebSocket, Controller}

import scala.concurrent.Future

object Application {
    private def handlerProps(appId: String): WebSocket.HandlerProps = WebSocketActor.props(_, appId)
}

class Application extends Controller {
    import io.pathfinder.websockets.WebSocketMessage.frameFormat
    import play.api.Play.current
    import Application._

    def socket = WebSocket.tryAcceptWithActor[WebSocketMessage,WebSocketMessage] { request: RequestHeader =>
        Future.successful(
            request.headers.get("Authorization").orElse(request.cookies.get("AppId").map(_.value)).flatMap(
                appId =>
                    Option(io.pathfinder.models.Application.finder.byId(appId)).map(
                        app => Right(handlerProps(appId))
                    )
            ).getOrElse(Left(Results.Unauthorized))
        )
    }
}
