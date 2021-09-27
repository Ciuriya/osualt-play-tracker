package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
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
	private Timestamp m_lastUploadedTime;
	
	private int m_activityCycle = 0;
	
	private boolean m_isFetching;
	private boolean m_isDeleted;

	public OsuTrackedUser(String p_userId, int p_mode, int p_playcount) {
		m_userId = p_userId;
		m_mode = p_mode;
		m_playcount = p_playcount;
		m_isFetching = false;
		m_isDeleted = false;
		
		setLastActiveTime();
		setLastUpdateTime();
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
	
	public Timestamp getLastUploadedTime() {
		return m_lastUploadedTime;
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
	
	public void setLastUploadedTime() {				
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		setLastUploadedTime(new Timestamp(calendar.getTime().getTime()));
	}
	
	public void setLastUploadedTime(Timestamp p_timestamp) {
		m_lastUploadedTime = p_timestamp;
	}
	
	public void setIsFetching(boolean p_isFetching) {
		m_isFetching = p_isFetching;
	}
	
	public void setIsDeleted(boolean p_isDeleted) {
		m_isDeleted = p_isDeleted;
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
	
	public void updateDatabaseEntry() {
		if(m_isDeleted) return;
		
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO `tracked-osu-user` " +
								   "(`id`, `mode`, `playcount`, `last-active`, `last-update`, `last-uploaded`) " +
								   "VALUES (?, 0, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
								   "`playcount`=?, `last-active`=?, `last-update`=?, `last-uploaded`=?");
			
			addDatabaseEntryToUserUpdatePreparedStatement(st);
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to update registered user (" + getUserId() + ") in local database", e);
		} finally {
			db.closeConnection(conn);
		}
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
