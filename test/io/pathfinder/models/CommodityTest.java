package io.pathfinder.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import play.mvc.Result;
import play.test.Helpers;
import play.mvc.Http.RequestBuilder;
import play.test.FakeApplication;
import play.core.j.JavaResultExtractor;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedList;
import java.util.List;

public class CommodityTest {
    private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private FakeApplication fakeApp;

    private JsonNode bodyForResult(Result r) {
        String resultBody;

        try {
            resultBody = new String(JavaResultExtractor.getBody(r, 0L), "UTF-8");

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(resultBody);
        } catch(Exception e) {
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

    @Test
    public void ebeanModelShouldBeValid() {
        Commodity commodity = Commodity.apply(1, 1.0, 1.0, 1.0, 1.0, 1);
        commodity.save();
        assertEquals(1, Commodity.finder().all().size());
    }

    @Test
    public void validPostShouldCreateCommodity() {
        Helpers.running(fakeApp, () -> {
            ObjectNode body = jsonNodeFactory.objectNode();

            body.put("startLatitude", 6.0);
            body.put("startLongitude", 7.0);
            body.put("endLatitude", 8.0);
            body.put("endLongitude", 9.0);
            body.put("param", 42);

            RequestBuilder request = new RequestBuilder()
                    .bodyJson(body)
                    .header("Content-Type", "application/json")
                    .method(Helpers.POST)
                    .uri("/commodity");

            Result result = Helpers.route(request);

            // Check for 'Created' Status Code
            assertEquals(201, result.status());

            ObjectNode resultJson = (ObjectNode) bodyForResult(result);

            // Ensure that all fields were correctly written to the database
            assertTrue("db record should have startLatitude", resultJson.hasNonNull("startLatitude"));
            assertTrue("db record should have startLongitude", resultJson.hasNonNull("startLongitude"));
            assertTrue("db record should have endLatitude", resultJson.hasNonNull("endLatitude"));
            assertTrue("db record should have endLongitude", resultJson.hasNonNull("endLongitude"));

            // Ensure that the correct values were written to the database
            assertEquals("db record should have correct value for startLatitude",
                    6.0, resultJson.findPath("startLatitude").asDouble(), .001);
            assertEquals("db record should have correct value for startLongitude",
                    7.0, resultJson.findPath("startLongitude").asDouble(), .001);
            assertEquals("db record should have correct value for  endLatitude",
                    8.0, resultJson.findPath("endLatitude").asDouble(), .001);
            assertEquals("db record should have correct value for endLongitude",
                    9.0, resultJson.findPath("endLongitude").asDouble(), .001);

        });
    }

    @Test
    public void validPostShouldCreateCommodityWithParam() {
        Helpers.running(fakeApp, () -> {
            ObjectNode body = jsonNodeFactory.objectNode();

            body.put("startLatitude", 1.0);
            body.put("startLongitude", 2.0);
            body.put("endLatitude", 3.0);
            body.put("endLongitude", 4.0);
            body.put("param", 5);

            RequestBuilder request = new RequestBuilder()
                    .bodyJson(body)
                    .header("Content-Type", "application/json")
                    .method(Helpers.POST)
                    .uri("/commodity");

            Result result = Helpers.route(request);
            assertEquals(201, result.status());

            ObjectNode resultJson = (ObjectNode) bodyForResult(result);

            // Ensure that all fields were correctly written to the database
            assertTrue("db record should have startLatitude", resultJson.hasNonNull("startLatitude"));
            assertTrue("db record should have startLongitude", resultJson.hasNonNull("startLongitude"));
            assertTrue("db record should have endLatitude", resultJson.hasNonNull("endLatitude"));
            assertTrue("db record should have endLongitude", resultJson.hasNonNull("endLongitude"));
            assertTrue("db record should have param", resultJson.hasNonNull("param"));

            // Ensure that the correct values were written to the database
            assertEquals("db record should have correct value for startLatitude",
                    1.0, resultJson.findPath("startLatitude").asDouble(), .001);
            assertEquals("db record should have correct value for startLongitude",
                    2.0, resultJson.findPath("startLongitude").asDouble(), .001);
            assertEquals("db record should have correct value for  endLatitude",
                    3.0, resultJson.findPath("endLatitude").asDouble(), .001);
            assertEquals("db record should have correct value for endLongitude",
                    4.0, resultJson.findPath("endLongitude").asDouble(), .001);
            assertEquals("db record should have correct value for param",
                    5, resultJson.findPath("param").asInt());
        });
    }

    @Test
    public void getShouldReturnAllCommodities() {
        Helpers.running(fakeApp, () -> {
            populateCommodities();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/commodity");
            Result result = Helpers.route(request);

            assertEquals("Get all commodities should return status 200", 200, result.status());

            ArrayNode resultJson = (ArrayNode) bodyForResult(result);
            List<Commodity> commodities = new LinkedList<>();

            for (int i = 0; i < resultJson.size(); i++) {
                ObjectNode commodityNode = (ObjectNode) resultJson.get(i);
                commodities.add(Commodity.format().reads(play.api.libs.json.Json.parse(commodityNode.toString())).get());
            }

            assertEquals("Should have returned two commodities", 2, commodities.size());
            assertEquals("Should have correct values in first commodity", 3.0, commodities.get(0).endLatitude(), .001);
        });
    }

    private void populateCommodities() {
        Commodity commodity1 = Commodity.apply(1, 1.0, 2.0, 3.0, 4.0, 5);
        Commodity commodity2 = Commodity.apply(2, 10.0, 20.0, 30.0, 40.0, 50);
        commodity1.save();
        commodity2.save();
    }

    @Test
    public void getByExistingIDShouldReturnCommodity() {
        Helpers.running(fakeApp, () -> {
            populateCommodities();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/commodity/1");
            Result result = Helpers.route(request);

            assertEquals("Get commodity by id should return status 200", 200, result.status());

            ObjectNode resultJson = (ObjectNode) bodyForResult(result);
            Commodity commodity = Commodity.format().reads(play.api.libs.json.Json.parse(resultJson.toString())).get();

            assertNotNull("Get by id should return a result", commodity);
            assertEquals("Get by id should return correct commodity", 1.0, commodity.startLatitude(), .001);
        });
    }

    @Test
    public void getByInvalidIdShouldReturnNotFound() {
        Helpers.running(fakeApp, () -> {
            populateCommodities();

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/commodity/100");
            Result result = Helpers.route(request);

            assertEquals("Get by invalid should return status 404", 404, result.status());
        });
    }

    @Test
    public void putShouldUpdateCommodity() {
        Helpers.running(fakeApp, () -> {
            populateCommodities();

            ObjectNode body = JsonNodeFactory.instance.objectNode();
            body.put("startLatitude", 500.0);

            RequestBuilder request = new RequestBuilder()
                    .method(Helpers.PUT)
                    .uri("/commodity/1")
                    .bodyJson(body);
            Result result = Helpers.route(request);

            assertEquals("Successful Put should return no content code", 204, result.status());

            Commodity commodity = Commodity.finder().byId(1L);
            assertEquals("Commodity PUT changes should persist in the db", 500.0, commodity.startLatitude(), .001);

            RequestBuilder invalidIdRequest = new RequestBuilder()
                    .method(Helpers.PUT)
                    .uri("/commodity/500")
                    .bodyJson(body);
            Result invalidIdResult = Helpers.route(invalidIdRequest);
            assertEquals("Put to invalid id should 404", 404, invalidIdResult.status());

            // Test Put with fake fields
            body.put("FAKE FIELD", 25);

            RequestBuilder fakeFieldRequest = new RequestBuilder()
                    .method(Helpers.PUT)
                    .uri("/commodity/1")
                    .bodyJson(body);
            Result fakeFieldResult = Helpers.route(fakeFieldRequest);
        });
    }

    @Test
    public void deleteShouldDeleteCommodity() {
        Helpers.running(fakeApp, () -> {
            populateCommodities();

            RequestBuilder deleteRequest = new RequestBuilder()
                    .method(Helpers.DELETE)
                    .uri("/commodity/1");
            Result deleteResult = Helpers.route(deleteRequest);

            assertEquals("valid DELETE commodity request should return status 200", 200, deleteResult.status());

            RequestBuilder getRequest = new RequestBuilder()
                    .method(Helpers.GET)
                    .uri("/commodity/1");
            Result getResult = Helpers.route(getRequest);

            assertEquals("GET req for deleted item should 404", 404, getResult.status());

            RequestBuilder deleteFakeRequest = new RequestBuilder()
                    .method(Helpers.DELETE)
                    .uri("/commodity/500");
            Result deleteFakeResult = Helpers.route(deleteFakeRequest);

            assertEquals("Deleting nonexistent commodity should return 404", 404, deleteFakeResult.status());
        });
    }

}
