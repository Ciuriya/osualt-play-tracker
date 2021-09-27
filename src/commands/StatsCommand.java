package commands;

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
		super(null, true, true, CommandCategory.ADMIN, new String[]{"stats"}, 
			  "Shows o!alt tracker stats.", 
			  "Shows a variety of stats linked to this bot.", 
			  new String[]{"stats", "Shows the stats page."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		EmbedBuilder builder = new EmbedBuilder();
		ApplicationStats stats = ApplicationStats.getInstance();
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);
		builder.setAuthor("o!alt tracker stats page", Constants.SUPPORT_SERVER_LINK,
						  p_event.getJDA().getSelfUser().getAvatarUrl());
		builder.setDescription("**__" + TimeUtils.toDuration(stats.getUptime(), false) + "__** uptime");
		
		builder.addField("Startup Time", TimeUtils.toDuration(stats.getStartupTime(), true), true);
		
		builder.addField("osu! api/html status", (stats.isOsuApiStalled() ? "Paused" : "Running") +
												 " / " +
												 (stats.isOsuHtmlStalled() ? "Paused" : "Running"), true);
		builder.addField("osu! api/html sent last minute", GeneralUtils.df(Constants.OSU_API_REQUESTS_PER_MINUTE * stats.getOsuApiLoad(), 2) + " / " + 
														   GeneralUtils.df(Constants.OSU_HTML_REQUESTS_PER_MINUTE * stats.getOsuHtmlLoad(), 2), true);
		builder.addField("o!api requests sent/failed", 
						 stats.getOsuApiRequestsSent() + " / " + stats.getOsuApiRequestsFailed(), true);
		builder.addField("o!html requests sent/failed", 
						 stats.getOsuHtmlRequestsSent() + " / " + stats.getOsuHtmlRequestsFailed(), true);
		
		builder.addField("Total registered osu! users", String.valueOf(osuTrackManager.getLoadedRegisteredUsers()), false);
		
		for(int i = 0; i < Constants.OSU_ACTIVITY_CYCLES.length; ++i) {
			OsuRefreshRunnable cycleRunnable = osuTrackManager.getRefreshRunnable(i);
			String fieldText = "Not running";
			
			if(cycleRunnable != null) {
				fieldText = cycleRunnable.getUsersLeft() + " / ";
				fieldText += cycleRunnable.getInitialUserListSize() + " / ";
				fieldText += TimeUtils.toDuration(cycleRunnable.getAverageUserRefreshDelay(), true) + " / ";
				fieldText += TimeUtils.toDuration(cycleRunnable.getTimeElapsed(), false) + " / ";
				
				
				long expectedTimeLeft = cycleRunnable.getExpectedTimeUntilStop();
				String expectedTimeLeftSign = "";
				if(expectedTimeLeft < 0) { 
					expectedTimeLeft = -expectedTimeLeft;
					expectedTimeLeftSign = "-";
				}
				
				fieldText += expectedTimeLeftSign + TimeUtils.toDuration(expectedTimeLeft, false) + " / ";
				fieldText += TimeUtils.toDuration(cycleRunnable.getTimeUntilStop(), false);
			}
			
			builder.addField("Cycle " + i + " users left/total/avg delay/elapsed/expected time left/calc'd time left", fieldText, false);
		}
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
