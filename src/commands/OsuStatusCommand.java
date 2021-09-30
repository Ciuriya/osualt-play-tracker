package commands;

import java.util.List;

import data.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import osu.api.Mods;
import osu.tracking.OsuPlay;
import osu.tracking.OsuRefreshRunnable;
import osu.tracking.OsuTrackedUser;
import osu.tracking.OsuTrackingManager;
import utils.Constants;
import utils.DiscordChatUtils;
import utils.GeneralUtils;
import utils.OsuUtils;
import utils.TimeUtils;

public class OsuStatusCommand extends Command {

	public OsuStatusCommand() {
		super(null, false, true, CommandCategory.OSU, new String[]{"osustatus", "status"}, 
			  "Shows tracking status for the registered osu! player.", 
			  "Shows tracking information for the registered osu! player.",
			  new String[]{"osustatus", "Shows the tracking info for the osu! player linked to the discord user using this command."},
			  new String[]{"osustatus <osu! name>", "Shows the tracking info for the given osu! player.\n" +
													"Example: **`{prefix}osustatus nathan on osu`**"});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		String userId = getUserIdFromArgsSimple(p_event, p_args);
		
		if(userId.isEmpty()) return;
		
		EmbedBuilder builder = new EmbedBuilder();
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		OsuTrackedUser user = osuTrackManager.getUser(userId);
		
		if(user == null) {
			DiscordChatUtils.message(p_event.getChannel(), "This osu! player isn't registered!");
			return;
		}
		
		int activityCycle = user.getActivityCycle();
		OsuRefreshRunnable refreshRunnable = osuTrackManager.getRefreshRunnable(activityCycle);
		
		if(refreshRunnable == null) {
			DiscordChatUtils.message(p_event.getChannel(), "Cycle " + activityCycle + " not found, please try again later!");
			return;
		}
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);
		
		String cycleText = "";
		
		if(activityCycle == 0) {
			cycleText = "Live Tracking";
		} else {
			long previousCutoff = Constants.OSU_ACTIVITY_CYCLES[activityCycle - 1][0] * 1000;
			long cutoff = Constants.OSU_ACTIVITY_CYCLES[activityCycle][0] * 1000;
			
			cycleText = "Activity Cycle " + activityCycle + " (Inactivity >" + TimeUtils.toDuration(previousCutoff, false) + 
															" and <" + TimeUtils.toDuration(cutoff, false) + ")";
		}

		String username = OsuUtils.getOsuPlayerUsernameFromIdWithApi(userId, true);
		builder.setAuthor(username + " • " + cycleText, 
						  "https://osu.ppy.sh/users/" + user.getUserId(),
						  "https://a.ppy.sh/" + user.getUserId());
		
		String pastRefreshName = activityCycle > 0 || user.justMovedCycles() ? "activity check" : "refresh";
		String descriptionText = "Last " + pastRefreshName + " was **" + 
								 TimeUtils.toDuration(System.currentTimeMillis() - user.getLastRefreshTime().getTime(), false) + "** ago";
		
		long timeUntilRefresh = refreshRunnable.getTimeUntilUserRefresh(user.getUserId());
		String refreshTimeAddedText = "";
		
		if(timeUntilRefresh == -1) {
			timeUntilRefresh = refreshRunnable.getExpectedTimeUntilStop();
			
			String cycleLengthText = activityCycle > 0 ? "(**" + TimeUtils.toDuration(refreshRunnable.getRefreshDelay(), false) + "** long)" :
														 "";
			refreshTimeAddedText = "during the next cycle " + cycleLengthText + " starting in ";
		} else {
			refreshTimeAddedText = "in ";
		}
		
		String futureRefreshName = activityCycle == 0 ? "Refreshing " : "Checking activity ";
		descriptionText += "\n" + futureRefreshName + refreshTimeAddedText + "**" + TimeUtils.toDuration(timeUntilRefresh, false) + "**";
		
		if(user.getActivityCycle() > 0) {
			String storedUserId = OsuUtils.getOsuPlayerIdFromDiscordUserId(p_event.getAuthor().getId(), true);
			
			if(storedUserId.contentEquals(userId))
				descriptionText += "\nUse **`" + Constants.DEFAULT_PREFIX + "osutrack`** to manually enter the live tracking cycle";
			else 
				descriptionText += "\nUse **`" + Constants.DEFAULT_PREFIX + "osutrack " + username + 
								   "`** to manually enter them into the live tracking cycle";
		}

		builder.setDescription(descriptionText);
		
		String scoresText = "No plays found!";
		List<OsuPlay> latestFetchedScores = user.getCachedLatestPlays(Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT);
		
		if(!latestFetchedScores.isEmpty()) {
			scoresText = "";
			
			for(OsuPlay play : latestFetchedScores) {
				scoresText += "\n[" + play.getTitle() + "](https://osu.ppy.sh/beatmaps/" + play.getBeatmapId() + ")";
				scoresText += " " + Mods.getModDisplay(Mods.getModsFromBit(play.getEnabledMods()));
				scoresText += "\n" + (play.isUploaded() ? ":white_check_mark:" : ":x:");
				scoresText += " | " + TimeUtils.toDuration(System.currentTimeMillis() - play.getDatePlayed().getTime(), false) + " ago";
				scoresText += " | " + GeneralUtils.toFormattedNumber(play.getScore());
				scoresText += " | " + (play.getAccuracy() == 1.0 ? "" : GeneralUtils.toFormattedNumber(play.getAccuracy() * 100) + "% ");
				scoresText += (play.isPerfect() ? (play.getAccuracy() == 1.0 ? "SS" : "FC") : play.getCombo() + "x");
				scoresText += " " + play.getRank();
				
				if(play.getPP() > 0.0) 
					scoresText += " | " + GeneralUtils.toFormattedNumber(play.getPP()) + "pp";
			}
			
			scoresText = scoresText.substring(1);
		}
		
		builder.addField("Latest fetched scores", scoresText, false);
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
