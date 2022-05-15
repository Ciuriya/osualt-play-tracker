package commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import data.CommandCategory;
import data.Database;
import data.Log;
import managers.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import osu.tracking.OsuPlay;
import osu.tracking.OsuTrackedUser;
import osu.tracking.OsuTrackingManager;
import utils.Constants;
import utils.DiscordChatUtils;
import utils.GeneralUtils;
import utils.OsuUtils;
import utils.TimeUtils;

public class OsuStatsCommand extends Command {

	public OsuStatsCommand() {
		super(null, false, true, CommandCategory.OSU, new String[]{"osustats", "stats"}, 
			  "Shows miscellaneous stats for the registered osu! player.", 
			  "Shows miscellaneous stats for the registered osu! player.",
			  new String[]{"osustats", "Shows miscellaneous stats for the osu! player linked to the discord user using this command."},
			  new String[]{"osustats <osu! name>", "Shows miscellaneous stats for the given osu! player.\n" +
													"Example: **`{prefix}osustats nathan on osu`**"});
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
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);

		String username = OsuUtils.getOsuPlayerUsernameFromIdWithApi(userId, true);
		builder.setAuthor(username, 
						  "https://osu.ppy.sh/users/" + user.getUserId(),
						  "https://a.ppy.sh/" + user.getUserId());
		
		String cachingLengthDisplay = TimeUtils.toDuration(TimeUnit.DAYS.toMillis(Constants.OSU_PLAY_PRUNE_DELAY), false);
		builder.setDescription("All values below **ONLY** account for new **HIGHEST SCORE** plays on maps played in the last " + cachingLengthDisplay + " and discards any other plays.");
		
		List<OsuPlay> allCachedPlays = getAllCachedPlays(userId);
		List<OsuPlay> lastDayCachedPlays = filterCachedPlaysInLastDurationTimeFrame(allCachedPlays, 24 * 60 * 60 * 1000);
		
		builder.addField("Saved scores for last 24h/" + cachingLengthDisplay, 
						 GeneralUtils.addCommasToNumber(lastDayCachedPlays.size()) + " / " + 
						 GeneralUtils.addCommasToNumber(allCachedPlays.size()), true);
		
		builder.addField("Play Ranks in last 24h", 
						 getUniqueRanksInPlays(lastDayCachedPlays), false);

		builder.addField("Play Ranks in last " + cachingLengthDisplay, 
		 		 		 getUniqueRanksInPlays(allCachedPlays), false);
		
		builder.addField("Raw ranked score in last 24h/" + cachingLengthDisplay,
						 GeneralUtils.formatLargeNumber(getTotalScoreInPlays(lastDayCachedPlays)) + " / " + 
						 GeneralUtils.formatLargeNumber(getTotalScoreInPlays(allCachedPlays)), true);
		
		builder.addField("Average Score per play in last 24h/" + cachingLengthDisplay, 
						 getAverageScorePerPlay(lastDayCachedPlays) + " / " + getAverageScorePerPlay(allCachedPlays), true);
		
		builder.addField("Average Accuracy in last 24h/" + cachingLengthDisplay, 
						 getAverageAccuracyInPlays(lastDayCachedPlays) + " / " + getAverageAccuracyInPlays(allCachedPlays), true);
		
		builder.addField("Average PP per play in last 24h/" + cachingLengthDisplay,
						 getAveragePPInPlays(lastDayCachedPlays) + " / " + getAveragePPInPlays(allCachedPlays), true);
		
		builder.addField("Highest pp in last 24h/" + cachingLengthDisplay, 
						 getMaxPPInPlays(lastDayCachedPlays) + " / " + getMaxPPInPlays(allCachedPlays), true);
		
		builder.addField("Highest combo in last 24h/" + cachingLengthDisplay, 
						 getMaxComboInPlays(lastDayCachedPlays) + " / " + getMaxComboInPlays(allCachedPlays), true);
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
	
