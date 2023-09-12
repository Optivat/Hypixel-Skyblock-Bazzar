import api.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.apache.ApacheHttpClient;
import net.querz.nbt.io.NBTDeserializer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class MainBazaar {
    public static BazaarResponse bazaarResponse;
    public static AuctionResponse auctionResponse;
    public static Map<String, RecipeItem> craftingRecipes;
    public static Map<User, BazaarItem> notifierPing= new HashMap<>();
    public static JsonObject jsonObject = new JsonObject();
    public static Map<String, List<String>> notifierChannel = new HashMap<>();
    public static Map<String, String> notifierExperiment = new HashMap<>();
    public static Map<String, List<String>> notifierChannelAH = new HashMap<>();
    public static JDA jda;
    public static String VCchannelID = null;

    public static void main(String[] args) throws Exception {

        String token = null;

        //Json File
        String dir = System.getProperty("user.home") + "/FRstudios/hypskyblockbot.json";
        File jsonFile = new File(dir);
        if(jsonFile.exists() && !jsonFile.isDirectory()) {
            Gson gson = new Gson();
            jsonObject = gson.fromJson(new FileReader(dir), JsonObject.class);
            token = jsonObject.get("TOKEN").getAsString();
            VCchannelID = jsonObject.get("VCCHANNELID").getAsString();
            Type notifierPingType = new TypeToken<Map<User, BazaarItem>>(){}.getType();
            notifierPing = gson.fromJson(jsonObject.get("NOTIFIERPING"), notifierPingType);
            Type notifierChannelType = new TypeToken<Map<String, List<String>>>(){}.getType();
            notifierChannel = gson.fromJson(jsonObject.get("NOTIFIERCHANNEL"), notifierChannelType);
            Type notifierExperimentType = new TypeToken<Map<String, String>>(){}.getType();
            notifierExperiment = gson.fromJson(jsonObject.get("NOTIFIEREXPERIMENT"), notifierExperimentType);
            notifierChannelAH = gson.fromJson(jsonObject.get("NOTIFIERCHANNELAH"), notifierChannelType);
        } else {
            new File(System.getProperty("user.home") + "/FRstudios/").mkdirs();
            FileWriter file = new FileWriter(dir);
            jsonObject.add("TOKEN", null);
            jsonObject.add("VCCHANNELID", null);
            jsonObject.add("NOTIFIERPING", JsonParser.parseString(notifierPing.toString()));
            jsonObject.add("NOTIFIERCHANNEL", JsonParser.parseString(notifierChannel.toString()));
            jsonObject.add("NOTIFIERCHANNELAH", JsonParser.parseString(notifierChannelAH.toString()));
            jsonObject.add("NOTIFIEREXPERIMENT", JsonParser.parseString(notifierExperiment.toString()));
            file.write(jsonObject.toString());
            file.close();
        }

        JDABuilder builder = JDABuilder.createDefault(token);

        // Disable parts of the cache
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY, CacheFlag.ONLINE_STATUS);
        // Enable the bulk delete event
        builder.setBulkDeleteSplittingEnabled(false);
        // Set activity (like "playing Something")
        builder.setActivity(Activity.playing("Hypixel Skyblock"));
        builder.setMemberCachePolicy(MemberCachePolicy.VOICE);
        builder.enableIntents(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_WEBHOOKS, GatewayIntent.GUILD_PRESENCES);
        builder.addEventListeners(new BotCommands());
        jda = builder.build();

        JFrame jFrame = new JFrame("Hypixel Skyblock Bot");
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setSize(400,100);
        jFrame.setVisible(true);

        jda.awaitReady();
        for (Guild guild: jda.getGuilds()) {
            guild.upsertCommand("notifyme", "It will notify you when the margin hits 10% for an item").addOption(OptionType.STRING, "item", "The item you want to be notified for", true).queue();
            guild.upsertCommand("notifyhere", "It will notify this channel when the margin hits 10% for an item").addOption(OptionType.INTEGER, "orders7d", "Instant buy/sell orders from the week (prerequisite)", true).addOption(OptionType.NUMBER, "minbuyprice", "Minimum Instant Sell Price, Buy Order Price prereq", true).queue();
            guild.upsertCommand("notifyhereah", "It will notify this channel with the highest bazaar craft vs AH prices").addOption(OptionType.INTEGER, "maxbuycost", "Maximum cost for poor people.", true).addOption(OptionType.NUMBER, "maxprofit", "the max of profit you want", true).queue();
            guild.upsertCommand("experimentnotifyhere", "It will notify with a score.").queue();
            guild.upsertCommand("setvc", "It will notify with a score.").addOption(OptionType.STRING, "vcchannelid", "amongus????", true).queue();
            guild.upsertCommand("stop", "It will stop notifying you in private messages.").queue();
            guild.upsertCommand("stopupdates", "It will stop notifying in the channel that the message is sent in.").queue();
            guild.upsertCommand("stopupdatesah", "It will stop notifying in the channel that the message is sent in.").queue();
            guild.upsertCommand("stopexperiment", "It will stop notifying in the channel that the message is sent in.").queue();
            guild.upsertCommand("sendmessage", "It will send a message.").queue();
            guild.upsertCommand("tellme", "Gives detail on an item, LIST THE ID").addOption(OptionType.STRING, "bazaarid", "The ID, not the IGN.", true).queue();
            guild.upsertCommand("search", "Searches each ID").addOption(OptionType.STRING, "name", "Name, not case sensitive", true).queue();
            guild.upsertCommand("highestmarginpercent", "Finds items with the highest margin.").addOption(OptionType.INTEGER, "orders7d", "Instant buy/sell orders from the week (prerequisite)", true).addOption(OptionType.NUMBER, "minbuyprice", "Minimum Instant Sell Price, Buy Order Price prereq", true).queue();
        }
        
        while(true) {
            Gson gson = new Gson();
            String jsonCrafting = null;
            String jsonBazaar = null;
            String jsonAuction = null;

            jsonBazaar = readUrl("https://api.hypixel.net/skyblock/bazaar");
            jsonAuction = readUrl("https://api.hypixel.net/skyblock/auctions");
            jsonCrafting = readUrl("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json");

            bazaarResponse = gson.fromJson(jsonBazaar, BazaarResponse.class);
            auctionResponse = gson.fromJson(jsonAuction, AuctionResponse.class);
            craftingRecipes = gson.fromJson(jsonCrafting, new TypeToken<Map<String, RecipeItem>>(){}.getType());

            synchronized (jda) {
                jda.wait(7500);
            }

            jsonObject.remove("NOTIFIERPING");
            jsonObject.remove("NOTIFIERCHANNEL");
            jsonObject.remove("NOTIFIERCHANNELAH");
            jsonObject.remove("NOTIFIEREXPERIMENT");

            //Updating Json
            FileWriter file = new FileWriter(dir);
            Gson gson1 = new Gson();
            jsonObject.addProperty("TOKEN", token);
            jsonObject.addProperty("VCCHANNELID", VCchannelID);
            jsonObject.add("NOTIFIERPING", gson1.toJsonTree(notifierPing));
            jsonObject.add("NOTIFIERCHANNEL", gson1.toJsonTree(notifierChannel));
            jsonObject.add("NOTIFIERCHANNELAH", gson1.toJsonTree(notifierChannelAH));
            jsonObject.add("NOTIFIEREXPERIMENT", gson1.toJsonTree(notifierExperiment));
            file.write(jsonObject.toString());
            file.close();
            Random rand = new Random();
            int randNum = rand.nextInt(1, 1000);

            //FunnyChannelJoin
            if(jda.getTextChannelById("1136117002080096314") != null) {
                ArrayList<AuctionItem> binAuctions = getAllBinAuctions();
                Map<String, ArrayList<AuctionItem>> organizedPrices = new HashMap<>();
                Map<AuctionItem, Float> steals = new HashMap<>();
                for(AuctionItem item : binAuctions) {
                    if(!organizedPrices.containsKey(item.itemID)) {
                        ArrayList<AuctionItem> longs = new ArrayList<>();
                        longs.add(item);
                        organizedPrices.put(item.itemID, longs);
                    } else {
                        ArrayList<AuctionItem> longs;
                        longs = organizedPrices.get(item.itemID);
                        longs.add(item);
                        organizedPrices.put(item.itemID, longs);
                    }
                }
                for(String name : organizedPrices.keySet()) {
                    ArrayList<AuctionItem> items = organizedPrices.get(name);
                    if(items.size() != 1) {
                        ArrayList<Long> prices = new ArrayList<>();
                        for(AuctionItem item : items) {
                            prices.add(item.starting_bid);
                        }
                        Collections.sort(prices);
                        //Collections.reverse(prices);
                        //long Q1 = prices.get((prices.size()-1)/4);
                        //long Q3 = prices.get(((prices.size()-1)/4)*3);
                        //long IQR = Q3-Q1;
                        //long LowerBound = (long) (Q1-(IQR*1.5));
                        //float percentDiff = ((float) (Math.abs(prices.get(0) - prices.get(1))) /((float) (prices.get(0) + prices.get(1)) /2))*100;
                        float percentDiff = ((float) (prices.get(1) - prices.get(0)) /prices.get(0))*100;
                        //float percentDiff = (float) prices.get(1) - prices.get(0);
                        System.out.println(percentDiff);
                        if(percentDiff >= 100) {
                            for (AuctionItem item : items) {
                                if(item.starting_bid == prices.get(0)) {
                                    steals.put(item, (float) percentDiff);
                                }
                            }
                        }
                        /**AuctionItem awesomeItem = null;
                        long estimatedProfit = 0;
                        for(AuctionItem item : items) {
                            if(item.starting_bid < LowerBound) {
                                awesomeItem = item;
                                estimatedProfit = Q1-item.starting_bid;
                            }
                        }
                        if(awesomeItem != null) {
                            steals.put(awesomeItem, estimatedProfit);
                        }**/
                    }
                }

                EmbedBuilder embedMargin = new EmbedBuilder();

                NumberFormat format = NumberFormat.getInstance();
                format.setGroupingUsed(true);

                embedMargin.setTitle("Auction House Steals");

                if(!steals.isEmpty()) {
                    for(int x = 0; x < 50; x++) {
                        AuctionItem highestProfitItem = null;
                        for(AuctionItem item : steals.keySet()) {
                            if(highestProfitItem == null) {
                                highestProfitItem = item;
                            }
                            if(steals.get(item) > steals.get(highestProfitItem)) {
                                highestProfitItem = item;
                            }
                        }
                        if(highestProfitItem != null) {
                            MojangProfile mojangProfile = gson.fromJson(readUrl("https://api.mojang.com/user/profile/" + highestProfitItem.auctioneer), MojangProfile.class);
                            embedMargin.addField("__**"+ highestProfitItem.item_name + "**__", "Percent Diff: " + format.format(steals.get(highestProfitItem)) + "%\n Price: $" + format.format(highestProfitItem.starting_bid) + "\n Auctioneer: " + mojangProfile.name, false);
                            //embedMargin.addField("__**"+ highestProfitItem.item_name + "**__", "Profit: $" + format.format(steals.get(highestProfitItem)) + "\n Price: $" + format.format(highestProfitItem.starting_bid) + "\n Auctioneer: " + mojangProfile.name, false);
                            steals.remove(highestProfitItem);
                        }
                    }
                } else {
                    embedMargin.setDescription("There are no steals...");
                }
                String channel = "1136117002080096314";
                String messageid = "1136118750219870359";

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();

                embedMargin.setFooter("Bot created by Optivat | Last Updated: " + dateFormat.format(date));
                jda.getTextChannelById(channel).editMessageEmbedsById(messageid, embedMargin.build()).queue();
                embedMargin.clear();
            }

            if(VCchannelID != null && randNum == 1) {
                System.out.println("joined ");
                Guild guild = Objects.requireNonNull(jda.getVoiceChannelById(VCchannelID)).getGuild();

                AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
                AudioSourceManagers.registerRemoteSources(playerManager);

                AudioPlayer player = playerManager.createPlayer();
                TrackScheduler trackScheduler = new TrackScheduler(player);
                player.addListener(trackScheduler);

                playerManager.loadItem("https://www.youtube.com/watch?v=TBTj4vdtqbg&ab_channel=BeastWarrior", new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack audioTrack) {
                            trackScheduler.queue(audioTrack);
                            player.playTrack(audioTrack);
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist audioPlaylist) {
                            for (AudioTrack track : audioPlaylist.getTracks())
                                trackScheduler.queue(track);
                        }

                        @Override
                        public void noMatches() {

                        }

                        @Override
                        public void loadFailed(FriendlyException e) {

                        }
                    });

                VoiceChannel channel = Objects.requireNonNull(jda.getVoiceChannelById(VCchannelID));
                AudioManager manager = guild.getAudioManager();

                manager.setSendingHandler(new AudioPlayerSendHandler(player));
                manager.openAudioConnection(channel);

                synchronized (MainBazaar.jda) {
                    try {
                        MainBazaar.jda.wait(7500);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                manager.closeAudioConnection();

            }

            if(jda.getTextChannelById("1134721323231350854") != null) {
                //Profile ID: 297c6265-d5ea-409f-9715-8dcd5f8e05a5
                Map<String, Long> itemsLowestBin = updateLowestBin(null);
                ArrayList<AuctionItem> itemsProfileBin = getProfileBins("297c6265d5ea409f97158dcd5f8e05a5");

                EmbedBuilder embedMargin = new EmbedBuilder();

                NumberFormat format = NumberFormat.getInstance();
                format.setGroupingUsed(true);
                TimeFormat timeFormat = TimeFormat.TIME_LONG;

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                SimpleDateFormat dateFormatTimeLeft = new SimpleDateFormat("ss S");
                Date date = new Date();

                embedMargin.setTitle("Profile AH Items");
                for(AuctionItem item : itemsProfileBin) {
                    boolean lowestbin = true;
                    MojangProfile mojangProfile = gson.fromJson(readUrl("https://api.mojang.com/user/profile/" + item.auctioneer), MojangProfile.class);
                    if(itemsLowestBin.get(item.itemID) < item.starting_bid) {
                        lowestbin = false;
                    }
                    embedMargin.addField("__**" + item.item_name + "**__", "*Auctioneer:* " + mojangProfile.name + "\n *Starting Bid:* $" + format.format(item.starting_bid) + "\n *End:* " + timeFormat.format(item.end) + "\n *Lowest Bin*: " + lowestbin + "\n", true);
                }
                embedMargin.setColor(Color.GREEN);

                embedMargin.setFooter("Bot created by Optivat | Last Updated: " + dateFormat.format(date));
                String channel = "1134721323231350854";
                String messageid = "1134731321583870002";
                Objects.requireNonNull(jda.getTextChannelById(channel)).editMessageEmbedsById(messageid, embedMargin.build()).queue();
                embedMargin.clear();
            }

            //NotifierChannelAH
            if(!notifierChannelAH.isEmpty()) {
                synchronized (jda) {
                    jda.wait(7500);
                }
                for(String channel : notifierChannelAH.keySet()) {
                    if(MainBazaar.notifierChannelAH.get(channel).size() == 3) {

                        double maxBuyCost = Double.parseDouble(MainBazaar.notifierChannelAH.get(channel).get(0));
                        double maxProfit = Double.parseDouble(MainBazaar.notifierChannelAH.get(channel).get(1));

                        Map<String, String> recipeLinked = new HashMap<>();
                        for (int x = 0; x < craftingRecipes.keySet().size() - 1; x++) {
                            recipeLinked.put((String) craftingRecipes.keySet().toArray()[x], ((RecipeItem) craftingRecipes.values().toArray()[x]).name);
                        }
                        Map<String, Long> itemsLowestBin = updateLowestBin(new HashSet<>(recipeLinked.values()));
                        EmbedBuilder embedMargin = new EmbedBuilder();

                        NumberFormat format = NumberFormat.getInstance();
                        format.setGroupingUsed(true);

                        embedMargin.setTitle("Highest Margin AH Items");
                        embedMargin.setDescription("Best items to craft and flip with the following parameters: \n" +
                                "Maximum Buy Cost: $" + format.format(maxBuyCost) +
                                "\n Max Profit: $" + format.format(maxProfit));

                        long buyCost = 0;
                        String bestName = "ERROR";
                        String recipeName = "ERROR";
                        long profit = 0;
                        for (int x = 0; x < 10; x++) {
                            for (String string : recipeLinked.keySet()) {
                                //Getting Craft Cost
                                if (craftingRecipes.get(string).recipe != null && itemsLowestBin.containsKey(string)) {
                                    Map<String, String> itemRecipe = craftingRecipes.get(string).recipe;
                                    for (String item : itemRecipe.keySet()) {
                                        String[] arrOfStr = itemRecipe.get(item).split(":");
                                        arrOfStr[0] = arrOfStr[0].replace(" ", "");
                                        if (bazaarResponse.products.containsKey(arrOfStr[0])) {
                                            if (arrOfStr.length == 2) {
                                                buyCost += bazaarResponse.products.get(arrOfStr[0]).quick_status.buyPrice * Long.parseLong(arrOfStr[1]);
                                            } else {
                                                buyCost += bazaarResponse.products.get(arrOfStr[0]).quick_status.buyPrice;
                                            }
                                        } else if (itemsLowestBin.containsKey(string)) {
                                            //System.out.println(itemsLowestBin.get(string) + ", " + recipeLinked.get(string));
                                            buyCost += itemsLowestBin.get(string);
                                        }
                                    }
                                    long currentProfit = (long) (itemsLowestBin.get(string) - buyCost);
                                    if (currentProfit <= maxProfit && maxBuyCost >= buyCost) {
                                        if (currentProfit > profit) {
                                            profit = currentProfit;
                                            bestName = string;
                                            recipeName = recipeLinked.get(string);
                                        }
                                    }
                                    buyCost = 0;
                                }
                            }
                            recipeLinked.remove(bestName, recipeName);
                            embedMargin.addField("__" + recipeName + "__", "**$" + format.format(profit) + " ** NET PROFIT \n" +
                                    "LB: $" + format.format(itemsLowestBin.get(bestName)) + " | BC: $" + format.format(itemsLowestBin.get(bestName) - profit), false);
                            profit = 0;
                        }
                        embedMargin.setColor(Color.GREEN);

                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();

                        embedMargin.setFooter("Bot created by Optivat | Last Updated: " + dateFormat.format(date));
                        Objects.requireNonNull(jda.getTextChannelById(channel)).editMessageEmbedsById(MainBazaar.notifierChannelAH.get(channel).get(2), embedMargin.build()).queue();
                        embedMargin.clear();
                    }
                }
            }

            //NotifierChannel
            if(!notifierChannel.isEmpty()) {
                for(String channel : notifierChannel.keySet()) {

                    int ordersReq = Integer.parseInt(String.valueOf(MainBazaar.notifierChannel.get(channel).get(0)));
                    double minBuyPrice = Double.parseDouble(String.valueOf(MainBazaar.notifierChannel.get(channel).get(1)));
                    Set<String> itemsSacrifice = new HashSet<>(bazaarResponse.products.keySet());
                    itemsSacrifice.removeIf(item -> (MainBazaar.bazaarResponse.products.get(item).quick_status.sellMovingWeek < ordersReq && MainBazaar.bazaarResponse.products.get(item).quick_status.buyMovingWeek < ordersReq));
                    itemsSacrifice.removeIf(item -> MainBazaar.bazaarResponse.products.get(item).quick_status.buyPrice < minBuyPrice && MainBazaar.bazaarResponse.products.get(item).quick_status.sellPrice < minBuyPrice);
                    itemsSacrifice.removeIf(item -> ((float)Math.round(MainBazaar.bazaarResponse.products.get(item).quick_status.sellPrice*100)/(float)100) == 0);
                    EmbedBuilder embedMargin = new EmbedBuilder();

                    NumberFormat format = NumberFormat.getInstance();
                    format.setGroupingUsed(true);

                    embedMargin.setTitle("Highest Margin Items");
                    embedMargin.setDescription("All of these items have at least: \n" +
                            format.format(ordersReq) + " orders in sell and buy. \n" +
                            "$" + format.format(minBuyPrice) + " Minimum Instant Sell Price or Buy Order Price");

                    BazaarItem highMarginItem = null;
                    double diffPercent = 0 ;
                    for(int x = 0; x < 10; x++) {
                        for(String item : itemsSacrifice) {
                            double currentDiffPercent = ((MainBazaar.bazaarResponse.products.get(item).quick_status.buyPrice - MainBazaar.bazaarResponse.products.get(item).quick_status.sellPrice)/MainBazaar.bazaarResponse.products.get(item).quick_status.sellPrice)*100;
                            if (currentDiffPercent >= diffPercent) {
                                highMarginItem = MainBazaar.bazaarResponse.products.get(item);
                                diffPercent = currentDiffPercent;
                            }
                        }
                        if(highMarginItem != null) {
                            embedMargin.addField("__" + highMarginItem.product_id.replace("_", " ") + "__", "**" + format.format((float)Math.round(diffPercent*100)/(float)100) + "%** \n B: $"
                                    + format.format((float)Math.round(highMarginItem.quick_status.buyPrice*100)/(float)100) + " | S: $"
                                    + format.format((float)Math.round(highMarginItem.quick_status.sellPrice*100)/(float)100), false);
                            itemsSacrifice.remove(highMarginItem.product_id);
                            diffPercent = 0;
                        } else {
                            embedMargin.addField("ERROR", (float)Math.round(diffPercent*100)/(float)100 + "%", false);
                            itemsSacrifice.remove("ERROR");
                            diffPercent = 0;
                        }
                    }
                    embedMargin.setColor(Color.GREEN);

                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();

                    embedMargin.setFooter("Bot created by Optivat | Last Updated: " + dateFormat.format(date));
                    Objects.requireNonNull(jda.getTextChannelById(channel)).editMessageEmbedsById(MainBazaar.notifierChannel.get(channel).get(2), embedMargin.build()).queue();
                    embedMargin.clear();
                }
            }

            //NotifierExperiment
            if(!notifierExperiment.isEmpty()) {
                for(String channel : notifierExperiment.keySet()) {

                    Set<String> itemsSacrifice = new HashSet<>(bazaarResponse.products.keySet());

                    EmbedBuilder embedMargin = new EmbedBuilder();

                    NumberFormat format = NumberFormat.getInstance();
                    format.setGroupingUsed(true);

                    embedMargin.setTitle("Highest Scored Items");
                    embedMargin.setDescription("All of these items follow a unique score calculator. It is very experimental.");

                    itemsSacrifice.removeIf(item -> MainBazaar.bazaarResponse.products.get(item).quick_status.sellOrders == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.buyOrders == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.sellPrice == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.buyPrice == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.sellMovingWeek == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.buyMovingWeek == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.buyVolume == 0 || MainBazaar.bazaarResponse.products.get(item).quick_status.sellVolume == 0);
                    itemsSacrifice.removeIf(item -> item.contains("ENCHANTMENT"));

                    BazaarItem highScoreItem = null;
                    float highScore = 0 ;
                    for(int x = 0; x < 10; x++) {
                        for(String item : itemsSacrifice) {
                            BazaarItem.BazaarItemSummary itemSum = MainBazaar.bazaarResponse.products.get(item).quick_status;
                            float currentScore = 0;
                            if (itemSum.sellMovingWeek > itemSum.buyMovingWeek) {
                                currentScore = ((long) ((itemSum.buyMovingWeek)/168)
                                        *((float) (itemSum.buyPrice-itemSum.sellPrice))
                                        *((float) (itemSum.sellVolume/itemSum.buyVolume)));
                            } else {
                                currentScore = ((long) ((itemSum.sellMovingWeek)/168)
                                        *((float) (itemSum.buyPrice-itemSum.sellPrice))
                                        *((float) (itemSum.sellVolume/itemSum.buyVolume)));
                            }
                            if (currentScore >= highScore) {
                                highScoreItem = MainBazaar.bazaarResponse.products.get(item);
                                highScore = currentScore;
                            }
                        }
                        if(highScoreItem!= null) {
                            embedMargin.addField(highScoreItem.product_id.replace("_", " "), format.format((long)highScore), false);
                            itemsSacrifice.remove(highScoreItem.product_id);
                        } else {
                            embedMargin.addField("ERROR", format.format((long)highScore), false);
                            itemsSacrifice.remove("ERROR");
                        }
                        highScore = 0;
                    }
                    embedMargin.setColor(Color.GREEN);

                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();

                    embedMargin.setFooter("Bot created by Optivat | Last Updated: " + dateFormat.format(date));
                    Objects.requireNonNull(jda.getTextChannelById(channel)).editMessageEmbedsById(MainBazaar.notifierExperiment.get(channel), embedMargin.build()).queue();
                    embedMargin.clear();
                }
            }
            //NotifierPing
            if(!notifierPing.isEmpty()) {
                for(User user: notifierPing.keySet()) {
                    user.openPrivateChannel().queue((channel) ->
                    {
                        BazaarItem item = notifierPing.get(user);

                        NumberFormat format = NumberFormat.getInstance();
                        format.setGroupingUsed(true);

                        DecimalFormat decimalFormat = new DecimalFormat("#.#");

                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle(item.product_id.replace("_", " "));
                        embed.addField("# Of Buy Orders", format.format(item.quick_status.buyOrders), false);
                        embed.addField("Current Buy Price", "$" + format.format(Double.valueOf(decimalFormat.format(item.quick_status.buyPrice))), false);
                        embed.addField("# Of Sell Orders", format.format(item.quick_status.sellOrders), false);
                        embed.addField("Current Sell Price", "$" + format.format(Double.valueOf(decimalFormat.format(item.quick_status.sellPrice))), false);

                        embed.setColor(Color.GREEN);

                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();

                        embed.setFooter("Bot created by Optivat | Last Updated: " + dateFormat.format(date));
                        channel.sendMessageEmbeds(embed.build()).queue();
                        embed.clear();
                    });
                }
            }
        }
    }
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }
    public static Map<String, Long> updateLowestBin(Set<String> recipes) throws Exception {
        Map<String, Long> itemsLowestBin = new HashMap<>();
        Gson gson = new Gson();
        if (recipes == null) {
            for (int y = 0; y < auctionResponse.totalPages; y++) {
                auctionResponse = gson.fromJson(readUrl("https://api.hypixel.net/skyblock/auctions?page=" + y), AuctionResponse.class);
                for(AuctionItem item : auctionResponse.auctions) {
                    byte[] decoded = Base64.getDecoder().decode(item.item_bytes.getBytes());
                    NBTDeserializer nbt = new NBTDeserializer(true);
                    Map<String, ItemBytes>  itemBytesMap = gson.fromJson(nbt.fromBytes(decoded).getTag().valueToString(), new TypeToken<Map<String, ItemBytes>>(){}.getType());
                    //System.out.println(itemBytesMap.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value);

                    if(item.bin) {
                        String itemID = itemBytesMap.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value;
                        if(!itemsLowestBin.containsKey(itemID)) {
                            itemsLowestBin.put(itemID, item.starting_bid);
                        } else if (itemsLowestBin.get(itemID) > item.starting_bid) {
                            itemsLowestBin.remove(itemID, item.starting_bid);
                            itemsLowestBin.put(itemID, item.starting_bid);
                        }
                    }
                }
            }
        } else {
            for (int y = 0; y < auctionResponse.totalPages; y++) {
                auctionResponse = gson.fromJson(readUrl("https://api.hypixel.net/skyblock/auctions?page=" + y), AuctionResponse.class);
                for(AuctionItem item : auctionResponse.auctions) {
                    byte[] decoded = Base64.getDecoder().decode(item.item_bytes.getBytes());
                    NBTDeserializer nbt = new NBTDeserializer(true);
                    Map<String, ItemBytes>  itemBytesMap = gson.fromJson(nbt.fromBytes(decoded).getTag().valueToString(), new TypeToken<Map<String, ItemBytes>>(){}.getType());
                    //System.out.println(itemBytesMap.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value);

                    if(recipes.contains(item.item_name) && item.bin) {
                        String itemID = itemBytesMap.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value;
                        if(!itemsLowestBin.containsKey(itemID)) {
                            itemsLowestBin.put(itemID, item.starting_bid);
                        } else if (itemsLowestBin.get(itemID) > item.starting_bid) {
                            itemsLowestBin.remove(itemID, item.starting_bid);
                            itemsLowestBin.put(itemID, item.starting_bid);
                        }
                    }
                }
            }
        }
        return itemsLowestBin;
    }
    public static ArrayList<AuctionItem> getAllBinAuctions() throws Exception {
        Gson gson = new Gson();
        ArrayList<AuctionItem> itemsLowestBin = new ArrayList<>();
        for (int y = 0; y < auctionResponse.totalPages; y++) {
            auctionResponse = gson.fromJson(readUrl("https://api.hypixel.net/skyblock/auctions?page=" + y), AuctionResponse.class);
            for(AuctionItem item : auctionResponse.auctions) {
                if(item.bin) {
                    byte[] decoded = Base64.getDecoder().decode(item.item_bytes.getBytes());
                    NBTDeserializer nbt = new NBTDeserializer(true);
                    Map<String, ItemBytes>  itemBytesMap = gson.fromJson(nbt.fromBytes(decoded).getTag().valueToString(), new TypeToken<Map<String, ItemBytes>>(){}.getType());
                    item.itemID = itemBytesMap.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value;
                    itemsLowestBin.add(item);
                }
            }
        }
        return itemsLowestBin;
    }
    public static ArrayList<AuctionItem> getProfileBins(String profileid) throws Exception {
        ArrayList<AuctionItem> itemsProfileBins = new ArrayList<>();
        Gson gson = new Gson();
        for (int y = 0; y < auctionResponse.totalPages; y++) {
            auctionResponse = gson.fromJson(readUrl("https://api.hypixel.net/skyblock/auctions?page=" + y), AuctionResponse.class);
            for(AuctionItem item : auctionResponse.auctions) {
                byte[] decoded = Base64.getDecoder().decode(item.item_bytes.getBytes());
                NBTDeserializer nbt = new NBTDeserializer(true);
                Map<String, ItemBytes>  itemBytesMap = gson.fromJson(nbt.fromBytes(decoded).getTag().valueToString(), new TypeToken<Map<String, ItemBytes>>(){}.getType());

                if(item.profile_id.equalsIgnoreCase(profileid)) {
                    itemsProfileBins.add(item);
                    item.itemID = itemBytesMap.get("i").value.list.get(0).tag.value.ExtraAttributes.value.id.value;
                }
            }
        }
        return itemsProfileBins;
    }
}
