package commands;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
		boolean debug = false;
		
		if(p_args.length > 0 && p_args[p_args.length - 1].contentEquals("debug")) {
			debug = true;
			p_args = Arrays.asList(p_args).subList(0, p_args.length - 1).toArray(new String[]{});
		}
		
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
															(activityCycle == Constants.OSU_ACTIVITY_CYCLES.length - 1 ? 
															")" : " and <" + TimeUtils.toDuration(cutoff, false) + ")");
		}

		String username = OsuUtils.getOsuPlayerUsernameFromIdWithApi(userId, true);
		builder.setAuthor(username + " • " + cycleText, 
						  "https://osu.ppy.sh/users/" + user.getUserId(),
						  "https://a.ppy.sh/" + user.getUserId());
		
		String pastRefreshName = activityCycle > 0 || user.justMovedCycles() ? "activity check" : "refresh";
		String descriptionText = "Last " + pastRefreshName + " was **<t:" + (user.getLastRefreshTime().getTime() / 1000) + ":R>**";
		
		long timeUntilRefresh = refreshRunnable.getTimeUntilUserRefresh(user.getUserId());
		String refreshTimeAddedText = "";
		
		if(timeUntilRefresh == -1) {
			timeUntilRefresh = refreshRunnable.getExpectedTimeUntilStop();
			
			String cycleLengthText = activityCycle > 0 ? "(**" + TimeUtils.toDuration(refreshRunnable.getRefreshDelay(), false) + "** long)" :
														 "";
			refreshTimeAddedText = "during the next cycle " + cycleLengthText + " starting ";
		}
		
		String futureRefreshName = activityCycle == 0 ? "Refreshing " : "Checking activity ";
		descriptionText += "\n" + futureRefreshName + refreshTimeAddedText + "**<t:" + ((System.currentTimeMillis() + timeUntilRefresh) / 1000) + ":R>**";
		
		if(user.getActivityCycle() > 0) {
			String storedUserId = OsuUtils.getOsuPlayerIdFromDiscordUserId(p_event.getAuthor().getId(), true);
			
			if(storedUserId.contentEquals(userId))
				descriptionText += "\nUse **`" + Constants.DEFAULT_PREFIX + "osutrack`** to manually enter the live tracking cycle";
			else 
				descriptionText += "\nUse **`" + Constants.DEFAULT_PREFIX + "osutrack " + username + 
								   "`** to manually enter them into the live tracking cycle";
		}

		builder.setDescription(descriptionText);
		
		String scoresText = "No plays found in the last " + TimeUtils.toDuration(TimeUnit.DAYS.toMillis(Constants.OSU_PLAY_PRUNE_DELAY), false) + "!";
		List<OsuPlay> latestFetchedScores = user.getCachedLatestPlays(Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT);
		
		if(!latestFetchedScores.isEmpty()) {
			scoresText = "";
			
			for(OsuPlay play : latestFetchedScores) {
				String currentScoreText = "";
				
				currentScoreText += "\n[" + play.getTitle() + "](https://osu.ppy.sh/beatmaps/" + play.getBeatmapId() + ")";
				currentScoreText += " " + Mods.getModDisplay(Mods.getModsFromBit(play.getEnabledMods()));
				currentScoreText += "\n" + (play.isUploaded() ? ":ballot_box_with_check:" : ":x:");
				currentScoreText += " | **<t:" + (play.getDatePlayed().getTime() / 1000) + ":R>**";
				currentScoreText += " | " + GeneralUtils.toFormattedNumber(play.getScore());
				currentScoreText += " | " + (play.getAccuracy() == 1.0 ? "" : GeneralUtils.toFormattedNumber(play.getAccuracy() * 100) + "% ");
				currentScoreText += (play.isPerfect() ? (play.getAccuracy() == 1.0 ? "SS" : "FC") : play.getCombo() + "x");
				currentScoreText += " " + play.getRank();
				
				if(play.getPP() > 0.0) 
					currentScoreText += " | " + GeneralUtils.toFormattedNumber(play.getPP()) + "pp";
				
				if(debug)
					currentScoreText += " | Update Status: " + play.getUpdateStatus();
				
				if(scoresText.length() + currentScoreText.length() > 1000) break;
				
				scoresText += currentScoreText;
			}
			
			scoresText = scoresText.substring(1);
		}
		
		builder.addField("Latest fetched scores", scoresText, false);
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
