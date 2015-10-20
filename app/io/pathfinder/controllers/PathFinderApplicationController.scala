package io.pathfinder.controllers

import io.pathfinder.models.PathFinderApplication
import play.api.mvc.{Action, Controller}

class PathFinderApplicationController extends Controller {

    def get(id: String) = Action {
        Option(PathFinderApplication.finder.byId(id)).map {
            app => Ok(PathFinderApplication.format.writes(app))
        }.getOrElse(NotFound("No Application with id: "+id))
    }
}
