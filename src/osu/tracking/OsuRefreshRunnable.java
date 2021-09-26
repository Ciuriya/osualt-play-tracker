package osu.tracking;

import java.util.LinkedList;

public class OsuRefreshRunnable implements Runnable {

	protected LinkedList<OsuTrackedUser> m_usersToRefresh;
	protected int m_initialUserListSize;
	
	protected long m_runnableRefreshDelay;
	protected long m_lastStartTime;
	
	private int m_activityCycle;
	private long m_cachedAverageUserRefreshDelay;
	
	public OsuRefreshRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		m_runnableRefreshDelay = p_runnableRefreshDelay;
		m_activityCycle = p_activityCycle;
		m_cachedAverageUserRefreshDelay = 0;
	}
	
	@Override
	public void run() {
		m_lastStartTime = System.currentTimeMillis();
	}
	
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
		return m_lastStartTime + m_runnableRefreshDelay - System.currentTimeMillis();
	}
	
	public long getTimeElapsed() {
		return System.currentTimeMillis() - m_lastStartTime;
	}
	
	public int getActivityCycle() {
		return m_activityCycle;
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
