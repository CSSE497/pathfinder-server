package io.pathfinder.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.*;
import play.api.db.*;

import io.pathfinder.models.Commodity;

import javax.sql.DataSource;

import play.libs.Json;
import io.pathfinder.models.Commodity;

/**
 * Created by Carter on 9/17/2015.
 */
public class CommodityController extends Controller {

  public Result getCommodities() {
    return ok("You called the getCommodities() action!");
  }

  public Result getCommodity() {
    return null;
  }

  public Result createCommodity() {
    JsonNode json = request().body().asJson();
    if (json == null) {
      return badRequest("Expecting content-type text/json or application/json");
    }

    Commodity commodity = new Commodity();

    commodity.startLatitude = json.findPath("startLatitude").doubleValue();
    commodity.startLongitude = json.findPath("startLongitude").doubleValue();
    commodity.endLatitude = json.findPath("endLatitude").doubleValue();
    commodity.endLongitude = json.findPath("endLongitude").doubleValue();
    commodity.param = json.findPath("param").intValue();

    commodity.save();

    return null;
  }

  public Result editCommodity() {
    return null;
  }

  public Result deleteCommodity() {
    return null;
  }

}
