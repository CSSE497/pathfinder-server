package io.pathfinder.websockets;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import io.pathfinder.BaseAppTest;
import io.pathfinder.models.Cluster;
import io.pathfinder.models.Commodity;
import io.pathfinder.models.Vehicle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

/**
 * This test was based off of the documentation at
 * http://doc.akka.io/docs/akka/snapshot/java/testing.html
 */
@RunWith(JUnit4.class)
public class WebSocketActorTest extends BaseAppTest {
    private TestActorRef<WebSocketActor> socket;
    private TestProbe client;

    private static final JsValue JSON_CREATE_CLUSTER =
        Json.parse("{\"create\":{\"model\":\"Cluster\",\"value\":{}}}");
    private static final JsValue JSON_CREATE_VEHICLE =
        Json.parse("{\"create\":{\"model\":\"Vehicle\",\"value\":{\"latitude\":0.1,\"longitude\":-12.3,\"clusterId\":1,\"capacity\":99}}}");
    private static final JsValue JSON_CREATE_COMMODITY =
        Json.parse("{\"create\":{\"model\":\"Commodity\",\"value\":{\"startLatitude\":0.1,\"startLongitude\":-12.3,\"endLatitude\":99.4,\"endLongitude\":-3.5,\"clusterId\":1,\"param\":5}}}");
    private static final JsValue JSON_GET_CLUSTERS =
        Json.parse("{\"getApplicationCluster\":{\"id\":\""+APPLICATION_ID+"\"}}");

    private static final int TIMEOUT = 3000;

    @Before
    public void initActor() {
        ActorSystem sys = ActorSystem.create();
        client = new TestProbe(sys);
        final Props props = WebSocketActor.props(client.ref());
        socket = TestActorRef.create(sys, props);
    }

    @Test
    public void testCreateCluster() throws Exception {
        final int NEXT_UNUSED_ID = 2;
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_CREATE_CLUSTER).get(), TIMEOUT);
        Cluster createdCluster = new Cluster();
        createdCluster.id_$eq(NEXT_UNUSED_ID);
        client.expectMsg(new WebSocketMessage.Created(
            ModelTypes.Cluster(), Cluster.format().writes(createdCluster)));
    }

    @Test
    public void testCreateVehicle() {
        final int NEXT_UNUSED_ID = 1;
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_CREATE_VEHICLE).get(), TIMEOUT);
        Vehicle createdVehicle = new Vehicle();
        createdVehicle.id_$eq(NEXT_UNUSED_ID);
        createdVehicle.latitude_$eq(0.1);
        createdVehicle.longitude_$eq(-12.3);
        createdVehicle.capacity_$eq(99);
        client.expectMsgClass(WebSocketMessage.Routed.class);
        client.expectMsg(new WebSocketMessage.Created(
            ModelTypes.Vehicle(), Vehicle.format().writes(createdVehicle)));
    }

    @Test
    public void testCreateCommodity() {
        final int NEXT_UNUSED_ID = 1;
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_CREATE_COMMODITY).get(), TIMEOUT);
        Commodity createdCommodity = new Commodity();
        createdCommodity.id_$eq(NEXT_UNUSED_ID);
        createdCommodity.startLatitude_$eq(0.1);
        createdCommodity.startLongitude_$eq(-12.3);
        createdCommodity.endLatitude_$eq(99.4);
        createdCommodity.endLongitude_$eq(-3.5);
        createdCommodity.param_$eq(5);
        client.expectMsg(new WebSocketMessage.Created(
            ModelTypes.Commodity(), Commodity.format().writes(createdCommodity)));
    }

    @Test
    public void testGetClusters() {
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_GET_CLUSTERS).get(), TIMEOUT);
        client.expectMsg(new WebSocketMessage.ApplicationCluster(APPLICATION_ID,1L));
    }
}
