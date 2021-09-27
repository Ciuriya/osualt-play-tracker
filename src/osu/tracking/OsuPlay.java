package osu.tracking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.json.JSONObject;

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
	private long m_enabledMods;
	private Timestamp m_datePlayed;
	private String m_rank;
	private double m_pp;
	private boolean m_replayAvailable;
	private boolean m_uploaded;
	
	public OsuPlay(JSONObject p_jsonPlay) {
		loadFromJson(p_jsonPlay);
	}
	
	public OsuPlay(ResultSet p_resultSet) throws SQLException {
		loadFromSql(p_resultSet);
	}
	
	private void loadFromJson(JSONObject p_jsonPlay) {
		m_scoreId = p_jsonPlay.optLong("score_id", 0);
		m_userId = String.valueOf(p_jsonPlay.optLong("user_id", 0));
		m_beatmapId = p_jsonPlay.optLong("beatmap_id", 0);
		m_score = p_jsonPlay.optLong("score", 0);
		m_count300 = p_jsonPlay.optInt("count300", 0);
		m_count100 = p_jsonPlay.optInt("count100", 0);
		m_count50 = p_jsonPlay.optInt("count50", 0);
		m_countMiss = p_jsonPlay.optInt("countmiss", 0);
		m_combo = p_jsonPlay.optInt("combo", 0);
		m_perfect = p_jsonPlay.optBoolean("perfect", false);
		m_enabledMods = p_jsonPlay.optLong("enabled_mods", 0);
		m_datePlayed = new Timestamp(TimeUtils.toTime(p_jsonPlay.optString("date", "2007-01-01 00:00:00")));
		m_rank = p_jsonPlay.optString("rank", "F");
		m_pp = 0;
		m_replayAvailable = false;
	}
	
	private void loadFromSql(ResultSet p_resultSet) throws SQLException {
		
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
	
	public long getEnabledMods() {
		return m_enabledMods;
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
}
