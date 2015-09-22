package io.pathfinder.controllers

import play.api.mvc.{Controller,Action}

class ViewController extends Controller {
    def index = Action {
        Ok("Pathfinder Webservice")
    }
}
