package io.pathfinder.controllers

import akka.actor.{Props, ActorRef}
import io.pathfinder.websockets.{WebSocketActor, WebSocketMessage}
import play.api.mvc.{Results, RequestHeader, WebSocket, Controller}
import play.Logger

import scala.concurrent.Future

object Application {
    private def handlerProps(appId: String, useAuth: Boolean): WebSocket.HandlerProps = WebSocketActor.props(_, appId, useAuth)
}

class Application extends Controller {
    import io.pathfinder.websockets.WebSocketMessage.frameFormat
    import play.api.Play.current
    import Application._

    def socket = WebSocket.tryAcceptWithActor[WebSocketMessage,WebSocketMessage] { request: RequestHeader =>
        Future.successful(
            request.headers.get("Authorization").map{ h =>
                Logger.info("Connection using id: " + h + ", from authorization header"); h
            }.orElse(
                request.cookies.get("AppId").map{ c =>
                    Logger.info("Connection using id: " + c.value + ", from cookie")
                    c.value
                }
            ).orElse(
                request.getQueryString("AppId").map{ id =>
                    Logger.info("Connection using id: " + id + ", from query string")
                    id
                }
            ).orElse {
                Logger.info("Using default id: 7d8f2ead-ee48-45ef-8314-3c5bebd4db82")
                Some("7d8f2ead-ee48-45ef-8314-3c5bebd4db82")
            }.flatMap(
                appId =>
                    Option(io.pathfinder.models.Application.finder.byId(appId)).map{
                        app =>
                            val useAuth = !request.getQueryString("auth").map("false".equals).getOrElse(false)
			    Right(handlerProps(appId, useAuth))
                    }
            ).getOrElse(Left(Results.Unauthorized))
        )
    }
}
