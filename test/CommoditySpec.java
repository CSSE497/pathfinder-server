import play.mvc.Result;
import play.test.Helpers;
import play.mvc.Http.RequestBuilder;
import play.test.FakeApplication;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.pathfinder.models.Commodity;

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
