import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import play.db.Database;
import play.db.Databases;
import play.mvc.Result;
import play.test.Helpers;
import play.mvc.Http.RequestBuilder;
import play.test.FakeApplication;

public class CommoditySpec {

  private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
  private Helpers helpers = new Helpers();
//  Database db = Databases.inMemory(
//      "default",
//      ImmutableMap.of(
//          "MODE", "PostgreSQL"
//      ),
//      ImmutableMap.of(
//          "logStatements", true
//      )
//  );
  private FakeApplication fakeApp;

  @Before
  public void setup() {
    fakeApp = Helpers.fakeApplication(Helpers.inMemoryDatabase("default"));
    helpers.start(fakeApp);
  }

  @After
  public void teardown() {
    helpers.stop(fakeApp);
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
      assertEquals(200, result.status());
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
