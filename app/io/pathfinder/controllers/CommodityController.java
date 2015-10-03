package io.pathfinder.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.mvc.Controller;
import io.pathfinder.models.Commodity;
import play.mvc.Result;

import java.util.Iterator;
import play.api.libs.json.Json;

import javax.persistence.PersistenceException;

public class CommodityController extends Controller {

  private JsonContext jsonContext = Ebean.createJsonContext();

  public Result getCommodities() {
    return ok(jsonContext.toJson(Commodity.finder().all()));
  }

  public Result getCommodity(long id) {
    Commodity commodity = Commodity.finder().byId(id);

    if (commodity == null) {
      return notFound();
    }

    return ok(Commodity.format().writes(commodity).toString());
  }

  public Result createCommodity() {
    JsonNode json = request().body().asJson();
    if (json == null) {
      return badRequest("Expecting content-type text/json or application/json");
    }
    Logger.info(String.format("Create commodity request: %s", json.toString()));
    try {
      Commodity commodity = Commodity.resourceFormat().reads(Json.parse(json.toString())).get().create().get();
      commodity.save();
      return created(Commodity.format().writes(commodity).toString());
    } catch (PersistenceException e) {
      e.printStackTrace();
      return internalServerError("Error saving commodity to the database: " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      return badRequest("Unable to map json to commodity object: " + json.toString());
    }
  }

  public Result editCommodity(long id) {
    Commodity commodity = Commodity.finder().byId(id);
    if (commodity == null) return notFound();
    Commodity.resourceFormat().reads(Json.parse(request().body().asJson().toString())).get().update(commodity);
    commodity.save();
    return noContent();
  }

  public Result deleteCommodity(long id) {
    Commodity commodity = Commodity.finder().byId(id);

    if (commodity == null) {
      return notFound();
    }

    commodity.delete();
    return ok(jsonContext.toJson(commodity));
  }

}
