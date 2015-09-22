import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class CommoditySpec {

  private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
  private FakeApplication fakeApp;

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

      String resultBody = null;
      ObjectNode resultJson = null;

      try {
        resultBody = new String(JavaResultExtractor.getBody(result, 0L), "UTF-8");

        ObjectMapper mapper = new ObjectMapper();
        resultJson = (ObjectNode) mapper.readTree(resultBody);
      } catch(Exception e) {
        fail("Could not process database record");
      }


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

        String resultBody = null;
        ObjectNode resultJson = null;

        try {
          resultBody = new String(JavaResultExtractor.getBody(result, 0L), "UTF-8");

          ObjectMapper mapper = new ObjectMapper();
          resultJson = (ObjectNode) mapper.readTree(resultBody);
        } catch(Exception e) {
          fail("Could not process database record");
        }

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

  }

  @Test
  public void getShouldReturnAllCommodities() {

  }

  @Test
  public void getByExistingIDShouldReturnCommodity() {

  }

  @Test
  public void getByInvalidIdShouldReturnNotFound() {

  }

  @Test
  public void putShouldUpdateCommodity() {

  }

  @Test
  public void deleteShouldDeleteCommodity() {

  }

}
