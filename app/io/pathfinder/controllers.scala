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
            Ok("<html><head><script src=\"https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js\"></script></head><body>Pathfinder Webservice</body></html>").as(HTML)
        }
    }

    abstract class CrudController[K,V <: Model](comp: CrudCompanion[K,V]) extends Controller {

        import comp._

        def get(id: K) = Action{    // TODO: proper error handling
            val vehicle = comp.finder.byId(id)
            Ok(Json.toJson(vehicle))
        }

        def post = Action(parse.json){ request =>
            val update = updateReads.reads(request.body).get
            val model = comp.create
            update.apply(model)
            model.insert()
            Created
        }

        @Transactional
        def put(id: K) = Action(parse.json){ request =>
            val update = updateReads.reads(request.body).get
            val model = comp.finder.byId(id)
            update.apply(model)
            model.save
            Ok
        }

        def delete(id: K) = Action {
            comp.finder.deleteById(id)
            Ok
        }
    }

    class VehicleController extends CrudController[Long,Vehicle](models.Vehicle)
}