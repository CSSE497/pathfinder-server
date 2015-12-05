package io.pathfinder.models;

import io.pathfinder.BaseAppTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import play.api.libs.json.JsNumber;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsResult;
import play.api.libs.json.Json;
import scala.Option;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class VehicleTest extends BaseAppTest {
    private final String JSON_VEHICLE = "{\"id\":1,\"latitude\":0.123,\"longitude\":4.567,\"metadata\":{\"capacity\":7},\"status\":\"Online\"}";
    private final String JSON_PARTIAL_VEHICLE = "{\"latitude\":0.123,\"longitude\":4.567,\"metadata\":{\"capacity\":8}}";

    @Test
    public void testVehicleDeserializesWithoutErrors() {
        JsResult result = Vehicle.format().reads(Json.parse(JSON_VEHICLE));
        assertTrue(result.isSuccess());
    }

    @Test
    public void testVehicleDeserializesCorrectly() {
        Vehicle actual = Vehicle.format().reads(Json.parse(JSON_VEHICLE)).get();
        assertEquals(1, actual.id());
        assertEquals(7, ((JsNumber) (actual.metadata()).value().get("capacity").get()).value().toInt());
        assertEquals(0.123, actual.latitude(), 0.001);
        assertEquals(4.567, actual.longitude(), 0.001);
        assertEquals(VehicleStatus.Online, actual.status());
    }

    @Test
    public void testVehicleResourceDeserializesWithoutErrors() {
        JsResult result = Vehicle.resourceFormat().reads(Json.parse(JSON_PARTIAL_VEHICLE));
        assertTrue(result.isSuccess());
    }

    @Test
    public void testVehicleResourceDeserializesCorrectly() {
        Option<Vehicle> result =
            Vehicle.resourceFormat().reads(Json.parse(JSON_PARTIAL_VEHICLE)).get().create(cluster);
        assertTrue(result.nonEmpty());
        Vehicle vehicle = result.get();
        assertEquals(8, ((JsNumber) vehicle.metadata().value().get("capacity").get()).value().toInt());
        assertEquals(0.123, vehicle.latitude(), 0.01);
        assertEquals(4.567, vehicle.longitude(), 0.01);
        assertEquals(VehicleStatus.Offline, vehicle.status());
    }

    @Test
    public void testGeneratedId() {
        Vehicle actual = Vehicle.resourceFormat().reads(Json.parse(JSON_PARTIAL_VEHICLE)).get().create(cluster).get();
        actual.insert();
        assertEquals(1, actual.id());
    }
}
