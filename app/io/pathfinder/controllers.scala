package io.pathfinder

import scala.util.{Try,Success,Failure}

import play.api.mvc.Controller
import play.api.mvc._
import play.api.libs.json.Json

import com.avaje.ebean.{Model,Ebean}

import play.db.ebean.Transactional

import models._

package controllers {

    class Application extends Controller {
        def index = Action {
            Ok("Pathfinder Webservice")
        }
    }

//    abstract class CrudController[K,V <: Model](ccomp: CrudCompanion[K,V]) extends Controller {
//
//        @Transactional
//        def get(id: K) = Action{    // TODO: proper error handling
//            val model = ccomp.finder.byId(id)
//            val json  = Json.toJson(model)(ccomp.writes)
//            Ok(json.toString)
//        }
//    }
//
//    class VehicleController extends CrudController[Long,Vehicle](models.Vehicle){
//        val v = new Vehicle(1l,0.0,0.0,5)
//        Ebean.save(v)
//    }
}