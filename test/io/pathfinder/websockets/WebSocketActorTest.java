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
import io.pathfinder.models.VehicleStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import play.api.libs.json.JsNumber;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;
import scala.math.BigDecimal;

/**
 * This test was based off of the documentation at
 * http://doc.akka.io/docs/akka/snapshot/java/testing.html
 */
@RunWith(JUnit4.class)
public class WebSocketActorTest extends BaseAppTest {
    private TestActorRef<WebSocketActor> socket;
    private TestProbe client;

    private static final JsValue JSON_CREATE_CLUSTER =
        Json.parse("{\"message\":\"Create\"," +
                    "\"model\":\"Cluster\"," +
                    "\"value\":{" +
                        "\"id\":\"" + CLUSTER_PATH + "/subcluster\"" +
                    "}}");
    private static final JsValue JSON_CREATE_VEHICLE =
        Json.parse("{\"message\":\"Create\"," +
                    "\"model\":\"Vehicle\"," +
                    "\"value\":{" +
                        "\"latitude\":0.1," +
                        "\"longitude\":-12.3," +
                        "\"clusterId\":\"" + CLUSTER_PATH + "\"," +
                        "\"metadata\":{\"capacity\":99}," +
                        "\"status\":\"Online\"" +
                    "}}");
    private static final JsValue JSON_CREATE_COMMODITY =
        Json.parse("{\"message\":\"Create\"," +
                    "\"model\":\"Commodity\"," +
                    "\"value\":{" +
                        "\"startLatitude\":0.1," +
                        "\"startLongitude\":-12.3," +
                        "\"endLatitude\":99.4," +
                        "\"endLongitude\":-3.5," +
                        "\"clusterId\":\"" + CLUSTER_PATH + "\"," +
                        "\"metadata\":{\"param\":5}" +
                    "}}");
    private static final JsValue JSON_GET_APPLICATION_CLUSTER =
        Json.parse("{\"message\":\"GetApplicationCluster\", \"id\":\""+APPLICATION_ID+"\"}");

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
        final String PATH = CLUSTER_PATH + "/subcluster";
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_CREATE_CLUSTER).get(), TIMEOUT);
        Cluster createdCluster = new Cluster();
        createdCluster.id_$eq(PATH);
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
        Map<String,JsValue> meta = new HashMap<>();
        meta.put("capacity", new JsNumber(BigDecimal.valueOf(99)));
        createdVehicle.metadata_$eq(new JsObject(meta));
        createdVehicle.status_$eq(VehicleStatus.Online);
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
        Map<String,JsValue> meta = new HashMap<>();
        meta.put("param", new JsNumber(BigDecimal.valueOf(5)));
        createdCommodity.metadata_$eq(new JsObject(meta));
        client.expectMsg(new WebSocketMessage.Created(
            ModelTypes.Commodity(), Commodity.format().writes(createdCommodity)));
    }

    @Test
    public void testGetApplicationClusters() {
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_GET_APPLICATION_CLUSTER).get(), TIMEOUT);
        JsValue json = Json.parse("{" +
                "\"id\":\"" + CLUSTER_PATH + "\"," +
                "\"vehicles\":[]," +
                "\"commodities\":[]," +
                "\"subClusters\":[]" +
        "}");
        client.expectMsg(new WebSocketMessage.ApplicationCluster(APPLICATION_ID, json));
    }
}
