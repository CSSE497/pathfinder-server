package io.pathfinder.controllers

import io.pathfinder.websockets.ModelTypes.ModelType
import play.api.mvc.{Controller,Action}
import play.api.libs.json.{Json,Reads,Writes}
import io.pathfinder.data.{CrudDao,Resource}

/**
 * A controller that implements CRUD methods to any controller that extends it
 */
abstract class CrudController[K,V](dao: CrudDao[K,V])
        (implicit val reads: Reads[V], implicit val writes: Writes[V], implicit val resources: Reads[_ <: Resource[V]]) extends Controller {

    def model: ModelType

    /**
     * returns all of this controller's models
     */
    def getAll = Action(Ok(Json.toJson(dao.readAll)))

    def get(id: K) = Action {
        dao.read(id).map {
            model => Ok(Json.toJson(model))
        } getOrElse NotFound("No "+ model+" with id "+id.toString+" in database")
    }

    /**
     * Adds a new row to the model's table, returns a 400 if the json is invalid or if
     * not all required fields are present, otherwise it returns the new object
     * with a 201 code.
     */
    def post = Action(parse.json){ request =>
        resources.reads(request.body).map {
            create =>
                dao.create(create).map {
                    model => Created(Json.toJson(model))
                } getOrElse BadRequest("Error creating "+model+" from resource: "+resources)
        } getOrElse BadRequest("Error reading "+model+": "+request.body)
    }

    /**
     * Updates the row with the specified id, returns 404 if the id does not exist in the table.
     * It returns 400 if the json is invalid. It returns 200 along with the newly updated model
     * if successful.
     */
    def put(id: K) = Action(parse.json){ request =>
        resources.reads(request.body).map {
            update =>
                dao.update(id,update).map{
                    model =>
                        Ok(Json.toJson(model))
                } getOrElse NotFound("No "+model+" with id: "+id)
        } getOrElse BadRequest("Error reading "+model+": "+request.body)
    }

    /**
     * Deletes the row with the specified id, returns 404 if the id does not exist prior to deleting.
     */
    def delete(id: K) = Action {
        dao.delete(id).map{
            model =>
                Ok(Json.toJson(model))
        } getOrElse NotFound("No "+model+" with id: "+id)
    }
}
