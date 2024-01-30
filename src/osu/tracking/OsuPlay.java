package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import data.Database;
import data.Log;
import managers.DatabaseManager;
import utils.Constants;
import utils.GeneralUtils;
import utils.TimeUtils;

public class OsuPlay {

	private long m_scoreId;
	private String m_userId;
	private long m_beatmapId;
	private long m_score;
	private int m_count300;
	private int m_count100;
	private int m_count50;
	private int m_countMiss;
	private int m_combo;
	private boolean m_perfect;
	private String m_mods;
	private Timestamp m_datePlayed;
	private String m_rank;
	private double m_pp;
	private boolean m_replayAvailable;
	private boolean m_uploaded;
	private Timestamp m_insertionDate;
	private String m_title;
	private double m_accuracy;
	private int m_updateStatus = 0;
	
	private boolean m_canUploadRankedStatus;
	private long m_bestId;
	
	public OsuPlay(JSONObject p_jsonPlay) {
		loadFromJson(p_jsonPlay);
	}
	
	public OsuPlay(ResultSet p_resultSet) throws SQLException {
		loadFromSql(p_resultSet);
	}
	
	private void loadFromJson(JSONObject p_jsonPlay) {
		try {
			m_bestId = p_jsonPlay.optLong("best_id", 0);
			
			JSONObject statisticsObject = p_jsonPlay.getJSONObject("statistics");
			JSONObject beatmapObject = p_jsonPlay.optJSONObject("beatmap");
			JSONObject beatmapSetObject = p_jsonPlay.optJSONObject("beatmapset");
			
			m_scoreId = p_jsonPlay.optLong("id", 0);
			m_userId = String.valueOf(p_jsonPlay.optLong("user_id", 0));
			m_beatmapId = beatmapObject != null ? beatmapObject.optInt("id", 0) : 0;
			m_score = p_jsonPlay.optLong("score", 0);
			m_count300 = statisticsObject.optInt("count_300", 0);
			m_count100 = statisticsObject.optInt("count_100", 0);
			m_count50 = statisticsObject.optInt("count_50", 0);
			m_countMiss = statisticsObject.optInt("count_miss", 0);
			m_combo = p_jsonPlay.optInt("max_combo", 0);
			m_perfect = p_jsonPlay.optBoolean("perfect", false);
			
			JSONArray modArray = p_jsonPlay.optJSONArray("mods");
			
			if (modArray == null) m_mods = "[]";
			else m_mods = modArray.toString();
			
			// 2007-01-01T12:34:56+00:00 could be + or -
			String datePlayedString = p_jsonPlay.optString("created_at", "2007-01-01T00:00:00+00:00").replace("T", " ");
			
			if(!datePlayedString.endsWith("Z")) {
				boolean positiveTimezone = datePlayedString.contains("+");
				String[] timezoneSplit = datePlayedString.split(positiveTimezone ? "\\+" : "-");
				long timezoneOffset = TimeUtils.timezoneOffsetToTime(timezoneSplit[positiveTimezone ? 1 : timezoneSplit.length - 1]);
				
				timezoneOffset *= positiveTimezone ? 1 : -1;
				datePlayedString = datePlayedString.substring(0, datePlayedString.lastIndexOf(positiveTimezone ? "+" : "-"));

				m_datePlayed = new Timestamp(TimeUtils.toTime(datePlayedString) - timezoneOffset);
			} else m_datePlayed = new Timestamp(TimeUtils.toTime(datePlayedString.replace("Z", "")));

			m_rank = p_jsonPlay.optString("rank", "F");
			m_pp = p_jsonPlay.optDouble("pp", 0);
			m_replayAvailable = p_jsonPlay.optBoolean("replay", false);
			m_title = beatmapSetObject != null ? (beatmapSetObject.optString("artist", "") + " - " + beatmapSetObject.optString("title", "") + " [" + beatmapObject.optString("version", "") + "]") : "";
			m_accuracy = p_jsonPlay.optDouble("accuracy", 1);
			
			if(m_title.length() > 255) m_title = m_title.substring(0, 255);
			
			String status = beatmapObject != null ? beatmapObject.optString("status", "") : "";
			m_canUploadRankedStatus = status.equalsIgnoreCase("ranked") || status.equalsIgnoreCase("loved") || status.equalsIgnoreCase("approved");
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to load play from json", e);
		}
	}
	
	private void loadFromSql(ResultSet p_resultSet) throws SQLException {
		m_scoreId = p_resultSet.getLong(1);
		m_userId = String.valueOf(p_resultSet.getInt(2));
		m_beatmapId = p_resultSet.getInt(3);
		m_score = p_resultSet.getInt(4);
		m_count300 = p_resultSet.getInt(5);
		m_count100 = p_resultSet.getInt(6);
		m_count50 = p_resultSet.getInt(7);
		m_countMiss = p_resultSet.getInt(8);
		m_combo = p_resultSet.getInt(9);
		m_perfect = p_resultSet.getBoolean(10);
		m_mods = p_resultSet.getString(11);
		m_datePlayed = p_resultSet.getTimestamp(12);
		m_rank = p_resultSet.getString(13);
		m_pp = p_resultSet.getDouble(14);
		m_replayAvailable = p_resultSet.getBoolean(15);
		m_uploaded = p_resultSet.getBoolean(16);
		m_insertionDate = p_resultSet.getTimestamp(17);
		m_title = p_resultSet.getString(18);
		m_accuracy = p_resultSet.getDouble(19);
		m_updateStatus = p_resultSet.getInt(20);
	}
	
