package commands;

import data.CommandCategory;
import managers.ApplicationStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.Constants;
import utils.DiscordChatUtils;
import utils.TimeUtils;

public class StatsCommand extends Command {

	public StatsCommand() {
		super(null, true, true, CommandCategory.ADMIN, new String[]{"stats"}, 
			  "Shows o!alt tracker stats.", 
			  "Shows a variety of stats linked to this bot.", 
			  new String[]{"stats", "Shows the stats page."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] args) {
		EmbedBuilder builder = new EmbedBuilder();
		ApplicationStats stats = ApplicationStats.getInstance();
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);
		builder.setAuthor("o!alt tracker stats page", Constants.SUPPORT_SERVER_LINK,
						  p_event.getJDA().getSelfUser().getAvatarUrl());
		builder.setDescription("**__" + TimeUtils.toDuration(stats.getUptime(), false) + "__** uptime");
		
		builder.addField("Startup Time", TimeUtils.toDuration(stats.getStartupTime(), true), false);
		builder.addField("osu! api/html status", (stats.isOsuApiStalled() ? "Paused" : "Running") +
												 " / " +
												 (stats.isOsuHtmlStalled() ? "Paused" : "Running"), false);
		builder.addField("osu! api/html loads", stats.getOsuApiLoad() + "/" + stats.getOsuHtmlLoad(), false);
		builder.addField("o!api requests sent/failed", 
						 stats.getOsuApiRequestsSent() + "/" + stats.getOsuApiRequestsFailed(), false);
		builder.addField("o!html requests sent/failed", 
						 stats.getOsuHtmlRequestsSent() + "/" + stats.getOsuHtmlRequestsFailed(), false);
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
