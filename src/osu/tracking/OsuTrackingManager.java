package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.json.JSONObject;

import data.Database;
import data.Log;
import managers.DatabaseManager;
import managers.ThreadingManager;
import osu.api.OsuRequestRegulator;
import osu.api.OsuRequestTypes;
import osu.api.requests.OsuUserRequest;
import utils.Constants;
import utils.GeneralUtils;
import utils.OsuUtils;

public class OsuTrackingManager {
	
	private static OsuTrackingManager instance;
	
	private LinkedList<OsuRefreshRunnable> m_refreshRunnables;
	private List<OsuTrackedUser> m_loadedUsers;
	private List<Integer> m_loadedUserIds;
	
	public static OsuTrackingManager getInstance() {
		if(instance == null) instance = new OsuTrackingManager();
		
		return instance;
	}
	
	public OsuTrackingManager() {
		m_refreshRunnables = new LinkedList<>();
		m_loadedUsers = new ArrayList<>();
		m_loadedUserIds = new ArrayList<>();
		
		ThreadingManager.getInstance().executeAsync(new Runnable() {
			public void run() {
				loadRegisteredUsers();
				refreshRegisteredUsers();
				startLoops();
				
				long registeredUserRefreshInterval = Constants.OSU_REGISTERED_USER_REFRESH_INTERVAL * 1000;
				new Timer().scheduleAtFixedRate(new TimerTask() {
					public void run() {
						refreshRegisteredUsers();
					}
				}, registeredUserRefreshInterval, registeredUserRefreshInterval);
				
				long osuPlayPruningInterval = Constants.OSU_PLAY_PRUNING_LOOP_INTERVAL * 1000;
				new Timer().scheduleAtFixedRate(new TimerTask() {
					public void run() {
						//OsuPlay.pruneOldPlays(); http://smcmax.com/s/2021-09-29_13-24-14.png
					}
				}, osuPlayPruningInterval, osuPlayPruningInterval);
			}
		}, 3600 * 1000, true);
	}
	
	public int getLoadedRegisteredUsers() {
		return m_loadedUsers.size();
	}
	
	public OsuTrackedUser getUser(String p_userId) {
		return m_loadedUsers.stream().filter(u -> u.getUserId().contentEquals(p_userId)).findFirst().orElse(null);
	}
	
	public OsuRefreshRunnable getRefreshRunnable(int p_cycle) {
		if(m_refreshRunnables.size() > p_cycle && m_refreshRunnables.size() > 0)
			return m_refreshRunnables.get(p_cycle);
		else return null;
	}
	
	private void startLoops() {
		for(int i = 0; i < Constants.OSU_ACTIVITY_CYCLES.length; ++i) {
			long[] cycle = Constants.OSU_ACTIVITY_CYCLES[i];
			long cutoff = cycle[0] * 1000;
			long refreshDelay = cycle[1] * 1000;
			
			OsuRefreshRunnable runnable = new OsuTrackingRunnable(i, refreshDelay > 0 ? refreshDelay : 0);
			m_refreshRunnables.add(runnable);
			
			startLoop(runnable, refreshDelay == 0 ? cutoff : refreshDelay);
		}
	}
	
	public void startLoop(OsuRefreshRunnable p_runnable, long p_maxDelay) {
		p_runnable.setUsersToRefresh(m_loadedUsers.stream().filter(u -> u.getActivityCycle() == p_runnable.getActivityCycle())
														   .collect(Collectors.toCollection(LinkedList::new)));
		ThreadingManager.getInstance().executeAsync(p_runnable, (int) p_maxDelay * 2, false);
	}
	
	public void stop() {
		for(OsuRefreshRunnable runnable : m_refreshRunnables)
			runnable.callStop();
	}
	
	private void loadRegisteredUsers() {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT * FROM `tracked-osu-user` WHERE `mode`=0");
			
			ResultSet rs = st.executeQuery();
	
			while(rs.next()) {
				int userId = rs.getInt(1);
				
				if(!m_loadedUserIds.contains(userId)) {
					OsuTrackedUser user = new OsuTrackedUser(rs);
					
					m_loadedUsers.add(user);
					m_loadedUserIds.add(userId);
				}
			}
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to fetch registered users from local database", e);
		} finally {
			db.closeConnection(conn);
		}
		
