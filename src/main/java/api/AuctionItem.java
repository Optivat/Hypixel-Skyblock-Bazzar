package api;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.querz.nbt.io.NBTDeserializer;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class AuctionItem {
    public String objectID;
    public String profile_id;
    public String uuid;
    public String auctioneer;
    public ArrayList<String> coop;
    public long start;
    public long end;
    public String item_name;
    public String item_lore;
    public String extra;
    public String category;
    public String tier;
    public long starting_bid;
    public String item_bytes;
    public boolean claimed;
    public ArrayList<String> claimed_bidders;
    public long highest_bid_amount;
    public long last_updated;
    public boolean bin;
    public ArrayList<Object> bids;
    public Map<String, ItemBytes> itemBytesDecoded;
    public String itemID;
    public AuctionItem(String uuid, String auctioneer, String profile_id, ArrayList<String> coop, long start, long end, String item_name, String item_lore, String extra, String category, String tier, long starting_bid, String item_bytes, boolean claimed, ArrayList<String> claimed_bidders, long highest_bid_amount, long last_updated, boolean bin, ArrayList<Object> bids, String objectID) throws IOException {
        this.auctioneer = auctioneer;
        this.bids = bids;
        this.category = category;
        this.claimed = claimed;
        this.highest_bid_amount = highest_bid_amount;
        this.claimed_bidders = claimed_bidders;
        this.item_bytes = item_bytes;
        this.item_lore = item_lore;
        this.item_name = item_name;
        this.objectID = objectID;
        this.uuid = uuid;
        this.coop = coop;
        this.start = start;
        this.end = end;
        this.extra = extra;
        this.tier = tier;
        this.starting_bid = starting_bid;
        this.bin = bin;
        this.last_updated = last_updated;
        byte[] decoded = Base64.getDecoder().decode(item_bytes.getBytes());
        NBTDeserializer nbt = new NBTDeserializer(true);
        this.itemBytesDecoded = (new Gson()).fromJson(nbt.fromBytes(decoded).getTag().valueToString(), new TypeToken<Map<String, ItemBytes>>(){}.getType());
        this.itemID = itemBytesDecoded.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value;

    }



}
