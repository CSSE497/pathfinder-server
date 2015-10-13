package io.pathfinder.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pathfinder.models.Cluster;
import play.Logger;
import play.api.libs.json.JsValue;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

public class ClusterController extends Controller {
    private JsonContext jsonContext = Ebean.createJsonContext();

    public Result getClusters() {
        List<String> jsObjs = Cluster.finder().all().stream().map(x -> Cluster.format().writes(x).toString()).collect(Collectors.toList());
        return ok("[" + String.join(",", jsObjs) + "]");
    }

    public Result getCluster(long id) {
        Cluster cluster = Cluster.finder().byId(id);

        if (cluster == null) {
            return notFound();
        }
        return ok(Cluster.format().writes(cluster).toString());
    }

    public Result createCluster() {
        Logger.info(String.format("Create request: %s", request().body().toString()));
        JsValue jsVal = play.api.libs.json.Json.parse(request().body().asJson().toString());
        if (jsVal == null) {
            return badRequest("Expected content-type text/json or application/json");
        }

        Cluster cluster;

        try {
            cluster = Cluster.resourceFormat().reads(jsVal).get().create().get();
            cluster.save();
            return created(Cluster.format().writes(cluster).toString());
        } catch (PersistenceException e) {
            e.printStackTrace();
            Logger.error(String.format("Error saving cluster to the database: %s", e));
            return internalServerError("Error saving cluster to the database: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            Logger.error("Unable to map json to cluster object: " + jsVal.toString(), e);
            return badRequest("Unable to map json to cluster object: " + jsVal.toString());
        }
    }

    public Result deleteCluster(long id) {
        Cluster cluster = Cluster.finder().byId(id);

        if (cluster == null) {
            return notFound();
        }
        String deletedCluster = Cluster.format().writes(cluster).toString();
        cluster.delete();
        return ok(deletedCluster);
    }
}
