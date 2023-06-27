package api;

import java.util.Map;

public class BazaarResponse {

    public boolean success;
    public long lastUpdated;

    public Map<String, BazaarItem> products;

    public BazaarResponse(boolean success, long lastUpdated, Map<String, BazaarItem> products) {
        this.success = success;
        this.lastUpdated = lastUpdated;
        this.products = products;
    }
}