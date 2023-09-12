package api;

import java.util.ArrayList;
import java.util.Map;

public class AuctionResponse {
    public boolean success;
    public int page;
    public int totalPages;
    public int totalAuctions;
    public long lastUpdated;

    public ArrayList<AuctionItem> auctions;

    public AuctionResponse(boolean success, int page, int totalPages, int totalAuctions, long lastUpdated, ArrayList<AuctionItem> auctions) {
        this.success = success;
        this.page = page;
        this.totalPages = totalPages;
        this.totalAuctions = totalAuctions;
        this.lastUpdated = lastUpdated;
        this.auctions = auctions;
    }
}
