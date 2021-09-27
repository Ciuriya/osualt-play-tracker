package commands;

import data.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import osu.tracking.OsuRefreshRunnable;
import osu.tracking.OsuTrackedUser;
import osu.tracking.OsuTrackingManager;
import utils.Constants;
import utils.DiscordChatUtils;
import utils.OsuUtils;
import utils.TimeUtils;

public class OsuStatusCommand extends Command {

	public OsuStatusCommand() {
		super(null, false, true, CommandCategory.OSU, new String[]{"osustatus", "status"}, 
			  "Shows tracking status for the registered osu! player.", 
			  "Shows tracking information for the registered osu! player.",
			  new String[]{"osustatus", "Shows the tracking info for the osu! player linked to the discord user using this command"},
			  new String[]{"osustatus <osu! name>", "Shows the tracking info for the given osu! player" +
													"Example: **`{prefix}osustatus nathan on osu`**"});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		String userId = "";
		
		if(p_args.length > 0) {
			String username = "";
			
			for(int i = 0; i < p_args.length; ++i) {
				username += " " + p_args[i];
			}
			
			userId = OsuUtils.getOsuPlayerIdFromUsernameWithSql(username.substring(1));
		} else {
			userId = OsuUtils.getOsuPlayerIdFromDiscordUserId(p_event.getAuthor().getId());
			
			if(userId.contentEquals("")) {
				DiscordChatUtils.message(p_event.getChannel(), "No osu! player is linked to this discord user!\n" + 
															   "Please use **`osuset your username here`** to link an osu! player or " + 
															   "simply add a username to this command like so: **`osustatus your username here`**");
				return;
			}
		}
		
		if(userId.contentEquals("")) {
			DiscordChatUtils.message(p_event.getChannel(), "This osu! player isn't registered!");
			return;
		}
		
		EmbedBuilder builder = new EmbedBuilder();
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		OsuTrackedUser user = osuTrackManager.getUser(userId);
		
		if(user == null) {
			DiscordChatUtils.message(p_event.getChannel(), "This osu! player isn't registered!");
			return;
		}
		
		OsuRefreshRunnable refreshRunnable = osuTrackManager.getRefreshRunnable(user.getActivityCycle());
		
		if(refreshRunnable == null) {
			DiscordChatUtils.message(p_event.getChannel(), "Cycle " + user.getActivityCycle() + " not found, please try again later!");
			return;
		}
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);
		
		String cycleText = "";
		
		if(user.getActivityCycle() == 0) {
			cycleText = "Live Tracking";
		} else {
			int activityCycle = user.getActivityCycle();
			long previousCutoff = Constants.OSU_ACTIVITY_CYCLES[activityCycle - 1][0] * 1000;
			long cutoff = Constants.OSU_ACTIVITY_CYCLES[activityCycle][0] * 1000;
			
			cycleText = "Activity Cycle " + activityCycle + " (Inactivity >" + TimeUtils.toDuration(previousCutoff, false) + 
															" and <" + TimeUtils.toDuration(cutoff, false) + ")";
		}
		
		builder.setAuthor(OsuUtils.getOsuPlayerUsernameFromIdWithApi(userId, true) + " � " + cycleText, 
						  "https://osu.ppy.sh/users/" + user.getUserId(),
						  "https://a.ppy.sh/" + user.getUserId());
		
		String descriptionText = "Last refresh was **" + TimeUtils.toDuration(System.currentTimeMillis() - user.getLastUpdateTime().getTime(), false) + "** ago";
		
		long timeUntilRefresh = refreshRunnable.getTimeUntilUserRefresh(user.getUserId());
		String refreshTimeAddedText = "";
		
		if(timeUntilRefresh == -1) {
			timeUntilRefresh = refreshRunnable.getExpectedTimeUntilStop();
			refreshTimeAddedText = "during the next cycle starting in ";
		} else {
			refreshTimeAddedText = "in ";
		}
		
		descriptionText += "\nRefreshing " + refreshTimeAddedText + "**" + TimeUtils.toDuration(timeUntilRefresh, false) + "**";
		builder.setDescription(descriptionText);
		
		builder.addField("Scores last uploaded on", TimeUtils.toDate(user.getLastUploadedTime().getTime()) + " UTC", false);
		builder.addField("Latest fetched scores", "Tracking not currently available, check again later!", false);
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
