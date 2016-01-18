package io.pathfinder;

import io.pathfinder.models.Cluster;
import io.pathfinder.models.Application;
import org.junit.After;
import org.junit.Before;
import play.test.FakeApplication;
import play.test.Helpers;

/**
 * Provides the boilerplate for setting up a FakeApplication. All vehicles and commodities will
 * need to be added to a cluster. One is created an inserted here for convenience.
 */
public class BaseAppTest {
    public FakeApplication app;
    public final Cluster cluster = new Cluster();
    public final Application PATHFINDER_APPLICATION = new Application();
    public static final String APPLICATION_ID = "001e7047-ee14-40d6-898a-5acf3a1cfd8a";
    public static final String CLUSTER_ID = APPLICATION_ID;

    public Cluster baseCluster() {
        return Cluster.finder().byId(CLUSTER_ID);
    }

    @Before
    public void startApp() {
        app = Helpers.fakeApplication(Helpers.inMemoryDatabase());
        Helpers.start(app);
        PATHFINDER_APPLICATION.id_$eq(APPLICATION_ID);
        PATHFINDER_APPLICATION.name_$eq("MY COOL APP");
        cluster.id_$eq(CLUSTER_ID);
        cluster.insert();
        cluster.id_$eq(CLUSTER_ID);
        PATHFINDER_APPLICATION.insert();
        cluster.save();
    }

    @After
    public void stopApp() {
        Helpers.stop(app);
    }
}
