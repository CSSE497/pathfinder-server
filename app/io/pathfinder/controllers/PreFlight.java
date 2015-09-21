package io.pathfinder.controllers;

import play.mvc.*;

/**
 * Created by Carter on 9/20/2015.
 */
public class PreFlight extends Controller {

  public Result preflight(String all)  {
    response().setHeader("Access-Control-Allow-Origin", "*");
    response().setHeader("Allow", "*");
    response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
    response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent");
    return ok();
  }

}
