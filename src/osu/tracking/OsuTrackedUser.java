package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import data.Database;
import data.Log;
import managers.DatabaseManager;
import utils.Constants;
import utils.GeneralUtils;

public class OsuTrackedUser {
	
	private String m_userId;
	private int m_mode;
	private int m_playcount;
	private Timestamp m_lastActiveTime;
	private Timestamp m_lastUpdateTime;
	private Timestamp m_lastRefreshTime; // THIS IS RUNTIME-ONLY, NOT SAVED
	private Timestamp m_lastUploadedTime;
	private Timestamp m_lastStatusMessageTime;
	
	private int m_activityCycle = 0;
	
	private boolean m_isFetching;
	private boolean m_isDeleted;

	private List<OsuPlay> m_cachedLatestPlays = new ArrayList<>();

	public OsuTrackedUser(String p_userId, int p_mode, int p_playcount) {
		m_userId = p_userId;
		m_mode = p_mode;
		m_playcount = p_playcount;
		m_isFetching = false;
		m_isDeleted = false;
		
		setLastActiveTime();
		setLastUpdateTime();
		setLastRefreshTime();
		setLastUploadedTime();
		
		updateActivityCycle();
	}
	
	public OsuTrackedUser(ResultSet p_resultSet) throws SQLException {
		m_isFetching = false;
		m_isDeleted = false;
		
		loadFromSql(p_resultSet);
		updateActivityCycle();
	}
	
	private void loadFromSql(ResultSet p_resultSet) throws SQLException {
		m_userId = String.valueOf(p_resultSet.getInt(1));
		m_mode = p_resultSet.getInt(2);
		m_lastActiveTime = p_resultSet.getTimestamp(3);
		m_lastUpdateTime = p_resultSet.getTimestamp(4);
		m_lastUploadedTime = p_resultSet.getTimestamp(5);
		m_playcount = p_resultSet.getInt(6);
		
		setLastRefreshTime();
	}
	
	public String getUserId() {
		return m_userId;
	}
	
	public int getMode() {
		return m_mode;
	}
	
	public int getPlaycount() {
		return m_playcount;
	}
	
	public Timestamp getLastActiveTime() {
		return m_lastActiveTime;
	}
	
	public Timestamp getLastUpdateTime() {
		return m_lastUpdateTime;
	}
	
	public Timestamp getLastRefreshTime() {
		return m_lastRefreshTime;
	}
	
	public Timestamp getLastUploadedTime() {
		return m_lastUploadedTime;
	}
	
	public Timestamp getLastStatusMessageTime() {
		return m_lastStatusMessageTime;
	}
	
	public int getActivityCycle() {
		return m_activityCycle;
	}
	
	public boolean isFetching() {
		return m_isFetching;
	}
	
	public boolean isDeleted() {
		return m_isDeleted;
	}
	
	public List<OsuPlay> getCachedLatestPlays(int p_amount, boolean p_ignoreUninserted) {
		p_amount = Math.min(p_amount, Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT);
		
		if(p_amount <= m_cachedLatestPlays.size())
			return m_cachedLatestPlays.subList(0, p_amount);
		else {
			Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
			Connection conn = db.getConnection();
			List<OsuPlay> fetchedPlays = new ArrayList<>();
			
			try {
				PreparedStatement st = conn.prepareStatement(
									   "SELECT * FROM `osu-play` WHERE `user_id`=? " +
									   (p_ignoreUninserted ? "AND `uploaded`=1 AND `upload_status`>=1 " : "") +
									   "ORDER BY `date_played` DESC LIMIT " + 
									   Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT);
				
				st.setInt(1, GeneralUtils.stringToInt(m_userId));
				
				ResultSet rs = st.executeQuery();
		
				while(rs.next()) {
					long scoreId = rs.getLong(1);

					if(!m_cachedLatestPlays.stream().anyMatch(p -> p.getScoreId() == scoreId))
						fetchedPlays.add(new OsuPlay(rs));
				}
				
				rs.close();
				st.close();
			} catch(Exception e) {
				Log.log(Level.SEVERE, "Could not fetch latest plays from sql for user: " + m_userId, e);
			} finally {
				db.closeConnection(conn);
			}
			
			addPlaysToCache(fetchedPlays);
			
			return m_cachedLatestPlays;
		}
	}
	
	public void addPlayToCache(OsuPlay p_play) {
		addPlaysToCache(Collections.singletonList(p_play));
	}
	
