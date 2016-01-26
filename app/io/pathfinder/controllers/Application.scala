package io.pathfinder.controllers

import akka.actor.{Props, ActorRef}
import io.pathfinder.websockets.{WebSocketActor, WebSocketMessage}
import play.api.mvc.{Results, RequestHeader, WebSocket, Controller}
import play.Logger

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
            request.headers.get("Authorization").map{
                h => Logger.info("Connection using id: " + h + ", from authorization header"); h
            }.orElse(
                request.cookies.get("AppId").map{
                    c =>  Logger.info("Connection using id: " + c.value + ", from cookie"); c.value
                }
            ).orElse{Logger.info("Using default id: 7d8f2ead-ee48-45ef-8314-3c5bebd4db82"); Some("7d8f2ead-ee48-45ef-8314-3c5bebd4db82")}.flatMap(
                appId =>
                    Option(io.pathfinder.models.Application.finder.byId(appId)).map(
                        app => Right(handlerProps(appId))
                    )
            ).getOrElse(Left(Results.Unauthorized))
        )
    }
}
