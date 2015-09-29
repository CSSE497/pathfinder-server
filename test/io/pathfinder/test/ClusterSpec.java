package io.pathfinder.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.pathfinder.models.Commodity;
import io.pathfinder.models.Vehicle;
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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.pathfinder.models.Cluster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ClusterSpec {
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

    @Before
    public void setup() {
        fakeApp = Helpers.fakeApplication(Helpers.inMemoryDatabase("default"));
    }

    @After
    public void teardown() {
        Helpers.stop(fakeApp);
    }

    public Vehicle createVehicle(long id) {
        Vehicle vehicle = new Vehicle();
        Random rand = new Random();

        return vehicle.apply(id, rand.nextDouble(), rand.nextDouble(), rand.nextInt());
    }

    public Commodity createCommodity() {
        Commodity commodity = new Commodity();
        Random rand = new Random();

        commodity.startLatitude = rand.nextDouble();
        commodity.endLatitude = rand.nextDouble();
        commodity.startLongitude = rand.nextDouble();
        commodity.endLongitude = rand.nextDouble();
        commodity.param = rand.nextInt();

        return commodity;
    }

    public Cluster createClusters() {
        Cluster mainCluster = new Cluster();
        Cluster cluster1 = new Cluster();
        Cluster cluster2 = new Cluster();
        Cluster cluster3 = new Cluster();
        Cluster cluster4 = new Cluster();

        Vehicle vehicle1 = createVehicle(0);
        Vehicle vehicle2 = createVehicle(1);
        Vehicle vehicle3 = createVehicle(2);

        Commodity commodity1 = createCommodity();
        Commodity commodity2 = createCommodity();
        Commodity commodity3 = createCommodity();

        cluster1.parent = mainCluster;
        cluster4.parent = mainCluster;
        cluster2.parent = cluster1;
        cluster3.parent = cluster2;

        mainCluster.subClusters.add(cluster1);
        mainCluster.subClusters.add(cluster4);
        cluster1.subClusters.add(cluster2);
        cluster2.subClusters.add(cluster3);

        mainCluster.vehicles.add(vehicle1);
        cluster1.vehicles.add(vehicle2);
        cluster2.vehicles.add(vehicle3);

        mainCluster.commodities.add(commodity1);
        cluster1.commodities.add(commodity2);
        cluster2.commodities.add(commodity3);

        return mainCluster;
    }

    @Test
    public void ebeanModelShouldBeValid() {
        Cluster mainCluster = createClusters();

        mainCluster.save();

        assertEquals(5, Cluster.find.all().size());
        assertEquals(3, Vehicle.finder().all().size());
        assertEquals(3, Commodity.find.all().size());
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
            assertTrue("db record should have clusters", resultJson.hasNonNull("subClusters"));
            assertTrue("db record should have vehicles", resultJson.hasNonNull("vehicles"));
            assertTrue("db record should have commodities", resultJson.hasNonNull("commodities"));
            assertTrue("db record should have parent", !resultJson.hasNonNull("parent"));

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
            List<Cluster> clusters = new LinkedList<Cluster>();

            for (int i = 0; i < resultJson.size(); i++) {
                ObjectNode clusterNode = (ObjectNode) resultJson.get(i);
                clusters.add(Json.fromJson(clusterNode, Cluster.class));
            }

            assertEquals("Should have returned five clusters", 5, clusters.size());
        });
    }

    @Test
    public void getByExistingIDShouldReturnCluster() {
        Helpers.running(fakeApp, () -> {
            createClusters().save();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster/1");
            Result result = Helpers.route(request);

            assertEquals("Get cluster by id should return status 200", 200, result.status());

            ObjectNode resultJson = (ObjectNode) bodyForResult(result);
            Cluster cluster = Json.fromJson(resultJson, Cluster.class);

            assertNotNull("Get by id should return a result", cluster);
        });
    }

    @Test
    public void getByInvalidIdShouldReturnNotFound() {
        Helpers.running(fakeApp, () -> {
            createClusters().save();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster/100");
            Result result = Helpers.route(request);

            assertEquals("Get by invalid should return status 404", 404, result.status());
        });
    }

    @Test
    public void putShouldUpdateClusters() {
        Helpers.running(fakeApp, () -> {
            createClusters().save();

            ObjectNode body = JsonNodeFactory.instance.objectNode();
            Cluster newCluster = new Cluster();
            newCluster.subClusters = new LinkedList<Cluster>();
            JsonNode newClusters = Json.toJson(newCluster);

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.PUT)
                    .uri("/cluster/1")
                    .bodyJson(newClusters);
            Result result = Helpers.route(request);

            assertEquals("Successful Put should return no content code", 204, result.status());

            Cluster cluster = Cluster.find.byId(1L);
            assertEquals("Commodity PUT changes should persist in the db", 0, cluster.subClusters.size());

            RequestBuilder invalidIdRequest = new RequestBuilder()
                    .method(Helpers.PUT)
                    .uri("/cluster/500")
                    .bodyJson(body);
            Result invalidIdResult = Helpers.route(invalidIdRequest);
            assertEquals("Put to invalid id should 404", 404, invalidIdResult.status());
        });
    }

    @Test
    public void deleteShouldDeleteCluster() {
        Helpers.running(fakeApp, () -> {
            createClusters().save();

            RequestBuilder deleteRequest = new RequestBuilder()
                    .method(Helpers.DELETE)
                    .uri("/cluster/1");
            Result deleteResult = Helpers.route(deleteRequest);

            assertEquals("valid DELETE cluster request should return status 200", 200, deleteResult.status());

            RequestBuilder getRequest = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/cluster/1");
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
