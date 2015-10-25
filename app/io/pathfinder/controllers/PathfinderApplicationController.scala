package io.pathfinder.controllers

import java.util.UUID

import io.pathfinder.models.PathfinderApplication
import play.api.mvc.{Action, Controller}

class PathfinderApplicationController extends Controller {

    def get(id: UUID) = Action {
        Option(PathfinderApplication.finder.byId(id)).map {
            app => Ok(PathfinderApplication.format.writes(app))
        }.getOrElse(NotFound("No Application with id: "+id))
    }
}
