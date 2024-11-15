package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
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
	
	private List<OsuRefreshRunnable> m_liveCycleRunnablesWaiting;
	
	public static OsuTrackingManager getInstance() {
		if(instance == null) instance = new OsuTrackingManager();
		
		return instance;
	}
	
	public OsuTrackingManager() {
		m_refreshRunnables = new LinkedList<>();
		m_loadedUsers = new ArrayList<>();
		m_loadedUserIds = new ArrayList<>();
		m_liveCycleRunnablesWaiting = new ArrayList<>();
		
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
						OsuPlay.pruneOldPlays();
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
			long refreshDelay = cycle[1] * 1000;
			
			OsuRefreshRunnable runnable = new OsuTrackingRunnable(i, refreshDelay);
			m_refreshRunnables.add(runnable);
			
			startLoop(runnable, refreshDelay);
		}
	}
	
	public void startLoop(OsuRefreshRunnable p_runnable, long p_maxDelay) {
		if(p_runnable.getActivityCycle() < Constants.OSU_FULL_REFRESH_ACTIVITY_CYCLE_COUNT) {
			m_liveCycleRunnablesWaiting.add(p_runnable);
			
			if (m_liveCycleRunnablesWaiting.size() != Constants.OSU_FULL_REFRESH_ACTIVITY_CYCLE_COUNT) return;
			
			List<OsuTrackedUser> users = m_loadedUsers.stream().filter(u -> u.getActivityCycle() < Constants.OSU_FULL_REFRESH_ACTIVITY_CYCLE_COUNT)
					   										   .collect(Collectors.toCollection(ArrayList::new));
			
			int usersPerCycle = (int) Math.ceil(users.size() / (double) m_liveCycleRunnablesWaiting.size());
			for (OsuRefreshRunnable runnable : m_liveCycleRunnablesWaiting) {
				LinkedList<OsuTrackedUser> runnableUsers = users.stream().limit(usersPerCycle)
																		 .collect(Collectors.toCollection(LinkedList::new));
				runnable.setUsersToRefresh(runnableUsers);
				
				users.removeAll(runnableUsers);
				
				ThreadingManager.getInstance().executeAsync(runnable, 5000 * 1000, false);
			}
			
			m_liveCycleRunnablesWaiting.clear();
			
			return;
		} 
		
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
			int successfullyAddedNewUserIds = 0;
			
			newUserIds.removeAll(m_loadedUserIds);
			
			try {
				List<Future<Object>> futures = new ArrayList<>();
				List<Future<Object>> futuresDone = new ArrayList<>();
				
				for(int userId : newUserIds) {
					OsuUserRequest userRequest = new OsuUserRequest(OsuRequestTypes.API, String.valueOf(userId), "0");
					futures.add(OsuRequestRegulator.getInstance().sendRequestAsync(userRequest, 15000, false));
					
					while (futures.size() >= Constants.THREAD_POOL_SIZE / 2) {
						GeneralUtils.sleep(1000);
						
						List<Future<Object>> tempFutures = new ArrayList<>();
						for (Future<Object> runningFuture : futures)
							if (runningFuture.isCancelled() || runningFuture.isDone())
								tempFutures.add(runningFuture);
						
						futures.removeAll(tempFutures);
						futuresDone.addAll(tempFutures);
					}
				}
				
				if (!futures.isEmpty()) {
					GeneralUtils.sleep(15000);
					
					for (Future<Object> runningFuture : futures)
						if (runningFuture.isCancelled() || runningFuture.isDone())
							futuresDone.add(runningFuture);
				}
				
				PreparedStatement osuUserInsertSt = conn.prepareStatement(
													"INSERT INTO `osu-user` (`id`, `username`) " +
					    							"VALUES (?, ?) ON DUPLICATE KEY UPDATE `username`=?");
				
				for(Future<Object> future : futuresDone) {
					Object userObject = null;
					
					try {
						userObject = future.get();
					} catch (Exception ex) {}
					
					if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
						JSONObject userJson = (JSONObject) userObject;
						
						if(userJson.has("username") && userJson.has("id")) {
							String username = userJson.getString("username");
							int userId = userJson.getInt("id");
							
							osuUserInsertSt.setInt(1, userId);
							osuUserInsertSt.setString(2, username);
							osuUserInsertSt.setString(3, username);
							
							osuUserInsertSt.addBatch();
							
							JSONObject stats = userJson.optJSONObject("statistics");
							
							if(stats != null) {
								OsuTrackedUser user = new OsuTrackedUser(String.valueOf(userId), 0, stats.optInt("play_count"));
								m_loadedUsers.add(user);
								m_loadedUserIds.add(userId);
								
								++successfullyAddedNewUserIds;
							}
							
							
							continue;
						}
					}
				}
				
				osuUserInsertSt.executeBatch();
				osuUserInsertSt.close();
				

				PreparedStatement trackUserUpdateSt = conn.prepareStatement(
													  "INSERT INTO `tracked-osu-user` " +
													  "(`id`, `mode`, `playcount`, `last-active`, `last-update`, `last-uploaded`) " +
													  "VALUES (?, 0, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
													  "`playcount`=?, `last-active`=?, `last-update`=?, `last-uploaded`=?");
				
				for(OsuTrackedUser user : m_loadedUsers) {
					user.addDatabaseEntryToUserUpdatePreparedStatement(trackUserUpdateSt);
					
					trackUserUpdateSt.addBatch();
				}
				
				trackUserUpdateSt.executeBatch();
				trackUserUpdateSt.close();
			} catch(Exception e) {
				Log.log(Level.SEVERE, "Failed to update registered users in local database", e);
			}
			
			db.closeConnection(conn);
		
			if(!p_removedUsers.isEmpty()) removeRegisteredUsers(p_removedUsers);

			Log.log(Level.INFO, "Updated registered users: " + m_loadedUsers.size() + " registered users, " + 
															   successfullyAddedNewUserIds + " added, " + 
															   p_removedUsers.size() + " removed, " +
															   (p_users.size() - m_loadedUsers.size()) + " ignored/banned");
		}
	}
	
	private void removeRegisteredUsers(List<Integer> p_userIds) {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		try {
			PreparedStatement userDeleteSt = conn.prepareStatement(
											 "DELETE FROM `tracked-osu-user` WHERE `id`=? AND `mode`=0");
			PreparedStatement playDeleteSt = conn.prepareStatement(
											 "DELETE FROM `osu-play` WHERE `user_id`=?");

			for(int userId : p_userIds) {
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
		
		m_loadedUserIds.removeAll(p_userIds);
		
		List<OsuTrackedUser> removedTrackedUsers = m_loadedUsers.stream().filter(u -> p_userIds.contains(GeneralUtils.stringToInt(u.getUserId())))
																		 .collect(Collectors.toList());
		
		for(OsuTrackedUser removed : removedTrackedUsers)
			removed.setIsDeleted(true);
		
		m_loadedUsers.removeAll(removedTrackedUsers);
		
		db.closeConnection(conn);
	}
}
