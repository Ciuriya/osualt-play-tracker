package commands;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import data.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
		
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		OsuTrackedUser user = osuTrackManager.getUser(userId);
		
		if(user == null) {
			DiscordChatUtils.message(p_event.getChannel(), "This osu! player isn't registered! :skull: http://smcmax.com/s/readinfo.gif");
			return;
		}
		
		int activityCycle = user.getActivityCycle();
		OsuRefreshRunnable refreshRunnable = osuTrackManager.getRefreshRunnable(activityCycle);
		
		if(refreshRunnable == null) {
			DiscordChatUtils.message(p_event.getChannel(), "Cycle " + activityCycle + " not found, please try again later!");
			return;
		}
		
		String username = OsuUtils.getOsuPlayerUsernameFromIdWithApi(userId, true);
		String authorId = p_event.getAuthor().getId();
		boolean finalDebug = debug;
		
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		Timestamp currentTime = new Timestamp(calendar.getTime().getTime());
		long expectedRefreshTime = refreshRunnable.getTimeUntilUserRefresh(user.getUserId());
		
		if(expectedRefreshTime == -1)
			expectedRefreshTime = refreshRunnable.getExpectedTimeUntilStop();
	
		final long refreshTime = expectedRefreshTime + System.currentTimeMillis() + 1000;
		
		user.setLastStatusMessageTime(currentTime);
		
		DiscordChatUtils.embedWithFuture(p_event.getChannel(), buildEmbed(userId, username, authorId, debug, true))
						.thenAccept((message) -> { updateEmbedAfterRefresh(message, currentTime, currentTime, refreshTime, user, username, activityCycle, authorId, finalDebug); });
	}
	
	private void updateEmbedAfterRefresh(Message p_message, Timestamp p_originalSendTime, Timestamp p_sendTimeInUse, long nextRefreshTime, OsuTrackedUser p_user, String p_username, int p_activityCycle, String p_authorId, boolean p_debug) {
		long waitCurrentTime = System.currentTimeMillis();
		Calendar waitCalendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		Timestamp waitCurrentTimestamp = new Timestamp(waitCalendar.getTime().getTime());
		
		if(nextRefreshTime > waitCurrentTime) {
			new Timer().schedule(new TimerTask() {
				public void run() {
					if(p_user.getLastRefreshTime().before(waitCurrentTimestamp)) {
						updateEmbedAfterRefresh(p_message, p_originalSendTime, p_sendTimeInUse, System.currentTimeMillis() + 5000, p_user, p_username, p_activityCycle, p_authorId, p_debug);
						return;
					}
					
					long currentTimeMillis = System.currentTimeMillis();
					long minUpdateTime = p_originalSendTime.getTime() + Constants.OSU_STATUS_MESSAGE_UPDATE_MIN_TIME * 1000;
					long maxUpdateTime = p_originalSendTime.getTime() + Constants.OSU_STATUS_MESSAGE_UPDATE_MAX_TIME * 1000;
					boolean willRefresh = p_user.getLastStatusMessageTime() == p_sendTimeInUse && (p_message.getChannel().getHistoryAfter(p_message, 26).complete().size() < 25 && maxUpdateTime > currentTimeMillis) || minUpdateTime > currentTimeMillis;
					
					int activityCycle = p_user.getActivityCycle();
					OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
					OsuRefreshRunnable refreshRunnable = osuTrackManager.getRefreshRunnable(activityCycle);		
					Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
					Timestamp currentTime = new Timestamp(calendar.getTime().getTime());
					long expectedRefreshTime = refreshRunnable.getTimeUntilUserRefresh(p_user.getUserId());
					
					if(expectedRefreshTime == -1) expectedRefreshTime = refreshRunnable.getExpectedTimeUntilStop();
				
					expectedRefreshTime += System.currentTimeMillis() + 1000;
					
					MessageEmbed embed = buildEmbed(p_user.getUserId(), p_username, p_authorId, p_debug, willRefresh);
					p_message.editMessageEmbeds(embed).queue();
					p_user.setLastStatusMessageTime(currentTime);
					
					if(willRefresh) updateEmbedAfterRefresh(p_message, p_originalSendTime, currentTime, expectedRefreshTime, p_user, p_username, activityCycle, p_authorId, p_debug);
				}
			}, nextRefreshTime - waitCurrentTime);
		}
	}
	
	private MessageEmbed buildEmbed(String p_userId, String p_username, String p_authorId, boolean p_debug, boolean p_willRefresh) {
		EmbedBuilder builder = new EmbedBuilder();
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		OsuTrackedUser user = osuTrackManager.getUser(p_userId);
		
		int activityCycle = user.getActivityCycle();
		OsuRefreshRunnable refreshRunnable = osuTrackManager.getRefreshRunnable(activityCycle);
		
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

		builder.setAuthor(p_username + " • " + cycleText, 
						  "https://osu.ppy.sh/users/" + user.getUserId(),
						  "https://a.ppy.sh/" + user.getUserId());
		
		String pastRefreshName = activityCycle > 0 || user.justMovedCycles() ? "activity check" : "refresh";
		String descriptionText = "Last " + pastRefreshName + " was **<t:" + (user.getLastRefreshTime().getTime() / 1000) + ":R>**";
		
		long timeUntilRefresh = refreshRunnable.getTimeUntilUserRefresh(user.getUserId());
		
		if(timeUntilRefresh == -1 && activityCycle > 0) {
			timeUntilRefresh = refreshRunnable.getExpectedTimeUntilStop();
			long maxTimeUntilNextRefresh = timeUntilRefresh + refreshRunnable.getRefreshDelay();
			
			String timeUntilRefreshTimestamp = "**<t:" + ((System.currentTimeMillis() + timeUntilRefresh) / 1000) + ":t>**";
			String maxTimeUntilNextRefreshTimestamp = "**<t:" + ((System.currentTimeMillis() + maxTimeUntilNextRefresh) / 1000) + ":t>**";
			
			descriptionText += "\nNext activity check ";
			
			if((maxTimeUntilNextRefresh - timeUntilRefresh) / 1000 < 60) 
				descriptionText += "around " + timeUntilRefreshTimestamp;
			else
				descriptionText += "between " + timeUntilRefreshTimestamp + " and " + maxTimeUntilNextRefreshTimestamp;
		} else if(activityCycle > 0){
			descriptionText += "\nRefreshing **<t:" + ((System.currentTimeMillis() + timeUntilRefresh) / 1000) + ":R>**";
		}

		if(activityCycle > 0) {
			String storedUserId = OsuUtils.getOsuPlayerIdFromDiscordUserId(p_authorId, true);
			
			if(storedUserId.contentEquals(p_userId))
				descriptionText += "\nUse **`" + Constants.DEFAULT_PREFIX + "track`** to manually enter the live tracking cycle";
			else 
				descriptionText += "\nUse **`" + Constants.DEFAULT_PREFIX + "track " + p_username + 
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
				currentScoreText += "\n" + (play.isUploaded() ? ":white_check_mark:" : "<a:loading:894822912408834089>");
				currentScoreText += " | **<t:" + (play.getDatePlayed().getTime() / 1000) + ":R>**";
				currentScoreText += " | " + GeneralUtils.toFormattedNumber(play.getScore());
				currentScoreText += " | " + (play.getAccuracy() == 1.0 ? "" : GeneralUtils.toFormattedNumber(play.getAccuracy() * 100) + "% ");
				currentScoreText += (play.isPerfect() ? (play.getAccuracy() == 1.0 ? " " : "FC ") : play.getCombo() + "x ");
				currentScoreText += "" + play.getRank();
				
				if(play.getPP() > 0.0) 
					currentScoreText += " | " + GeneralUtils.toFormattedNumber(play.getPP()) + "pp";
				
				if(p_debug)
					currentScoreText += " | Update Status: " + play.getUpdateStatus();
				
				if(scoresText.length() + currentScoreText.length() > 1000) break;
				
				scoresText += currentScoreText;
			}
			
			scoresText = scoresText.substring(1);
		}
		
		String selfUpdateMessage = " • Message will self-update after next refresh";
		builder.addField("Latest fetched scores" + (p_willRefresh ? selfUpdateMessage : ""), scoresText, false);
		
		return builder.build();
	}
}
