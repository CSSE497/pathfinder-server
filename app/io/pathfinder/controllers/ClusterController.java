package io.pathfinder.controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.Controller;
import io.pathfinder.models.Cluster;
import play.mvc.Result;
import java.util.Iterator;

import javax.persistence.PersistenceException;

public class ClusterController extends Controller {
    private JsonContext jsonContext = Ebean.createJsonContext();

    public Result getClusters() {
        return ok(jsonContext.toJson(Cluster.find.all()));
    }

    public Result getCluster(long id) {
        Cluster cluster = Cluster.find.byId(id);

        if (cluster == null) {
            return notFound();
        }

        return ok(jsonContext.toJson(cluster));
    }

    public Result createCluster() {
        JsonNode json = request().body().asJson();
        if (json == null) {
            return badRequest("Expected content-type text/json or application/json");
        }

        Cluster cluster;

        try {
            cluster = Json.fromJson(json, Cluster.class);

            cluster.save();
            return created(jsonContext.toJson(cluster));
        } catch (PersistenceException e) {
            return internalServerError("Error saving cluster to the database: " + e.getMessage());
        } catch (RuntimeException e) {
            return badRequest("Unable to map json to commodity object: " + e.getMessage());
        }
    }

    public Result editCluster(long id) {
        Cluster cluster = Cluster.find.byId(id);

        if (cluster == null) {
            return notFound();
        }

        ObjectNode clusterJson = (ObjectNode) Json.toJson(cluster);
        ObjectNode body;

        try {
            JsonNode json = request().body().asJson();
            body = (ObjectNode) json;
        } catch (ClassCastException e) {
            return badRequest("Cannot cast request body to ObjectNode: " + e.getMessage());
        }

        Iterator<String> fields = clusterJson.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();

            if (field.equals("id")) {
                continue;
            }

            if (body.has(field)) {
                clusterJson.replace(field, body.findPath(field));
            }
        }

        try {
            cluster = Json.fromJson(clusterJson, Cluster.class);

            cluster.update();

            return noContent();
        } catch (RuntimeException e) {
            return badRequest("Unable to update cluster: " + e.getMessage());
        }
    }

    public Result deleteCluster(long id) {
        Cluster cluster = Cluster.find.byId(id);

        if (cluster == null) {
            return notFound();
        }

        cluster.delete();
        return ok(jsonContext.toJson(cluster));
    }
}
