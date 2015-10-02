package io.pathfinder.actors;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;

public class ClusterSocket extends UntypedActor {
    private final ActorRef out;

    public ClusterSocket(ActorRef out) {
        this.out = out;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        Logger.info(String.format("ClusterSocket received request: %s", message));
        if (!(message instanceof ObjectNode)) {
            out.tell("All socket requests must be JSON.", self());
            Logger.info(String.format("Recieved non-JSON request: %s", message));
            return;
        }
        ObjectNode json = (ObjectNode) message;
    }
}
