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
            Logger.error(String.format("Error saving cluster to the database: %s", e.getMessage()));
            return internalServerError("Error saving cluster to the database: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            Logger.error("Unable to map json to cluster object: " + jsVal.toString());
            return badRequest("Unable to map json to cluster object: " + jsVal.toString());
        }
    }

    public Result editCluster(long id) {
        Cluster cluster = Cluster.finder().byId(id);

        if (cluster == null) {
            return notFound();
        }

        ObjectNode clusterJson = (ObjectNode) Json.toJson(cluster);
        ObjectNode body;

        try {
            JsonNode json = request().body().asJson();
            body = (ObjectNode) json;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return badRequest("Cannot cast request body to ObjectNode: " + e.getMessage());
        }
        Cluster.resourceFormat().reads(play.api.libs.json.Json.parse(body.toString())).get().update(cluster);
        cluster.save();
        return noContent();
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