	public static List<OsuPlay> getPlaysToUpload() {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		List<OsuPlay> fetchedPlays = new ArrayList<>();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT * FROM `osu-play` WHERE `uploaded`=0");
			
			ResultSet rs = st.executeQuery();
	
			while(rs.next()) fetchedPlays.add(new OsuPlay(rs));
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not fetch latest non-uploaded plays", e);
		} finally {
			db.closeConnection(conn);
		}
		
		return fetchedPlays;
	}
	
	public static void saveToDatabase(List<OsuPlay> p_plays) {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT IGNORE INTO `osu-play` " +
								   "(`score_id`, `user_id`, `beatmap_id`, `score`, `count300`, `count100`, `count50`, `countmiss`, " + 
								   "`combo`, `perfect`, `mods`, `date_played`, `rank`, `pp`, `replay_available`, `uploaded`, " + 
								   "`insertion-date`, `title`, `accuracy`, `upload_status`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
			for(OsuPlay play : p_plays) {
				st.setLong(1, play.m_scoreId);
				st.setInt(2, GeneralUtils.stringToInt(play.m_userId));
				st.setLong(3, play.m_beatmapId);
				st.setLong(4, play.m_score);
				st.setInt(5, play.m_count300);
				st.setInt(6, play.m_count100);
				st.setInt(7, play.m_count50);
				st.setInt(8, play.m_countMiss);
				st.setInt(9, play.m_combo);
				st.setBoolean(10, play.m_perfect);
				st.setString(11, play.m_mods);
				st.setTimestamp(12, play.m_datePlayed, calendar);
				st.setString(13, play.m_rank);
				st.setDouble(14, play.m_pp);
				st.setBoolean(15, play.m_replayAvailable);
				st.setBoolean(16, play.m_uploaded);

				play.m_insertionDate = new Timestamp(calendar.getTime().getTime());
				
				st.setTimestamp(17, play.m_insertionDate);
				st.setString(18, play.m_title);
				st.setDouble(19, play.m_accuracy);
				st.setInt(20, play.m_updateStatus);
				
				st.addBatch();
			}
			
			st.executeBatch();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to insert " + p_plays.size() + " plays", e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public static void setUploaded(List<OsuPlay> p_plays, int[] p_updateStatusList) {	
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "UPDATE `osu-play` SET `uploaded`=?, `upload_status`=? WHERE `score_id`=? AND `score`=?");
			
			for(int i = 0; i < p_plays.size(); ++i) {
				OsuPlay play = p_plays.get(i);
				
				st.setBoolean(1, true);
				st.setInt(2, p_updateStatusList[i]);
				st.setLong(3, play.m_scoreId);
				st.setLong(4, play.m_score);
			
				st.addBatch();
				
				play.setUploaded(true);
				play.setUpdateStatus(p_updateStatusList[i]);
			}
			
			st.executeBatch();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to update the uploaded status of " + p_plays.size() + " plays", e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public static void deletePlays(List<OsuPlay> p_plays) {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "DELETE FROM `osu-play` WHERE `score_id`=?");
			
			for(OsuPlay play : p_plays) {
				st.setLong(1, play.getScoreId());
				
				st.addBatch();
			}
			
			st.executeBatch();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to delete " + p_plays.size() + " plays", e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public static void pruneOldPlays() {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
			PreparedStatement st = conn.prepareStatement(
								   "DELETE FROM `osu-play` WHERE `insertion-date` < " + 
								   "DATE_SUB('" + TimeUtils.toDate(calendar.getTime().getTime()) +
								   "', INTERVAL " + Constants.OSU_PLAY_PRUNE_DELAY + " DAY)");

			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to prune old plays", e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public long getScoreId() {
		return m_scoreId;
	}
	
	public String getUserId() {
		return m_userId;
	}
	
	public long getBeatmapId() {
		return m_beatmapId;
	}
	
	public long getScore() {
		return m_score;
	}
	
	public int getCount300() {
		return m_count300;
	}
	
	public int getCount100() {
		return m_count100;
	}
	
	public int getCount50() {
		return m_count50;
	}
	
	public int getCountMiss() {
		return m_countMiss;
	}
	
	public int getCombo() {
		return m_combo;
	}
	
	public boolean isPerfect() {
		return m_perfect;
	}
	
	public String getMods() {
		return m_mods;
	}
	
	public Timestamp getDatePlayed() {
		return m_datePlayed;
	}
	
	public String getRank() {
		return m_rank;
	}
	
	public double getPP() {
		return m_pp;
	}
	
	public boolean isReplayAvailable() {
		return m_replayAvailable;
	}
	
	public boolean isUploaded() {
		return m_uploaded;
	}
	
	public Timestamp getInsertionDate() {
		return m_insertionDate;
	}
	
	public String getTitle() {
		return m_title;
	}
	
	public double getAccuracy() {
		return m_accuracy;
	}
	
	public boolean canUploadRankedStatus() {
		return m_canUploadRankedStatus;
	}
	
	public long getBestId() {
		return m_bestId;
	}
	
	public int getUpdateStatus() {
		return m_updateStatus;
	}
	
	public void setUploaded(boolean p_uploaded) {
		m_uploaded = p_uploaded;
	}
	
	public void setUpdateStatus(int p_uploadStatus) {
		m_updateStatus = p_uploadStatus;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof OsuPlay)) return false;
		
		OsuPlay other = (OsuPlay) o;
		
		return this.getUserId().equals(other.getUserId()) && this.getScore() == other.getScore() && this.getBeatmapId() == other.getBeatmapId() && 
			   this.getMods().contentEquals(other.getMods()) && this.getAccuracy() == other.getAccuracy() && this.getDatePlayed().getTime() == this.getDatePlayed().getTime();
	}
}
