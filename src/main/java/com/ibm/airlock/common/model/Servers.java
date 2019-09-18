package com.ibm.airlock.common.model;

import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Denis Voloshin
 */

public class Servers {

    private static final String TAG = "Airlock.Servers";
    private static final Object lock = new Object();
    private final PersistenceHandler sp;
    private Server currentServer;
    private final Server defaultServer;
    private final Map<String, Server> serverList = new HashMap<>();


    public Servers(PersistenceHandler sp) {
        this.sp = sp;
        defaultServer = new Server(sp.read(Constants.SP_DEFAULT_SERVER_NAME, "Default"), sp.read(Constants.SP_DIRECT_S3PATH, ""), sp.read(Constants.SP_CACHED_S3PATH, ""));
        String currentServerJson = sp.read(Constants.SP_CURRENT_SERVER, "");
        // use the default server
        if (currentServerJson.isEmpty()) {
            currentServer = defaultServer;
        } else {
            try {
                currentServer = new Server(new JSONObject(currentServerJson));
            } catch (JSONException e) {
                Logger.log.e(TAG, "Failed to parse server from SharedPreference.");
                currentServer = defaultServer;
            }
        }
    }

    public Server getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(Server currentServer) {
        if (currentServer.getDisplayName().isEmpty()) {
            Logger.log.d(TAG, "Can't set null as default server. No server matches the given empty name ");
        }
        this.currentServer = currentServer;
        sp.write(Constants.SP_CURRENT_SERVER, currentServer.toJson());
    }

    public Server getDefaultServer() {

        return defaultServer;
    }

    public void updateServersList(JSONObject jsonStr) throws JSONException {
        JSONArray servers = jsonStr.optJSONArray(Constants.JSON_SERVERS_ARRAY_FIELD_NAME);
        for (int i = 0; i < servers.length(); ++i) {
            JSONObject currentServer = servers.getJSONObject(i);
            String displayName = currentServer.optString(Constants.JSON_SERVER_NAME_FIELD_NAME);
            serverList.put(displayName, new Server(currentServer));
        }
        String defaultServerName = jsonStr.optString(Constants.JSON_DEFAULT_SERVER_FIELD_NAME);
        defaultServer.displayName = defaultServerName;
        sp.write(Constants.SP_DEFAULT_SERVER_NAME, defaultServerName);
    }

    public Map<String, Server> getList() {
        return serverList;
    }

    /*
    sets the object to null
    Note : need to call this on tests , since the existing SeverList object could hold url to wrong server from previous tests
     */
    public void nullifyServerList() {
        serverList.clear();
    }

    //********* Inner class ***************//
    public static class Server {

        private String displayName;
        private final String url; // for dev
        private final String cdnOverride; // for direct mode.
        private Collection<Product> products; //product with relevant seasonId for my version.

        Server(JSONObject server) {
            this.displayName = server.optString(Constants.JSON_SERVER_NAME_FIELD_NAME);
            this.url = server.optString(Constants.JSON_SERVER_URL_FIELD_NAME);
            this.cdnOverride = server.optString(Constants.JSON_SERVER_CDN_URL_FIELD_NAME);
        }

        Server(String displayName, String url, String cdnOverride) {
            this.displayName = displayName;
            this.url = url;
            this.cdnOverride = cdnOverride;
        }

        String getDisplayName() {
            return displayName;
        }

        public String getCdnOverride() {
            return cdnOverride;
        }

        public String getUrl() {
            return url;
        }

        @CheckForNull
        public Collection<Product> getProducts() {
            return products;
        }

        public void setProducts(Collection<Product> products) {
            this.products = products;
        }

        String toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put(Constants.JSON_SERVER_NAME_FIELD_NAME, displayName);
                obj.put(Constants.JSON_SERVER_CDN_URL_FIELD_NAME, cdnOverride);
                obj.put(Constants.JSON_SERVER_URL_FIELD_NAME, url);
            } catch (JSONException e) {
                e.printStackTrace();
                return new JSONObject().toString();
            }
            return obj.toString();
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    //********* Inner class ***************//
    public static class Product {
        final String name;
        final String seasonId;
        final String productId;
        final Server server;

        public Product(String name, String productId, String seasonId, Server server) {
            this.name = name;
            this.productId = productId;
            this.seasonId = seasonId;
            this.server = server;
        }

        public String getSeasonId() {
            return seasonId;
        }

        public String getProductId() {
            return productId;
        }

        public String getName() {
            return name;
        }

        public Server getServer() {
            return server;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
