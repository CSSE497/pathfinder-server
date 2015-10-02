package io.pathfinder.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;

import java.util.HashMap;
import java.util.Map;

public class WebSocketDelegator extends UntypedActor {
    // Every web socket request will have a "delegate" field which relates the message to which
    // "entity" it is applicable for. Note that these entities relate to actions, not necessarily
    // data models. Defining everything as static for now b/c I want to get this working ASAP but we
    // could make this more robust in the future if Dan really wants to.
    private static final String DELEGATE_FIELD = "delegate";
    private static final String VEHICLE = "vehicle";
    private static final String COMMODITY = "commodity";
    private static final String CLUSTER = "cluster";

    private static final Map<String, Class> socketControllers;
    static {
        socketControllers = new HashMap<>();
        socketControllers.put(CLUSTER, ClusterSocket.class);
        socketControllers.put(VEHICLE, ClusterSocket.class);
        socketControllers.put(COMMODITY, ClusterSocket.class);
    }

    private final ActorRef out;

    public static Props props(ActorRef out) {
        return Props.create(WebSocketDelegator.class, out);
    }

    public WebSocketDelegator(ActorRef out) {
        this.out = out;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (!(message instanceof ObjectNode)) {
            out.tell("All socket requests must be JSON.", self());
            Logger.info(String.format("Recieved non-JSON request: %s" , message));
            return;
        }
        ObjectNode json = (ObjectNode) message;
        if (json.get(DELEGATE_FIELD) == null || !socketControllers.containsKey(json.get(DELEGATE_FIELD).asText())) {
            out.tell("Must specify delegate field.", self());
            Logger.info(String.format("Received JSON request without delegate field: %s", json));
            return;
        }
        Class socketController = socketControllers.get(json.get(DELEGATE_FIELD).asText());
        Logger.info(String.format("Routing socket request to %s", socketController.getName()));
        getContext().actorOf(Props.create(socketController, out)).tell(message, self());
    }
}
