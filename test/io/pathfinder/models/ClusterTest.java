package io.pathfinder.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import play.libs.Json;
import play.mvc.Result;
import play.test.Helpers;
import play.mvc.Http.RequestBuilder;
import play.test.FakeApplication;
import play.core.j.JavaResultExtractor;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import scala.collection.JavaConversions;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ClusterTest {
    private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private FakeApplication fakeApp;

    private JsonNode bodyForResult(Result r) {
        String resultBody;

        try {
            resultBody = new String(JavaResultExtractor.getBody(r, 0L), "UTF-8");

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(resultBody);
        } catch(IOException e) {
            fail("Could not process database record");
            return null;
        }
    }

    static int id_count = 1;

    @Before
    public void setup() {
        fakeApp = Helpers.fakeApplication(Helpers.inMemoryDatabase("default"));
    }

    @After
    public void teardown() {
        Helpers.stop(fakeApp);
    }

    public Vehicle createVehicle() {
        Random rand = new Random();
        return Vehicle.apply(id_count++, rand.nextDouble(), rand.nextDouble(), VehicleStatus.Online, rand.nextInt());
    }

    public Commodity createCommodity() {
        Random rand = new Random();
        return Commodity.apply(id_count++, rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), 0);
    }

    private static <T> scala.collection.mutable.Buffer newList() {
        return wrap(new LinkedList<T>());
    }

    private static <T> scala.collection.mutable.Buffer wrap(List<T> list) {
        return (JavaConversions.<T>asScalaBuffer(list));
    }

    public Cluster createClusters() {
        Commodity commodity1 = createCommodity();
        Commodity commodity2 = createCommodity();
        Commodity commodity3 = createCommodity();
        commodity1.save();
        commodity2.save();
        commodity3.save();

        Vehicle vehicle1 = createVehicle();
        Vehicle vehicle2 = createVehicle();
        Vehicle vehicle3 = createVehicle();
        vehicle1.save();
        vehicle2.save();
        vehicle3.save();

        Cluster cluster4 = Cluster.apply(id_count++, newList(), newList());
        cluster4.save();
        Cluster cluster3 = Cluster.apply(id_count++, newList(), newList());
        cluster3.save();
        Cluster cluster2 = Cluster.apply(id_count++, wrap(Arrays.asList(vehicle3.id())), wrap(Arrays.asList(commodity3.id())));
        cluster2.save();
        Cluster cluster1 = Cluster.apply(id_count++, wrap(Arrays.asList(vehicle2.id())), wrap(Arrays.asList(commodity2.id())));
        cluster1.save();
        Cluster mainCluster = Cluster.apply(id_count++, newList(), newList());
        mainCluster.save();
        return mainCluster;
    }

    @Test
    public void ebeanModelShouldBeValid() {
        createClusters();
        assertEquals(5, Cluster.finder().all().size());
        assertEquals(3, Vehicle.finder().all().size());
        assertEquals(3, Commodity.finder().all().size());
    }

    @Test
    public void validPostShouldCreateClusterEasy() {
        Helpers.running(fakeApp, () -> {
            ObjectNode body = jsonNodeFactory.objectNode();

            RequestBuilder request = new RequestBuilder()
                    .bodyJson(body)
                    .header("Content-Type", "application/json")
                    .method(Helpers.POST)
                    .uri("/cluster");

            Result result = Helpers.route(request);

            // Check for 'Created' Status Code
            assertEquals(201, result.status());

            ObjectNode resultJson = (ObjectNode) bodyForResult(result);

            // Ensure that all fields were correctly written to the database
            assertTrue("db record should have id", resultJson.hasNonNull("id"));

        });
    }

    @Test
    public void validPostShouldCreateClusterHard() {
        Helpers.running(fakeApp, () -> {

            Cluster mainCluster = createClusters();
            JsonNode jsonCluster = Json.toJson(mainCluster);

            RequestBuilder request = new RequestBuilder()
                    .bodyJson(jsonCluster)
                    .header("Content-Type", "application/json")
                    .method(Helpers.POST)
                    .uri("/cluster");

            Result result = Helpers.route(request);
            assertEquals(201, result.status());

            ObjectNode resultJson = (ObjectNode) bodyForResult(result);
            assertTrue("db record should have vehicles", resultJson.hasNonNull("vehicles"));
            assertTrue("db record should have commodities", resultJson.hasNonNull("commodities"));
        });
    }

    @Test
    public void getShouldReturnAllCluster() {
        Helpers.running(fakeApp, () -> {
            Cluster cluster = createClusters();
            cluster.save();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster");
            Result result = Helpers.route(request);

            assertEquals("Get all clusters should return status 200", 200, result.status());

            ArrayNode resultJson = (ArrayNode) bodyForResult(result);
            assertEquals("Should have returned five clusters", 5, resultJson.size());
        });
    }

    @Test
    public void getByExistingIDShouldReturnCluster() {
        Helpers.running(fakeApp, () -> {
            long existingId = createClusters().id();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster/"+String.valueOf(existingId));
            Result result = Helpers.route(request);

            assertEquals("Get cluster by id should return status 200", 200, result.status());
            ObjectNode resultJson = (ObjectNode) bodyForResult(result);
            Cluster cluster = Cluster.format().reads(play.api.libs.json.Json.parse(resultJson.toString())).get();
            assertNotNull("Get by id should return a result", cluster);
        });
    }

    @Test
    public void getByInvalidIdShouldReturnNotFound() {
        Helpers.running(fakeApp, () -> {
            createClusters();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster/1000");
            Result result = Helpers.route(request);

            assertEquals("Get by invalid should return status 404", 404, result.status());
        });
    }

    @Test
    public void deleteShouldDeleteCluster() {
        Helpers.running(fakeApp, () -> {
            long existingId = createClusters().id();

            RequestBuilder deleteRequest = new RequestBuilder()
                    .method(Helpers.DELETE)
                    .uri("/cluster/" + String.valueOf(existingId));
            Result deleteResult = Helpers.route(deleteRequest);

            assertEquals("valid DELETE cluster request should return status 200", 200, deleteResult.status());

            RequestBuilder getRequest = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster/" + String.valueOf(existingId));
            Result getResult = Helpers.route(getRequest);

            assertEquals("GET req for deleted cluster should 404", 404, getResult.status());

            RequestBuilder deleteFakeRequest = new RequestBuilder()
                    .method(Helpers.DELETE)
                    .uri("/cluster/500");
            Result deleteFakeResult = Helpers.route(deleteFakeRequest);

            assertEquals("Deleting nonexistent cluster should return 404", 404, deleteFakeResult.status());
        });
    }

}
