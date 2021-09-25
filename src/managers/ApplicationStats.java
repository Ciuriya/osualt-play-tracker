package managers;

public class ApplicationStats {

	private static ApplicationStats instance;
	private long m_bootTimestamp;
	private long m_startupTime;
	private long m_timerStart;
	
	private boolean m_osuApiStalled;
	private double m_osuApiLoad;
	private int m_osuApiRequestsSent;
	private int m_osuApiRequestsFailed;
	
	private boolean m_osuHtmlStalled;
	private double m_osuHtmlLoad;
	private int m_osuHtmlRequestsSent;
	private int m_osuHtmlRequestsFailed;
	
	public static ApplicationStats getInstance() {
		if(instance == null) instance = new ApplicationStats();
		
		return instance;
	}
	
	public ApplicationStats() {
		m_bootTimestamp = System.currentTimeMillis();
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
	
	public boolean isOsuApiStalled() {
		return m_osuApiStalled;
	}
	
	public double getOsuApiLoad() {
		return m_osuApiLoad;
	}
	
	public int getOsuApiRequestsSent() {
		return m_osuApiRequestsSent;
	}
	
	public int getOsuApiRequestsFailed() {
		return m_osuApiRequestsFailed;
	}
	
	public void setOsuApiStalled(boolean p_apiStalled) {
		m_osuApiStalled = p_apiStalled;
	}
	
	public void setOsuApiLoad(double p_apiLoad) {
		m_osuApiLoad = p_apiLoad;
	}
	
	public void addOsuApiRequestSent() {
		m_osuApiRequestsSent++;
	}
	
	public void addOsuApiRequestFailed() {
		m_osuApiRequestsFailed++;
	}
	
	public boolean isOsuHtmlStalled() {
		return m_osuHtmlStalled;
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
	
	public void setOsuHtmlLoad(double p_htmlLoad) {
		m_osuHtmlLoad = p_htmlLoad;
	}
	
	public void addOsuHtmlRequestSent() {
		m_osuHtmlRequestsSent++;
	}
	
	public void addOsuHtmlRequestFailed() {
		m_osuHtmlRequestsFailed++;
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