		Log.log(Level.INFO, "Loaded " + m_loadedUsers.size() + " registered users from local db");
	}
	
	private void refreshRegisteredUsers() {
		List<Integer> remoteUsers = fetchRegisteredUsersFromServer();
		List<Integer> removedUsers = new ArrayList<>(m_loadedUserIds);
		
		removedUsers.removeAll(remoteUsers);
		updateRegisteredUsers(remoteUsers, removedUsers);
	}
	
	private List<Integer> fetchRegisteredUsersFromServer() {
		List<Integer> users = new ArrayList<>();
		Database db = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT user_id FROM priorityuser");
			
			ResultSet rs = st.executeQuery();
	
			while(rs.next())
				users.add(rs.getInt(1));
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to fetch registered users from osu!alt's remote database", e);
		} finally {
			db.closeConnection(conn);
		}
		
		Log.log(Level.INFO, "Fetched " + users.size() + " registered users from remote server");
		return users;
	}
	
	private void updateRegisteredUsers(List<Integer> p_users, List<Integer> p_removedUsers) {	
		if(!p_users.isEmpty()) {
			Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
			Connection conn = db.getConnection();
			
			List<Integer> newUserIds = new ArrayList<>(p_users);
			newUserIds.removeAll(m_loadedUserIds);
			
			try {
				PreparedStatement osuUserInsertSt = conn.prepareStatement(
												    "INSERT INTO `osu-user` (`id`, `username`) " +
												    "VALUES (?, ?) ON DUPLICATE KEY UPDATE `username`=?");
				PreparedStatement trackUserUpdateSt = conn.prepareStatement(
													  "INSERT INTO `tracked-osu-user` " +
													  "(`id`, `mode`, `playcount`, `last-active`, `last-update`, `last-uploaded`) " +
													  "VALUES (?, 0, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
													  "`playcount`=?, `last-active`=?, `last-update`=?, `last-uploaded`=?");
				
				for(int userId : newUserIds) {
					OsuUserRequest userRequest = new OsuUserRequest(OsuRequestTypes.API, String.valueOf(userId), "0");
					Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 30000, true);
					
					if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
						JSONObject userJson = (JSONObject) userObject;
						
						String username = userJson.getString("username");
						osuUserInsertSt.setInt(1, userId);
						osuUserInsertSt.setString(2, username);
						osuUserInsertSt.setString(3, username);
						
						osuUserInsertSt.addBatch();
						
						OsuTrackedUser user = new OsuTrackedUser(String.valueOf(userId), 0, userJson.getJSONObject("statistics").getInt("play_count"));
						m_loadedUsers.add(user);
						m_loadedUserIds.add(userId);
					}
				}
				
				osuUserInsertSt.executeBatch();
				osuUserInsertSt.close();
				
				for(OsuTrackedUser user : m_loadedUsers) {
					user.addDatabaseEntryToUserUpdatePreparedStatement(trackUserUpdateSt);
					
					trackUserUpdateSt.addBatch();
				}
				
				trackUserUpdateSt.executeBatch();
				trackUserUpdateSt.close();
			} catch(Exception e) {
				Log.log(Level.SEVERE, "Failed to update registered users in local database", e);
			}
		
			if(!p_removedUsers.isEmpty()) {
				try {
					PreparedStatement userDeleteSt = conn.prepareStatement(
													 "DELETE FROM `tracked-osu-user` WHERE `id`=? AND `mode`=0");
					PreparedStatement playDeleteSt = conn.prepareStatement(
													 "DELETE FROM `osu-play` WHERE `user_id`=?");
	
					for(int userId : p_removedUsers) {
						userDeleteSt.setInt(1, userId);
						userDeleteSt.addBatch();
						
						playDeleteSt.setInt(1, userId);
						playDeleteSt.addBatch();
					}
					
					playDeleteSt.executeBatch();
					playDeleteSt.close();
					
					userDeleteSt.executeBatch();
					userDeleteSt.close();
				} catch(Exception e) {
					Log.log(Level.SEVERE, "Failed to update registered users in local database", e);
				}
				
				m_loadedUserIds.removeAll(p_removedUsers);
				
				List<OsuTrackedUser> removedTrackedUsers = m_loadedUsers.stream().filter(u -> p_removedUsers.contains(GeneralUtils.stringToInt(u.getUserId())))
																				 .collect(Collectors.toList());
				
				for(OsuTrackedUser removed : removedTrackedUsers) {
					removed.setIsDeleted(true);
				}
				
				m_loadedUsers.removeAll(removedTrackedUsers);
			}
			
			db.closeConnection(conn);
			
			Log.log(Level.INFO, "Updated registered users: " + p_users.size() + " users, " + newUserIds.size() + " added, " + 
																							 p_removedUsers.size() + " removed");
		}
	}
}
