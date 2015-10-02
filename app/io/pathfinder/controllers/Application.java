package io.pathfinder.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.pathfinder.actors.WebSocketDelegator;
import play.mvc.Controller;
import play.mvc.WebSocket;

public class Application extends Controller {
    public WebSocket<JsonNode> socket() {
        return WebSocket.withActor(WebSocketDelegator::props);
    }
}
