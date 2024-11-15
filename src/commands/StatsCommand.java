package commands;

import data.BotAdmins;
import data.CommandCategory;
import managers.ApplicationStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import osu.tracking.OsuRefreshRunnable;
import osu.tracking.OsuTrackingManager;
import utils.Constants;
import utils.DiscordChatUtils;
import utils.GeneralUtils;
import utils.TimeUtils;

public class StatsCommand extends Command {

	public StatsCommand() {
		super(null, false, true, CommandCategory.ADMIN, new String[]{"botstats"}, 
			  "Shows o!alt tracker stats.", 
			  "Shows a variety of stats linked to this bot.", 
			  new String[]{"botstats", "Shows the stats page."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		boolean showFullStats = BotAdmins.isAdmin(p_event.getAuthor()) && (p_args.length == 0 || !p_args[0].equalsIgnoreCase("simple"));
		EmbedBuilder builder = new EmbedBuilder();
		ApplicationStats stats = ApplicationStats.getInstance();
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);
		builder.setAuthor("o!alt tracker stats page", Constants.SUPPORT_SERVER_LINK,
						  p_event.getJDA().getSelfUser().getAvatarUrl());
		builder.setDescription("**__" + TimeUtils.toDuration(stats.getUptime(), false) + "__** uptime");
		
		if(showFullStats) {
			builder.addField("Startup Time", TimeUtils.toDuration(stats.getStartupTime(), true), true);
			
			int stalled = stats.getOsuApiStallQuantity();
			builder.addField("osu! api status", ((Constants.OSU_API_CLIENTS_AUTHENTICATED - stalled) + " Running " + stalled + " Paused"), true);
			
			String requestsSentLastMinuteString = "";
			double totalRequests = 0;
			for (int i = 0; i < Constants.OSU_API_CLIENTS_AUTHENTICATED + 1; ++i) {
				double requests = 0;
				
				if (i == 0) requests = Constants.OSU_HTML_REQUESTS_PER_MINUTE * stats.getOsuHtmlLoad();
				else requests = Constants.OSU_API_REQUESTS_PER_MINUTE_PER_KEY * stats.getOsuApiLoad(i - 1);
				
				totalRequests += requests;
				requestsSentLastMinuteString += GeneralUtils.toFormattedNumber(requests) + " / ";
			}
			
			requestsSentLastMinuteString = GeneralUtils.toFormattedNumber(totalRequests) + " total\n" + requestsSentLastMinuteString;
			
			builder.addField("osu! html/api sent last minute", requestsSentLastMinuteString.substring(0, requestsSentLastMinuteString.length() - 3), true);
			builder.addField("o!api requests sent/failed", 
							 stats.getCombinedOsuApiRequestsSent() + " / " + stats.getCombinedOsuApiRequestsFailed(), true);
			builder.addField("o!html requests sent/failed", 
							 stats.getOsuHtmlRequestsSent() + " / " + stats.getOsuHtmlRequestsFailed(), true);
		}
		
		builder.addField("Total registered osu! users", osuTrackManager.getLoadedRegisteredUsers() + " ", false);
		builder.addField("Scores uploaded", String.valueOf(stats.getScoresUploaded()), true);
		
		for(int i = 0; i < Constants.OSU_ACTIVITY_CYCLES.length; ++i) {
			OsuRefreshRunnable cycleRunnable = osuTrackManager.getRefreshRunnable(i);
			String fieldText = "";
			
			if(cycleRunnable != null) {
				fieldText = (showFullStats ? cycleRunnable.getUsersLeft() : cycleRunnable.getInitialUserListSize() - cycleRunnable.getUsersLeft()) + "/";
				
				fieldText += cycleRunnable.getInitialUserListSize() + " | ";
				
				if(showFullStats) {
					fieldText += TimeUtils.toDuration(cycleRunnable.getAverageUserRefreshDelay(), true) + " / ";
					fieldText += TimeUtils.toDuration(cycleRunnable.getTimeElapsed(), false) + " / ";
				}
				
				long expectedTimeLeft = cycleRunnable.getExpectedTimeUntilStop();
				String expectedTimeLeftSign = "";
				if(expectedTimeLeft < 0) { 
					expectedTimeLeft = -expectedTimeLeft;
					expectedTimeLeftSign = "-";
				}
				
				fieldText += expectedTimeLeftSign + TimeUtils.toDuration(expectedTimeLeft, false) + " left";
				
				if(showFullStats)
					fieldText += " / " + TimeUtils.toDuration(cycleRunnable.getTimeUntilStop(), false) + " left";
			} else fieldText = "Not running";
			
			String cycleName = i < Constants.OSU_FULL_REFRESH_ACTIVITY_CYCLE_COUNT ? "Live Tracking Cycle" : "Cycle " + (i - Constants.OSU_FULL_REFRESH_ACTIVITY_CYCLE_COUNT);
			
			if(showFullStats)
				builder.addField(cycleName + " users left/total | avg delay/elapsed/expected time left/calc'd time left", fieldText, false);
			else
				builder.addField(cycleName + " progress", fieldText, false);
		}
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
