package io.pathfinder.websockets;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import io.pathfinder.BaseAppTest;
import io.pathfinder.models.Cluster;
import io.pathfinder.models.Commodity;
import io.pathfinder.models.Transport;
import io.pathfinder.models.TransportStatus;
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
                        "\"id\":\"" + ROOT + "/subcluster\"" +
                    "}}");
    private static final JsValue JSON_CREATE_TRANSPORT =
        Json.parse("{\"message\":\"Create\"," +
                    "\"model\":\"Transport\"," +
                    "\"value\":{" +
                        "\"latitude\":0.1," +
                        "\"longitude\":-12.3," +
                        "\"clusterId\":\"" + ROOT + "\"," +
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
                        "\"clusterId\":\"" + ROOT + "\"," +
                        "\"metadata\":{\"param\":5}" +
                    "}}");

    private static final int TIMEOUT = 3000;

    @Before
    public void initActor() {
        ActorSystem sys = ActorSystem.create();
        client = new TestProbe(sys);
        final Props props = WebSocketActor.props(client.ref(), APPLICATION_ID);
        socket = TestActorRef.create(sys, props);
    }

    @Test
    public void testCreateCluster() throws Exception {
        final String PATH = ROOT + "/subcluster";
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_CREATE_CLUSTER).get(), TIMEOUT);
        Cluster createdCluster = new Cluster();
        createdCluster.id_$eq(PATH);
        client.expectMsg(new WebSocketMessage.Created(
            ModelTypes.Cluster(), Cluster.format().writes(createdCluster)));
    }

    @Test
    public void testCreateTransport() {
        final int NEXT_UNUSED_ID = 1;
        Patterns.ask(socket, WebSocketMessage.format().reads(JSON_CREATE_TRANSPORT).get(), TIMEOUT);
        Transport createdTransport = new Transport();
        createdTransport.id_$eq(NEXT_UNUSED_ID);
        createdTransport.latitude_$eq(0.1);
        createdTransport.longitude_$eq(-12.3);
        Map<String,JsValue> meta = new HashMap<>();
        meta.put("capacity", new JsNumber(BigDecimal.valueOf(99)));
        createdTransport.metadata_$eq(new JsObject(meta));
        createdTransport.status_$eq(TransportStatus.Online);
        createdTransport.cluster_$eq(baseCluster());
        client.expectMsg(new WebSocketMessage.Created(
                ModelTypes.Transport(), Transport.format().writes(createdTransport)));
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
        createdCommodity.cluster_$eq(baseCluster());
        Map<String,JsValue> meta = new HashMap<>();
        meta.put("param", new JsNumber(BigDecimal.valueOf(5)));
        createdCommodity.metadata_$eq(new JsObject(meta));
        client.expectMsg(new WebSocketMessage.Created(
            ModelTypes.Commodity(), Commodity.format().writes(createdCommodity)));
    }
}
