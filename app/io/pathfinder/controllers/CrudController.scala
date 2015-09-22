package io.pathfinder.controllers

import play.api.mvc.{Controller,Action}
import play.api.libs.json.Json
import play.db.ebean.Transactional
import scala.collection.JavaConversions.asScalaBuffer
import com.avaje.ebean.{Model,Ebean}
import io.pathfinder.models.CrudCompanion

/**
 * A controller that implements CRUD methods to any controller that extends it, requires
 * an Ebean model's CrudCompanion
 */
abstract class CrudController[K,V <: Model](comp: CrudCompanion[K,V]) extends Controller {

    import comp.{format,updateReads,finder}

    /**
     * returns all of this controller's models
     */
    def getAll = Action(Ok(Json.toJson(finder.all.toList)))

    def get(id: K) = Action{
        Option(finder.byId(id)).map{
            vehicle => Ok(Json.toJson(vehicle))
        } getOrElse (
            NotFound
        )
    }

    /**
     * Adds a new row to the model's table, returns a 400 if the json is invalid or if
     * not all required fields are present, otherwise it returns the new object
     * with a 201 code.
     */
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

    /**
     * Updates the row with the specified id, returns 404 if the id does not exist in the table.
     * It returns 400 if the json is invalid. It returns 200 along with the newly updated model
     * if successful.
     */
    @Transactional
    def put(id: K) = Action(parse.json){ request =>
        updateReads.reads(request.body).map {
            update =>
                Option(finder.byId(id)).map{
                    model =>
                        update.apply(model)
                        model.save()
                        Ok(Json.toJson(model))
                } getOrElse(NotFound)
        } getOrElse (BadRequest)
    }

    /**
     * Deletes the row with the specified id, returns 404 if the id does not exist prior to deleting.
     */
    @Transactional
    def delete(id: K) = Action {
        Option(finder.byId(id)).map{
            model =>
                model.delete
                Ok(Json.toJson(model))
        } getOrElse (NotFound)
    }
}
