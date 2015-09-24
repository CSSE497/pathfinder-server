package io.pathfinder.controllers

import play.api.mvc.{Controller,Action}
import play.api.libs.json.Json
import com.avaje.ebean.{Model,Ebean}
import io.pathfinder.models.CrudCompanion
import io.pathfinder.data.{CrudDao,Update}

/**
 * A controller that implements CRUD methods to any controller that extends it
 */
abstract class CrudController[K,V](comp: CrudCompanion[K,V], dao: CrudDao[K,V]) extends Controller {

    import comp.{format,updateReads}

    /**
     * returns all of this controller's models
     */
    def getAll = Action(Ok(Json.toJson(dao.readAll)))

    def get(id: K) = Action{
        dao.read(id).map{
            model => Ok(Json.toJson(model))
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
                if(update(model)){
                    dao.create(model)
                    Created(Json.toJson(model))
                } else BadRequest
        } getOrElse {
            BadRequest
        }
    }

    /**
     * Updates the row with the specified id, returns 404 if the id does not exist in the table.
     * It returns 400 if the json is invalid. It returns 200 along with the newly updated model
     * if successful.
     */
    def put(id: K) = Action(parse.json){ request =>
        updateReads.reads(request.body).map {
            update =>
                dao.update(id,update).map{
                    model =>
                        Ok(Json.toJson(model))
                } getOrElse(NotFound)
        } getOrElse (BadRequest)
    }

    /**
     * Deletes the row with the specified id, returns 404 if the id does not exist prior to deleting.
     */
    def delete(id: K) = Action {
        dao.delete(id).map{
            model =>
                Ok(Json.toJson(model))
        } getOrElse (NotFound)
    }
}
