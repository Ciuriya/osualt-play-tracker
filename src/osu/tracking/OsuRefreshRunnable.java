package osu.tracking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import data.Log;
import utils.GeneralUtils;

public abstract class OsuRefreshRunnable implements Runnable {

	protected LinkedList<OsuTrackedUser> m_usersToRefresh;
	protected int m_initialUserListSize;
	
	protected long m_runnableRefreshDelay;
	protected long m_lastStartTime;
	
	private int m_activityCycle;
	private long m_cachedAverageUserRefreshDelay;
	private boolean m_stopping;
	
	public OsuRefreshRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		m_runnableRefreshDelay = p_runnableRefreshDelay;
		m_activityCycle = p_activityCycle;
		m_cachedAverageUserRefreshDelay = 0;
		m_stopping = false;
	}
	
	@Override
	public void run() {
		m_lastStartTime = System.currentTimeMillis();
		
		try {
			for(;;) {
				if(m_stopping) return;
				if(m_usersToRefresh.size() == 0) break;
				
				List<OsuTrackedUser> users = new ArrayList<>();
				
				if(m_activityCycle > 0) {
					for(OsuTrackedUser userToRefresh : new LinkedList<>(m_usersToRefresh)) {
						if(!userToRefresh.isFetching()) {
							users.add(userToRefresh);
							m_usersToRefresh.remove(userToRefresh);
							
							if(users.size() >= 50) break;
						}
					}
				} else users.add(m_usersToRefresh.removeFirst());
					
				int usersLeft = m_usersToRefresh.size();
				long currentTime = System.currentTimeMillis();
				long nextDelay = (m_lastStartTime + m_runnableRefreshDelay - currentTime) / (usersLeft < 50 ? 50 : usersLeft) * 50;
	
				if(m_activityCycle == 0) {
					OsuTrackedUser user = users.get(0);
					
					if(!user.isFetching()) {
						user.setIsFetching(true);
						
						if(refreshUser(user)) {
							user.updateActivityCycle();
							user.updateDatabaseEntry();
							OsuTrackingManager.getInstance().resetFailedUserChecks(GeneralUtils.stringToInt(user.getUserId()));
						}
	
						user.setLastRefreshTime();
						
						user.setJustMovedCycles(false);
						user.setIsFetching(false);
					}
				} else updateUserActivities(users);
								
				long updatedDelay = nextDelay - (System.currentTimeMillis() - currentTime);
				if(m_runnableRefreshDelay > 0 && updatedDelay > 0)
					GeneralUtils.sleep((int) updatedDelay);
			}
		} catch(Exception e) {
			Log.log(Level.SEVERE, "osu!refresh runnable exception | cycle: " + m_activityCycle, e);
		}
		
		long time = System.currentTimeMillis();
		long expectedEndTime = m_lastStartTime + m_runnableRefreshDelay;
		if(expectedEndTime > time) {
			GeneralUtils.sleep((int) (expectedEndTime - time));
		}
		
		callStart();
	}
	
	public abstract boolean refreshUser(OsuTrackedUser p_user);
	
	public abstract boolean updateUserActivities(List<OsuTrackedUser> p_users);
	
	public int getInitialUserListSize() {
		return m_initialUserListSize;
	}
	
	public int getUsersLeft() {
		return m_usersToRefresh.size();
	}

	public long getAverageUserRefreshDelay() {
		int usersRefreshed = getInitialUserListSize() - getUsersLeft();
		
		return usersRefreshed > 0 ? (System.currentTimeMillis() - m_lastStartTime) / usersRefreshed : m_cachedAverageUserRefreshDelay;
	}
	
	public long getTimeUntilStop() {
		return getUsersLeft() * getAverageUserRefreshDelay();
	}
	
	public long getExpectedTimeUntilStop() {
		if(m_runnableRefreshDelay == 0) {
			return getTimeUntilStop();
		}
		
		return m_lastStartTime + m_runnableRefreshDelay - System.currentTimeMillis();
	}
	
	public long getRefreshDelay() {
		return m_runnableRefreshDelay;
	}
	
	public long getTimeElapsed() {
		return System.currentTimeMillis() - m_lastStartTime;
	}
	
	public long getTimeUntilUserRefresh(String p_userId) {
		LinkedList<OsuTrackedUser> userListCopy = new LinkedList<>(m_usersToRefresh);
		for(int i = 0; i < userListCopy.size(); ++i)
			if(userListCopy.get(i).getUserId() == p_userId)
				return i * m_cachedAverageUserRefreshDelay;
		
		return -1;
	}
	
	public void removeUser(String p_userId) {
		m_usersToRefresh.removeIf(u -> u.getUserId().contentEquals(p_userId));
	}
	
	public int getActivityCycle() {
		return m_activityCycle;
	}
	
	public void callStop() {
		m_stopping = true;
	}
	
	public void setUsersToRefresh(LinkedList<OsuTrackedUser> p_usersToRefresh) {
		m_usersToRefresh = p_usersToRefresh;
		m_initialUserListSize = m_usersToRefresh.size();
	}
	
	public void callStart() {
		m_cachedAverageUserRefreshDelay = getAverageUserRefreshDelay();
		m_usersToRefresh.clear();
		OsuTrackingManager.getInstance().startLoop(this, m_runnableRefreshDelay);
	}
}
