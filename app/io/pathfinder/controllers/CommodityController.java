package io.pathfinder.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.mvc.*;
import io.pathfinder.models.Commodity;

import java.util.Iterator;

import static play.libs.Json.*;

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

    Commodity commodity;

    try {
      commodity = fromJson(json, Commodity.class);
    } catch (Exception e) {
      return badRequest("Unable to map json to commodity object: " + e.getMessage());
    }

    try {
      commodity.save();

      return created(jsonContext.toJson(commodity));
    } catch (Exception e) {
      return internalServerError("Error saving commodity to the database: " + e.getMessage());
    }
  }

  public Result editCommodity(long id) {
    Commodity commodity = Commodity.find.byId(id);

    if (commodity == null) {
      return notFound();
    }

    ObjectNode commodityJson = (ObjectNode) toJson(commodity);
    ObjectNode body;

    try {
      body = (ObjectNode) request().body().asJson();
    } catch (ClassCastException e) {
      return badRequest("Cannot cast request body to ObjectNode: " + e.getMessage());
    }

    Iterator<String> fields = commodityJson.fieldNames();
    while (fields.hasNext()) {
      String field = fields.next();

      if (field.equals("id"))
        continue;

      if (body.has(field)) {
        commodityJson.replace(field, body.findPath(field));
      }
    }

    try {
      commodity = fromJson(commodityJson, Commodity.class);
      commodity.update();

      return ok();
    } catch (Exception e) {
      return badRequest("Unable to update commodity: " + e.getMessage());
    }
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
