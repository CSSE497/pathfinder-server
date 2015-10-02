package io.pathfinder.controllers;

import io.pathfinder.models.Cluster;
import play.mvc.Http;
import play.mvc.Security.Authenticator;

public class ClusterAuthenticator extends Authenticator {
    private static final String AUTH_HEADER_FIELD = "Authentication";

    /**
     * Play usually uses login sessions for authentication. Instead of authenticating users, we are
     * authenticating clusters. Our clusters don't have String usernames, so to fit Play's
     * authentication model, we wrap the cluster id as a string.
     *
     * @return String clusterId
     */
    @Override
    public String getUsername(Http.Context ctx) {
        String token = extractAuthToken(ctx.request());
        if (token != null) {
            Cluster authenticatedCluster = Cluster.find.where().eq("token", token).findUnique();
            if (authenticatedCluster != null) {
                return String.valueOf(authenticatedCluster.id);
            }
        }
        return null;
    }

    /**
     * Checks the request headers for the correct field and confirms that there is only one
     * value for it. Null otherwise.
     */
    private String extractAuthToken(Http.Request req) {
        String[] authTokenHeaderValue = req.headers().get(AUTH_HEADER_FIELD);
        if (authTokenHeaderValue != null && authTokenHeaderValue.length == 1) {
            return authTokenHeaderValue[0];
        }
        return null;
    }
}
