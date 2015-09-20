package io.pathfinder.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.*;
import io.pathfinder.models.Commodity;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Carter on 9/17/2015.
 */
public class CommodityController extends Controller {

  private JsonContext jsonContext = Ebean.createJsonContext();

  public Result getCommodities() {
    return ok(jsonContext.toJson(Commodity.find.all()));
  }

  public Result getCommodity(long id) {
    Commodity commodity = Commodity.find.byId(id);

    if (commodity == null) {
      return notFound();
    }

    return ok(jsonContext.toJson(commodity));
  }

  public Result createCommodity() {
    JsonNode json = request().body().asJson();
    if (json == null) {
      return badRequest("Expecting content-type text/json or application/json");
    }

    Commodity commodity = new Commodity();

    commodity.startLatitude = json.findPath("startLatitude").asDouble();
    commodity.startLongitude = json.findPath("startLongitude").asDouble();
    commodity.endLatitude = json.findPath("endLatitude").asDouble();
    commodity.endLongitude = json.findPath("endLongitude").asDouble();
    commodity.param = json.findPath("param").asInt();

    commodity.save();

    return created(jsonContext.toJson(commodity));
  }

  public Result editCommodity(long id) {
    Commodity commodity = Commodity.find.byId(id);

    if (commodity == null) {
      return notFound();
    }

    JsonNode json = request().body().asJson();

    if (json.hasNonNull("startLatitude")) {
      commodity.startLatitude = json.findPath("startLatitude").asDouble();
    }
    if (json.hasNonNull("startLongitude")) {
      commodity.startLongitude = json.findPath("startLongitude").asDouble();
    }
    if (json.hasNonNull("endLatitude")) {
      commodity.endLatitude = json.findPath("endLatitude").asDouble();
    }
    if (json.hasNonNull("endLongitude")) {
      commodity.endLongitude = json.findPath("endLongitude").asDouble();
    }
    if (json.hasNonNull("param")) {
      commodity.param = json.findPath("param").asInt();
    }

    commodity.update();

    return ok();
  }

  public Result deleteCommodity(long id) {
    Commodity commodity = Commodity.find.byId(id);

    if (commodity == null) {
      return notFound();
    }

    commodity.delete();
    return ok(jsonContext.toJson(commodity));
  }

}
