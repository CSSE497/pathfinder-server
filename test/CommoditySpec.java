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
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.pathfinder.models.Commodity;

import java.util.LinkedList;
import java.util.List;

public class CommoditySpec {
  private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
  private FakeApplication fakeApp;

  private JsonNode bodyForResult(Result r) {
    String resultBody = null;

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
    Commodity commodity = new Commodity();

    commodity.endLatitude = 1.0;
    commodity.endLongitude = 1.0;
    commodity.startLatitude = 1.0;
    commodity.startLongitude = 1.0;
    commodity.param = 1;

    commodity.save();

    assertEquals(1, Commodity.find.all().size());
  }

  @Test
  public void validPostShouldCreateCommodity() {
    Helpers.running(fakeApp, () -> {
      ObjectNode body = jsonNodeFactory.objectNode();

      body.put("startLatitude", 6.0);
      body.put("startLongitude", 7.0);
      body.put("endLatitude", 8.0);
      body.put("endLongitude", 9.0);

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
  public void malformedPostShouldReturnBadRequest() {
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

      // Test type mismatch
      body.replace("startLatitude", jsonNodeFactory.booleanNode(false));
      Result result = Helpers.route(request);
      assertEquals(400, result.status());
      body.replace("startLatitude:", jsonNodeFactory.numberNode(1.0));

      body.replace("endLatitude", jsonNodeFactory.textNode("qwerty"));
      result = Helpers.route(request);
      assertEquals(400, result.status());
      body.replace("endLatitude", jsonNodeFactory.numberNode(3.0));

      // Test missing
      body.remove("startLongitude");
      result = Helpers.route(request);
      assertEquals(400, result.status());
      body.put("startLongitude", 2.0);

      body.remove("startLatitude");
      body.remove("param");
      result = Helpers.route(request);
      assertEquals(400, result.status());
      body.put("startLatitude", 1.0);
      body.put("param", 5);

      // Test Extremes
      body.replace("startLatitude", jsonNodeFactory.numberNode(Double.MAX_VALUE));
      result = Helpers.route(request);
      assertEquals(400, result.status());
      body.replace("startLatitude", jsonNodeFactory.numberNode(1.0));

      // Test incorrect content type
      RequestBuilder textRequest = new RequestBuilder()
          .bodyJson(body)
          .header("Content-Type", "text/plain")
          .method(Helpers.POST)
          .uri("/commodity");

      result = Helpers.route(textRequest);
      assertEquals(415, result.status());
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
      List<Commodity> commodities = new LinkedList<Commodity>();

      for (int i = 0; i < resultJson.size(); i++) {
        ObjectNode commodityNode = (ObjectNode) resultJson.get(0);
        commodities.add(Json.fromJson(commodityNode, Commodity.class));
      }

      assertEquals("Should have returned two commodities", 2, commodities.size());
      assertEquals("Should have correct values in first commodity", 3.0, commodities.get(0).endLatitude, .001);
    });
  }

  private void populateCommodities() {
    Commodity commodity1 = new Commodity();
    Commodity commodity2 = new Commodity();

    commodity1.startLatitude = 1.0;
    commodity1.startLongitude = 2.0;
    commodity1.endLatitude = 3.0;
    commodity1.endLongitude = 4.0;
    commodity1.param = 5;

    commodity2.startLatitude = 10.0;
    commodity2.startLongitude = 20.0;
    commodity2.endLatitude = 30.0;
    commodity2.endLongitude = 40.0;
    commodity2.param = 50;

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
      Commodity commodity = Json.fromJson(resultJson, Commodity.class);

      assertNotNull("Get by id should return a result", commodity);
      assertEquals("Get by id should return correct commodity", 1.0, commodity.startLatitude, .001);
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

  }

  @Test
  public void deleteShouldDeleteCommodity() {

  }

}
