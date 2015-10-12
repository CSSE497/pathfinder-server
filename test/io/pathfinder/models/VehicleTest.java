package io.pathfinder.models;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import play.api.libs.json.JsResult;
import play.api.libs.json.Json;
import scala.Option;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class VehicleTest {
    private final String JSON_VEHICLE = "{\"id\":1,\"latitude\":0.123,\"longitude\":4.567,\"capacity\":7}";
    private final String JSON_PARTIAL_VEHICLE = "{\"latitude\":0.123,\"longitude\":4.567,\"capacity\":7}";

    @Test
    public void testVehicleDeserializesWithoutErrors() {
        JsResult result = Vehicle.format().reads(Json.parse(JSON_VEHICLE));
        assertTrue(result.isSuccess());
    }

    @Test
    public void testVehicleDeserializesCorrectly() {
        Vehicle actual = Vehicle.format().reads(Json.parse(JSON_VEHICLE)).get();
        assertEquals(1, actual.id());
        assertEquals(7, actual.capacity());
        assertEquals(0.123, actual.latitude(), 0.001);
        assertEquals(4.567, actual.longitude(), 0.001);
    }

    @Test
    public void testVehicleResourceDeserializesWithoutErrors() {
        JsResult result = Vehicle.resourceFormat().reads(Json.parse(JSON_PARTIAL_VEHICLE));
        assertTrue(result.isSuccess());
    }

    @Test
    public void testVehicleResourceDeserializesCorrectly() {
        Option<Vehicle> result =
            Vehicle.resourceFormat().reads(Json.parse(JSON_PARTIAL_VEHICLE)).get().create();
        assertTrue(result.nonEmpty());
        Vehicle vehicle = result.get();
        assertEquals(7, vehicle.capacity());
        assertEquals(0.123, vehicle.latitude(), 0.01);
        assertEquals(4.567, vehicle.longitude(), 0.01);
    }
}
