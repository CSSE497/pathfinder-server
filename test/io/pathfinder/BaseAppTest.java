package io.pathfinder;

import io.pathfinder.models.Cluster;
import io.pathfinder.models.PathFinderApplication;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.test.FakeApplication;
import play.test.Helpers;

import java.util.UUID;

/**
 * Provides the boilerplate for setting up a FakeApplication. All vehicles and commodities will
 * need to be added to a cluster. One is created an inserted here for convenience.
 */
public class BaseAppTest {
    public static FakeApplication app;
    public static final Cluster cluster = new Cluster();
    public static final PathFinderApplication pathfinderApplication = new PathFinderApplication();
    public static final UUID APPLICATION_ID = UUID.fromString("001e7047-ee14-40d6-898a-5acf3a1cfd8a");

    @Before
    public void startApp() {
        app = Helpers.fakeApplication(Helpers.inMemoryDatabase());
        Helpers.start(app);
        pathfinderApplication.id_$eq(APPLICATION_ID);
        pathfinderApplication.name_$eq("MY COOL APP");
        cluster.id_$eq(1);
        cluster.insert();
        pathfinderApplication.cluster_$eq(cluster);
        pathfinderApplication.insert();
        cluster.save();
    }

    @After
    public void stopApp() {
        Helpers.stop(app);
    }
}
