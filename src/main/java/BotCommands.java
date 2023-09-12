import api.BazaarItem;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Widget;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;


public class BotCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        switch(e.getName()) {
            case "sendmessage":
                e.deferReply().queue();
                e.getHook().sendMessage("Ok!").queue();
                break;
            case "notifyme":
                String notifyItem = Objects.requireNonNull(e.getOption("item")).getAsString().toUpperCase();
                if(MainBazaar.bazaarResponse.products.keySet().toString().contains(notifyItem)) {
                    e.deferReply().queue();
                    MainBazaar.notifierPing.put(e.getUser(), MainBazaar.bazaarResponse.products.get(notifyItem));
                    e.getHook().sendMessage("Notifications are sending! Do /stop to stop being notified.").queue();
                } else {
                    e.deferReply().queue();
                    e.getHook().sendMessage("Invalid ID!").queue();
                }

                break;
            case "notifyhere":
                e.deferReply().queue();
                int ordersReqHere = Objects.requireNonNull(e.getOption("orders7d")).getAsInt();
                double minBuyPriceHere = Objects.requireNonNull(e.getOption("minbuyprice")).getAsDouble();
                MainBazaar.notifierChannel.put(e.getChannel().getId(), new ArrayList<>());
                MainBazaar.notifierChannel.get(e.getChannel().getId()).add(String.valueOf(ordersReqHere));
                MainBazaar.notifierChannel.get(e.getChannel().getId()).add(String.valueOf(minBuyPriceHere));
                e.getHook().sendMessage("Notifications are now sending to this channel!").queue();
                try {
                    MainBazaar.jda.awaitReady();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                e.getHook().sendMessage("This message will be updated constantly.").queue(message -> { try {
                    MainBazaar.jda.awaitReady();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }MainBazaar.notifierChannel.get(e.getChannel().getId()).add(message.getId());});
                break;
            case "notifyhereah":
                e.deferReply().queue();
                double maxBuyCost = Objects.requireNonNull(e.getOption("maxbuycost")).getAsDouble();
                double maxProfit = Objects.requireNonNull(e.getOption("maxprofit")).getAsDouble();
                MainBazaar.notifierChannelAH.put(e.getChannel().getId(), new ArrayList<>());
                MainBazaar.notifierChannelAH.get(e.getChannel().getId()).add(String.valueOf(maxBuyCost));
                MainBazaar.notifierChannelAH.get(e.getChannel().getId()).add(String.valueOf(maxProfit));
                e.getHook().sendMessage("Notifications are now sending to this channel!").queue();
                e.getHook().sendMessage("This message will be updated constantly.").queue();
                e.getHook().sendMessage("This message will be updated constantly.").queue(message -> {MainBazaar.notifierChannelAH.get(e.getChannel().getId()).add(message.getId());});
                break;
            case "experimentnotifyhere":
                e.deferReply().queue();
                e.getHook().sendMessage("Notifications are now sending to this channel!").queue();
                e.getHook().sendMessage("This message will be updated constantly.").queue(message -> { MainBazaar.notifierExperiment.put(e.getChannel().getId(), message.getId());});
                break;
            case "stop":
                e.deferReply().queue();
                if(MainBazaar.notifierPing.containsKey(e.getUser())) {
                    MainBazaar.notifierPing.remove(e.getUser());
                    e.getHook().sendMessage("You have been removed").queue();
                } else {
                    e.getHook().sendMessage("You were never being notified.").queue();
                }
                break;
            case "stopexperiment":
                e.deferReply().queue();
                if(MainBazaar.notifierExperiment.containsKey(e.getChannel().getId())) {
                    MainBazaar.notifierExperiment.remove(e.getChannel().getId());
                    e.getHook().sendMessage("This channel has removed.").queue();
                } else {
                    e.getHook().sendMessage("This channel was never getting updated.").queue();
                }
                break;
            case "stopupdates":
                e.deferReply().queue();
                if(MainBazaar.notifierChannel.containsKey(e.getChannel().getId())) {
                    MainBazaar.notifierChannel.remove(e.getChannel().getId());
                    e.getHook().sendMessage("This channel has removed.").queue();
                } else {
                    e.getHook().sendMessage("This channel was never getting updated.").queue();
                }
                break;
            case "stopupdatesah":
                e.deferReply().queue();
                if(MainBazaar.notifierChannelAH.containsKey(e.getChannel().getId())) {
                    MainBazaar.notifierChannelAH.remove(e.getChannel().getId());
                    e.getHook().sendMessage("This channel has removed.").queue();
                } else {
                    e.getHook().sendMessage("This channel was never getting updated.").queue();
                }
                break;
            case "play":
                e.deferReply().queue();
                String channelIDvc = Objects.requireNonNull(e.getOption("vcchannelid")).getAsString();
                if(Objects.requireNonNull(e.getGuild()).getVoiceChannelById(channelIDvc) == (null)) {
                    e.getHook().sendMessage("Invalid Channel ID").queue();
                    return;
                } else {
                    MainBazaar.VCchannelID = channelIDvc;
                    e.getHook().sendMessage("pog").queue();
                }
                break;
            case "tellme":
                String id = Objects.requireNonNull(e.getOption("bazaarid")).getAsString().toUpperCase().replace(" ", "_");
                if(MainBazaar.bazaarResponse.products.containsKey(id)) {
                    e.deferReply().queue();

                    BazaarItem item = MainBazaar.bazaarResponse.products.get(id);

                    NumberFormat format = NumberFormat.getInstance();
                    format.setGroupingUsed(true);

                    DecimalFormat decimalFormat = new DecimalFormat("#.#");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(item.product_id.replace("_", " "));
                    embed.addField("Instant Buy Orders (7d)", format.format(item.quick_status.buyMovingWeek), false);
                    embed.addField("Buy Price", "$" + format.format(Double.valueOf(decimalFormat.format(item.quick_status.buyPrice))), false);
                    embed.addField("Instant Sell Orders (7d)", format.format(item.quick_status.sellMovingWeek), false);
                    embed.addField("Sell Price", "$" + format.format(Double.valueOf(decimalFormat.format(item.quick_status.sellPrice))), false);

                    embed.setColor(Color.GREEN);

                    embed.setFooter("Bot created by Optivat");
                    e.getHook().sendMessageEmbeds(embed.build()).queue();
                    embed.clear();
                } else {
                    e.deferReply().queue();
                    e.getHook().sendMessage("Invalid ID!").queue();

                }
                break;
            case "search":
                String search = Objects.requireNonNull(e.getOption("name")).getAsString().toUpperCase();
                if(MainBazaar.bazaarResponse.products.keySet().toString().contains(search)) {
                    e.deferReply().queue();
                    Set<String> searchResponses = MainBazaar.bazaarResponse.products.keySet();
                    searchResponses.removeIf(string -> !string.contains(search));
                    String searchList = searchResponses.toString().replace("[","").replace("]","").replace(",", "\n").replace(" ", "");
                    if(searchList.toCharArray().length > 2000) {
                        e.getHook().sendMessage("There are too many items with that search! Can you be more specific?").queue();
                    } else {
                        e.getHook().sendMessage(searchList).queue();
                    }
                } else {
                    e.deferReply().queue();
                    e.getHook().sendMessage("There are no IDs that match your search.").queue();
                }
                break;
            case "highestmarginpercent":
                int ordersReq = Objects.requireNonNull(e.getOption("orders7d")).getAsInt();
                double minBuyPrice = Objects.requireNonNull(e.getOption("minbuyprice")).getAsDouble();
                Set<String> itemsSacrifice = MainBazaar.bazaarResponse.products.keySet();
                e.deferReply().queue();
                itemsSacrifice.removeIf(item -> MainBazaar.bazaarResponse.products.get(item).quick_status.sellMovingWeek < ordersReq && MainBazaar.bazaarResponse.products.get(item).quick_status.buyMovingWeek < ordersReq);
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
                embedMargin.setColor(Color.GREEN);

                embedMargin.setFooter("Bot created by Optivat");
                e.getHook().sendMessageEmbeds(embedMargin.build()).queue();
                embedMargin.clear();
                break;
            default:
        }
    }
}
