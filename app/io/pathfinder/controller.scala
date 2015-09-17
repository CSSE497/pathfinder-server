package io.pathfinder

import scala.util.{Try,Success,Failure}

import play.api.mvc.Controller
import play.api.mvc._
import play.api.libs.json.Json

import play.db.ebean.Transactional

import models._

package controller {
    
    

    class Application extends Controller {
        def index = Action {
            Ok("Pathfinder Webservice")
        }
    }

    abstract class CrudController[K,V](comp: CrudCompanion[K,V]) extends Controller {

        import comp._

        @Transactional
        def get(id: K) = Action{
            val model = comp.finder.byId(id)
            val json  = Json.toJson(model)(comp.writes)
            Ok(json.toString)
        }
    }

    class VehicleController extends CrudController(Vehicle){

    }
}