package managers;

import java.util.LinkedList;

import utils.Constants;

public class ApplicationStats {

	private static ApplicationStats instance;
	private long m_bootTimestamp;
	private long m_startupTime;
	private long m_timerStart;
	
	private LinkedList<Boolean> m_osuApisStalled;
	private LinkedList<Integer> m_osuApisFailedAttempts;
	private LinkedList<Double> m_osuApisLoads;
	private LinkedList<Integer> m_osuApisRequestsSent;
	private LinkedList<Integer> m_osuApisRequestsFailed;
	
	private boolean m_osuHtmlStalled;
	private int m_osuHtmlFailedAttempts;
	private double m_osuHtmlLoad;
	private int m_osuHtmlRequestsSent;
	private int m_osuHtmlRequestsFailed;
	
	private int m_scoresUploaded;
	
	public static ApplicationStats getInstance() {
		if(instance == null) instance = new ApplicationStats();
		
		return instance;
	}
	
	public ApplicationStats() {
		m_bootTimestamp = System.currentTimeMillis();
		m_osuApisStalled = new LinkedList<Boolean>();
		m_osuApisFailedAttempts = new LinkedList<Integer>();
		m_osuApisLoads = new LinkedList<Double>();
		m_osuApisRequestsSent = new LinkedList<Integer>();
		m_osuApisRequestsFailed = new LinkedList<Integer>();
	}
	
	public void loadOsuApiLists() {
		m_osuApisStalled.clear();
		m_osuApisFailedAttempts.clear();
		m_osuApisLoads.clear();
		m_osuApisRequestsSent.clear();
		m_osuApisRequestsFailed.clear();
		
		for (int i = 0; i < Constants.OSU_API_CLIENTS_AUTHENTICATED; ++i) {
			m_osuApisStalled.add(false);
			m_osuApisFailedAttempts.add(0);
			m_osuApisLoads.add(0d);
			m_osuApisRequestsSent.add(0);
			m_osuApisRequestsFailed.add(0);
		}
	}
	
	public long getStartupTime() {
		return m_startupTime;
	}
	
	public void setStartupTime(long p_startupTime) {
		m_startupTime = p_startupTime;
	}
	
	public long getUptime() {
		return System.currentTimeMillis() - m_bootTimestamp;
	}
	
	public boolean isOsuApiStalled(int index) {
		return m_osuApisStalled.get(index);
	}
	
	public int getOsuApiStallQuantity() {
		int stalled = 0;
		
		for (int i = 0; i < m_osuApisStalled.size(); ++i)
			if (m_osuApisStalled.get(i))
				stalled++;
		
		return stalled;
	}
	
	public boolean areAllOsuApisStalled() {
		return getOsuApiStallQuantity() == m_osuApisStalled.size();
	}
	
	public int getOsuApiFailedAttempts(int index) {
		return m_osuApisFailedAttempts.get(index);
	}
	
	public double getOsuApiLoad(int index) {
		return m_osuApisLoads.get(index);
	}
	
	public double getCombinedOsuApiLoad() {
		double load = 0.0d;
		for (int i = 0; i < m_osuApisLoads.size(); ++i)
			load += getOsuApiLoad(i);
		
		return load;
	}
	
	public int getOsuApiRequestsSent(int index) {
		return m_osuApisRequestsSent.get(index);
	}	
	
	public int getCombinedOsuApiRequestsSent() {
		int sent = 0;
		for (int i = 0; i < m_osuApisRequestsSent.size(); ++i)
			sent += getOsuApiRequestsSent(i);
		
		return sent;
	}
	
	public int getOsuApiRequestsFailed(int index) {
		return m_osuApisRequestsFailed.get(index);
	}
	
	public int getCombinedOsuApiRequestsFailed() {
		int failed = 0;
		for (int i = 0; i < m_osuApisRequestsFailed.size(); ++i)
			failed += getOsuApiRequestsFailed(i);
		
		return failed;
	}
	
	public void setOsuApiStalled(int index, boolean p_apiStalled) {
		m_osuApisStalled.set(index, p_apiStalled);
	}
	
	public void setOsuApiFailedAttempts(int index, int p_failedAttempts) {
		m_osuApisFailedAttempts.set(index, p_failedAttempts);
	}
	
	public void setOsuApiLoad(int index, double p_apiLoad) {
		m_osuApisLoads.set(index, p_apiLoad);
	}
	
	public void addOsuApiRequestSent(int index) {
		m_osuApisRequestsSent.set(index, getOsuApiRequestsSent(index) + 1);
	}
	
	public void addOsuApiRequestFailed(int index) {
		m_osuApisRequestsFailed.set(index, getOsuApiRequestsFailed(index) + 1);
	}
	
	public boolean isOsuHtmlStalled() {
		return m_osuHtmlStalled;
	}
	
	public int getOsuHtmlFailedAttempts() {
		return m_osuHtmlFailedAttempts;
	}
	
	public double getOsuHtmlLoad() {
		return m_osuHtmlLoad;
	}
	
	public int getOsuHtmlRequestsSent() {
		return m_osuHtmlRequestsSent;
	}
	
	public int getOsuHtmlRequestsFailed() {
		return m_osuHtmlRequestsFailed;
	}
	
	public void setOsuHtmlStalled(boolean p_htmlStalled) {
		m_osuHtmlStalled = p_htmlStalled;
	}
	
	public void setOsuHtmlFailedAttempts(int p_failedAttempts) {
		m_osuHtmlFailedAttempts = p_failedAttempts;
	}
	
	public void setOsuHtmlLoad(double p_htmlLoad) {
		m_osuHtmlLoad = p_htmlLoad;
	}
	
	public void addOsuHtmlRequestSent() {
		m_osuHtmlRequestsSent++;
	}
	
	public void addOsuHtmlRequestFailed() {
		m_osuHtmlRequestsFailed++;
	}
	
	public int getScoresUploaded() {
		return m_scoresUploaded;
	}
	
	public void addScoresUploaded(int p_scoresUploaded) {
		m_scoresUploaded += p_scoresUploaded;
	}
	
	// a timer to count the time something takes in code
	// simply start when needed and stop when needed
	// start will tell you if it started and stop will give you the time it took
	public boolean startTimer() {
		if(m_timerStart > 0) return false;
		
		m_timerStart = System.currentTimeMillis();
		
		return true;
	}
	
	public long stopTimer() {
		if(m_timerStart == 0) return 0;
		
		long time = System.currentTimeMillis() - m_timerStart;
		
		m_timerStart = 0;
		
		return time;
	}
}
