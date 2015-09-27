package io.pathfinder.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.mvc.Controller;
import io.pathfinder.models.Commodity;
import play.mvc.Result;

import java.util.Iterator;
import play.libs.Json;

import javax.persistence.PersistenceException;

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
      commodity = Json.fromJson(json, Commodity.class);
      commodity.save();

      return created(jsonContext.toJson(commodity));
    } catch (PersistenceException e) {
      return internalServerError("Error saving commodity to the database: " + e.getMessage());
    } catch (Exception e) {
      return badRequest("Unable to map json to commodity object: " + e.getMessage());
    }
  }

  public Result editCommodity(long id) {
    Commodity commodity = Commodity.find.byId(id);

    if (commodity == null) {
      return notFound();
    }

    ObjectNode commodityJson = (ObjectNode) Json.toJson(commodity);
    ObjectNode body;

    try {
      body = (ObjectNode) request().body().asJson();
    } catch (ClassCastException e) {
      return badRequest("Cannot cast request body to ObjectNode: " + e.getMessage());
    }

    Iterator<String> fields = commodityJson.fieldNames();
    while (fields.hasNext()) {
      String field = fields.next();

      if (field.equals("id")) {
        continue;
      }

      if (body.has(field)) {
        commodityJson.replace(field, body.findPath(field));
      }
    }

    try {
      commodity = Json.fromJson(commodityJson, Commodity.class);
      commodity.update();

      return noContent();
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