	private List<OsuPlay> getAllCachedPlays(String p_userId) {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		Map<Long, OsuPlay> fetchedPlays = new HashMap<>();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT * FROM `osu-play` WHERE `user_id`=? ORDER BY `date_played`");
			
			st.setInt(1, GeneralUtils.stringToInt(p_userId));
			
			ResultSet rs = st.executeQuery();
	
			while(rs.next()) {
				long beatmapId = rs.getLong(3);
				long score = rs.getLong(4);
				int updateStatus = rs.getInt(20);

				if(updateStatus > 0 && (!fetchedPlays.containsKey(beatmapId) || fetchedPlays.get(beatmapId).getScore() < score))
					fetchedPlays.put(beatmapId, new OsuPlay(rs));
			}
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not fetch all cached plays from sql for user: " + p_userId, e);
		} finally {
			db.closeConnection(conn);
		}
		
		return new ArrayList<>(fetchedPlays.values());
	}
	
	private List<OsuPlay> filterCachedPlaysInLastDurationTimeFrame(List<OsuPlay> p_cachedPlays, long p_duration) {
		return p_cachedPlays.stream().filter(p -> p.getDatePlayed().after(new Timestamp(System.currentTimeMillis() - p_duration))).collect(Collectors.toList());
	}
	
	private long getTotalScoreInPlays(List<OsuPlay> p_plays) {
		long totalScore = 0;
		for(OsuPlay play : p_plays)
			totalScore += play.getScore();
		
		return totalScore;
	}
	
	private String getAverageScorePerPlay(List<OsuPlay> p_plays) {
		return GeneralUtils.formatLargeNumber((long) Math.floor((double) getTotalScoreInPlays(p_plays) / (double) p_plays.size()));
	}
	
	private String getUniqueRanksInPlays(List<OsuPlay> p_plays) {
		long xh = 0, x = 0, sh = 0, s = 0, a = 0, b = 0, c = 0, d = 0;
		
		for(OsuPlay play : p_plays) {
			switch(play.getRank().toLowerCase()) {
				case "xh": ++xh; break;
				case "x": ++x; break;
				case "sh": ++sh; break;
				case "s": ++s; break;
				case "a": ++a; break;
				case "b": ++b; break;
				case "c": ++c; break;
				case "d": ++d; break;
				default: break;
			}
		}
		
		String display = "";
		
		if(xh > 0) display += GeneralUtils.addCommasToNumber(xh) + " XH / ";
		if(x > 0) display += GeneralUtils.addCommasToNumber(x) + " X / ";
		if(sh > 0) display += GeneralUtils.addCommasToNumber(sh) + " SH / ";
		if(s > 0) display += GeneralUtils.addCommasToNumber(s) + " S / ";
		if(a > 0) display += GeneralUtils.addCommasToNumber(a) + " A / ";
		if(b > 0) display += GeneralUtils.addCommasToNumber(b) + " B / ";
		if(c > 0) display += GeneralUtils.addCommasToNumber(c) + " C / ";
		if(d > 0) display += GeneralUtils.addCommasToNumber(d) + " D / ";
		
		return display.isEmpty() ? "No plays" : display.substring(0, display.length() - 3);
	}
	
	private String getAveragePPInPlays(List<OsuPlay> p_plays) {
		long totalPP = 0;
		for(OsuPlay play : p_plays)
			totalPP += play.getPP();
		
		return totalPP == 0 ? "0pp" : GeneralUtils.toFormattedNumber(totalPP / (double) p_plays.size()) + "pp";
	}
	
	private String getAverageAccuracyInPlays(List<OsuPlay> p_plays) {
		double totalAccuracy = 0;
		for(OsuPlay play : p_plays)
			totalAccuracy += play.getAccuracy();
		
		totalAccuracy *= 100;
		
		return totalAccuracy == 0 ? "0%" : GeneralUtils.toFormattedNumber(totalAccuracy / (double) p_plays.size()) + "%";
	}
	
	private String getMaxComboInPlays(List<OsuPlay> p_plays) {
		long highestCombo = 0;
		for(OsuPlay play : p_plays)
			if(play.getCombo() > highestCombo)
				highestCombo = play.getCombo();
		
		return GeneralUtils.addCommasToNumber(highestCombo) + "x";
	}
	
	private String getMaxPPInPlays(List<OsuPlay> p_plays) {
		double highestPP = 0;
		for(OsuPlay play : p_plays)
			if(play.getPP() > highestPP)
				highestPP = play.getPP();
		
		return GeneralUtils.toFormattedNumber(highestPP) + "pp";
	}
}
