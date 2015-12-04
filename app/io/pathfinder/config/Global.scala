package io.pathfinder.config

import akka.actor.ActorSystem
import io.pathfinder.routing.Router
import play.api.GlobalSettings
import play.api.Application
import play.Logger

/**
 * These hooks are called by Play Framework. We can use them to initialize expensive objects and
 * set up any communication channels.
 */
object Global extends GlobalSettings {

    val actorSystem = ActorSystem.create()

    override def onStart(app: Application) {
        Logger.info("Application has started.")
        Router
    }

    override def onStop(app: Application) {
        Logger.info("Application has stopped.")
    }
}
