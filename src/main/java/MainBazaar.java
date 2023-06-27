import api.BazaarItem;
import api.BazaarResponse;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class MainBazaar {
    public static BazaarResponse bazaarResponse;
    public static Map<User, BazaarItem> notifierPing= new HashMap<>();
    public static JsonObject jsonObject = new JsonObject();
    public static Map<String, List<Double>> notifierChannel = new HashMap<>();
    public static JDA jda;
    public static void main(String[] args) throws InterruptedException, IOException {

        String token = null;

        //Json File
        String dir = System.getProperty("user.home") + "/FRstudios/hypskyblockbot.json";
        File jsonFile = new File(dir);
        if(jsonFile.exists() && !jsonFile.isDirectory()) {
            Gson gson = new Gson();
            jsonObject = gson.fromJson(new FileReader(dir), JsonObject.class);
            token = jsonObject.get("TOKEN").getAsString();
            Type notifierPingType = new TypeToken<Map<User, BazaarItem>>(){}.getType();
            notifierPing = gson.fromJson(jsonObject.get("NOTIFIERPING"), notifierPingType);
            Type notifierChannelType = new TypeToken<Map<String, List<Double>>>(){}.getType();
            notifierChannel = gson.fromJson(jsonObject.get("NOTIFIERCHANNEL"), notifierChannelType);
        } else {
            new File(System.getProperty("user.home") + "/FRstudios/").mkdirs();
            FileWriter file = new FileWriter(dir);
            jsonObject.add("TOKEN", null);
            jsonObject.add("NOTIFIERPING", JsonParser.parseString(notifierPing.toString()));
            jsonObject.add("NOTIFIERCHANNEL", JsonParser.parseString(notifierChannel.toString()));
            file.write(jsonObject.toString());
            file.close();
        }

        JDABuilder builder = JDABuilder.createDefault(token);

        // Disable parts of the cache
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        // Enable the bulk delete event
        builder.setBulkDeleteSplittingEnabled(false);
        // Set activity (like "playing Something")
        builder.setActivity(Activity.playing("Hypixel Skyblock"));
        builder.enableIntents(GatewayIntent.DIRECT_MESSAGES);
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
            guild.upsertCommand("stop", "It will stop notifying you in private messages.").queue();
            guild.upsertCommand("stopupdates", "It will stop notifying in the channel that the message is sent in.").queue();
            guild.upsertCommand("tellme", "Gives detail on an item, LIST THE ID").addOption(OptionType.STRING, "bazaarid", "The ID, not the IGN.", true).queue();
            guild.upsertCommand("search", "Searches each ID").addOption(OptionType.STRING, "name", "Name, not case sensitive", true).queue();
            guild.upsertCommand("highestmarginpercent", "Finds items with the highest margin.").addOption(OptionType.INTEGER, "orders7d", "Instant buy/sell orders from the week (prerequisite)", true).addOption(OptionType.NUMBER, "minbuyprice", "Minimum Instant Sell Price, Buy Order Price prereq", true).queue();
            guild.updateCommands().queue();
        }


        while(true) {
            Gson gson = new Gson();
            String json = null;
            try {
                json = readUrl("https://api.hypixel.net/skyblock/bazaar");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            bazaarResponse = gson.fromJson(json, BazaarResponse.class);

            synchronized (jda) {
                jda.wait(2500);
            }

            jsonObject.remove("NOTIFIERPING");
            jsonObject.remove("NOTIFIERCHANNEL");

            //Updating Json
            FileWriter file = new FileWriter(dir);
            Gson gson1 = new Gson();
            jsonObject.addProperty("TOKEN", token);
            jsonObject.add("NOTIFIERPING", gson1.toJsonTree(notifierPing));
            jsonObject.add("NOTIFIERCHANNEL", gson1.toJsonTree(notifierChannel));
            file.write(jsonObject.toString());
            file.close();

            //NotifierChannel
            if(!notifierChannel.isEmpty()) {
                for(String channel : notifierChannel.keySet()) {
                    int ordersReq = (int) Math.round(MainBazaar.notifierChannel.get(channel).get(0));
                    double minBuyPrice = MainBazaar.notifierChannel.get(channel).get(1);
                    Set<String> itemsSacrifice = MainBazaar.bazaarResponse.products.keySet();
                    itemsSacrifice.removeIf(item -> (MainBazaar.bazaarResponse.products.get(item).quick_status.sellMovingWeek < ordersReq && MainBazaar.bazaarResponse.products.get(item).quick_status.buyMovingWeek < ordersReq));
                    itemsSacrifice.removeIf(item -> MainBazaar.bazaarResponse.products.get(item).quick_status.sellPrice < minBuyPrice);
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
                        assert highMarginItem != null;
                        embedMargin.addField(highMarginItem.product_id.replace("_", " "), (double)Math.round(diffPercent*100)/(double)100 + "%", false);
                        itemsSacrifice.remove(highMarginItem.product_id);
                        diffPercent = 0;
                    }
                    embedMargin.setColor(Color.RED);

                    embedMargin.setFooter("Bot created by Optivat");
                    Objects.requireNonNull(jda.getTextChannelById(channel)).sendMessageEmbeds(embedMargin.build()).queue();
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

                        embed.setColor(Color.RED);

                        embed.setFooter("Bot created by Optivat");
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
}
