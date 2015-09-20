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

    abstract class CrudController[K,V <: Model](comp: CrudCompanion[K,V]) extends Controller {

        import comp._

        def get(id: K) = Action{    // TODO: proper error handling
            Option(comp.finder.byId(id)).map{
                vehicle => Ok(Json.toJson(vehicle))
            } getOrElse (
                NotFound
            )
        }

        def post = Action(parse.json){ request =>
            updateReads.reads(request.body).map {
                update =>
                    val model = comp.create
                    if(!update(model)){
                        BadRequest
                    } else {
                        model.insert()
                        Created(Json.toJson(model))
                    }
            } getOrElse {
                BadRequest
            }
        }

        @Transactional
        def put(id: K) = Action(parse.json){ request =>
            updateReads.reads(request.body).map {
                update =>
                    Option(comp.finder.byId(id)).map{
                        model =>
                            update.apply(model)
                            model.save()
                            Ok(Json.toJson(model))
                    } getOrElse(NotFound)
            } getOrElse (BadRequest)
        }

        @Transactional
        def delete(id: K) = Action {
            Option(comp.finder.byId(id)).map{
                model =>
                    model.delete
                    Ok
            } getOrElse (NotFound)
        }
    }

    class VehicleController extends CrudController[Long,Vehicle](models.Vehicle)
}