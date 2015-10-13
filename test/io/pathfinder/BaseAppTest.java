package io.pathfinder;

import io.pathfinder.models.Cluster;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.test.FakeApplication;
import play.test.Helpers;

/**
 * Provides the boilerplate for setting up a FakeApplication. All vehicles and commodities will
 * need to be added to a cluster. One is created an inserted here for convenience.
 */
public class BaseAppTest {
    public static FakeApplication app;
    public static final Cluster cluster = new Cluster();

    @Before
    public void startApp() {
        app = Helpers.fakeApplication(Helpers.inMemoryDatabase());
        Helpers.start(app);
        cluster.id_$eq(1);
        cluster.insert();
    }

    @After
    public void stopApp() {
        Helpers.stop(app);
    }
}