	public void addPlaysToCache(List<OsuPlay> p_plays) {
		m_cachedLatestPlays.addAll(p_plays);
		m_cachedLatestPlays.sort(new Comparator<OsuPlay>() {
			@Override
			public int compare(OsuPlay o1, OsuPlay o2) {
				return o2.getDatePlayed().compareTo(o1.getDatePlayed());
			}
		});
		
		if(m_cachedLatestPlays.size() > Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT) 
			m_cachedLatestPlays = m_cachedLatestPlays.subList(0, Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT);
	}
	
	public void setPlaycount(int p_playcount) {
		m_playcount = p_playcount;
	}
	
	public void setLastActiveTime() {				
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		setLastActiveTime(new Timestamp(calendar.getTime().getTime()));
	}
	
	public void setLastActiveTime(Timestamp p_timestamp) {
		m_lastActiveTime = p_timestamp;
	}
	
	public void setLastUpdateTime() {
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		setLastUpdateTime(new Timestamp(calendar.getTime().getTime()));
	}
	
	public void setLastUpdateTime(Timestamp p_timestamp) {
		m_lastUpdateTime = p_timestamp;
	}
	
	public void setLastRefreshTime() {
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		m_lastRefreshTime = new Timestamp(calendar.getTime().getTime());
	}
	
	public void setLastUploadedTime() {				
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		setLastUploadedTime(new Timestamp(calendar.getTime().getTime()));
	}
	
	public void setLastUploadedTime(Timestamp p_timestamp) {
		m_lastUploadedTime = p_timestamp;
	}
	
	public void setLastStatusMessageTime() {
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		setLastStatusMessageTime(new Timestamp(calendar.getTime().getTime()));
	}
	
	public void setLastStatusMessageTime(Timestamp p_timestamp) {
		m_lastStatusMessageTime = p_timestamp;
	}
	
	public void setIsFetching(boolean p_isFetching) {
		m_isFetching = p_isFetching;
	}
	
	public void setIsDeleted(boolean p_isDeleted) {
		m_isDeleted = p_isDeleted;
	}
	
	public void forceSetActivityCycle(int p_activityCycle) {
		m_activityCycle = p_activityCycle;
	}

	public int updateActivityCycle() {
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		long currentTimeMs = calendar.getTime().getTime();
		
		m_activityCycle = 0;
		for(long[] cycle : Constants.OSU_ACTIVITY_CYCLES)
			if(!m_lastActiveTime.after(new Timestamp(currentTimeMs - cycle[0] * 1000)))
				m_activityCycle++;
			else break;
		
		if(m_activityCycle >= Constants.OSU_ACTIVITY_CYCLES.length)
			m_activityCycle = Constants.OSU_ACTIVITY_CYCLES.length - 1;
		
		return m_activityCycle;
	}
	
	public boolean updateDatabaseEntry() {
		if(m_isDeleted) return false;
		
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		boolean updated = false;
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO `tracked-osu-user` " +
								   "(`id`, `mode`, `playcount`, `last-active`, `last-update`, `last-uploaded`) " +
								   "VALUES (?, 0, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
								   "`playcount`=?, `last-active`=?, `last-update`=?, `last-uploaded`=?");
			
			addDatabaseEntryToUserUpdatePreparedStatement(st);
			
			st.executeUpdate();
			st.close();
			
			updated = true;
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to update registered user (" + getUserId() + ") in local database", e);
		} finally {
			db.closeConnection(conn);
		}
		
		return updated;
	}
	
	public void addDatabaseEntryToUserUpdatePreparedStatement(PreparedStatement p_statement) throws Exception {
		if(m_isDeleted) return;
		
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);

		p_statement.setInt(1, GeneralUtils.stringToInt(getUserId()));
		p_statement.setInt(2, getPlaycount());
		p_statement.setTimestamp(3, getLastActiveTime(), calendar);
		p_statement.setTimestamp(4, getLastUpdateTime(), calendar);
		p_statement.setTimestamp(5, getLastUploadedTime(), calendar);
		p_statement.setInt(6, getPlaycount());
		p_statement.setTimestamp(7, getLastActiveTime(), calendar);
		p_statement.setTimestamp(8, getLastUpdateTime(), calendar);
		p_statement.setTimestamp(9, getLastUploadedTime(), calendar);
	}
}
