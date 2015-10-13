package io.pathfinder.config;

import io.pathfinder.routing.Router;
import play.Application;
import play.GlobalSettings;
import play.Logger;

/**
 * These hooks are called by Play Framework. We can use them to initialize expensive objects and
 * set up any communication channels.
 */
public class Global extends GlobalSettings {
    public void onStart(Application app) {
        Logger.info("Application has started.");
        Router.init();
    }

    public void onStop(Application app) {
        Logger.info("Application has stopped.");
    }
}
