package osu.api;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;

import data.Log;
import managers.ApplicationStats;
import managers.ThreadingManager;
import utils.Constants;
import utils.GeneralUtils;

public class OsuRequestRegulator {
	
	private static OsuRequestRegulator instance;
	private LinkedList<OsuRequest> m_apiRequests;
	private LinkedList<OsuRequest> m_htmlRequests;
	
	private LinkedList<Integer> m_lastRefreshRequestsCount;
	private long lastLoadRefresh = 0;
	
	public static OsuRequestRegulator getInstance() {
		if(instance == null) instance = new OsuRequestRegulator();
		
		return instance;
	}
	
	public OsuRequestRegulator() {
		lastLoadRefresh = System.currentTimeMillis();
		
		m_lastRefreshRequestsCount = new LinkedList<>();
		m_apiRequests = new LinkedList<>();
		m_htmlRequests = new LinkedList<>();
		
		startRequestDistributionTimer();
		startLoadTimer();
	}

	private void startRequestDistributionTimer() {
		final ApplicationStats stats = ApplicationStats.getInstance();
		final ThreadingManager threadManager = ThreadingManager.getInstance();
		
		stats.loadOsuApiLists();
		
		for (int i = -1; i < Constants.OSU_API_CLIENTS_AUTHENTICATED; ++i) {
			long delay = (long) (60000.0 / (double) (i == -1 ? Constants.OSU_HTML_REQUESTS_PER_MINUTE :
															   Constants.OSU_API_REQUESTS_PER_MINUTE_PER_KEY));
			final int j = i;
			m_lastRefreshRequestsCount.add(0);
			
			new Timer().scheduleAtFixedRate(new TimerTask() {
				public void run() {
					// if we have a request that can't send, stall the timer until it works out
					if (j == -1 && stats.isOsuHtmlStalled()) return;
					else if (j >= 0 && stats.isOsuApiStalled(j)) return;
					
					OsuRequest toProcess = null;
					
					try {
						toProcess = j == -1 ? m_htmlRequests.getFirst() : m_apiRequests.getFirst();
						
						if(toProcess != null) {
							if(j >= 0) m_apiRequests.remove(toProcess);
							else m_htmlRequests.remove(toProcess);
						}
					} catch(NoSuchElementException e) { }
					
					if(toProcess != null) {
						final OsuRequest request = toProcess;
						request.setHandlerIndex(j);
						
						threadManager.executeAsync(new Runnable() {
							public void run() {
								processOsuRequest(request, j);
							}
						}, (int) (request.getTimeout() - (System.currentTimeMillis() - request.getTimeSent())), true);
					}
				}
			}, delay, delay);
		}
		
	}
	
	// p_apiIndex is -1 for html, 0-indexed for each api key being used
	private void processOsuRequest(final OsuRequest p_request, int p_apiIndex) {
		int currentRequestAttempts = 0;
		int attempts = getFailedAttempts(p_apiIndex);
		
		if (attempts > 0) setStalled(p_apiIndex, true);
		
		while(currentRequestAttempts < Constants.OSU_REQUEST_FAILS_ALLOWED) {
			try {
				addRequestSent(p_apiIndex);
				p_request.send(p_apiIndex);
				
				if(p_request.getAnswer() instanceof String && ((String) p_request.getAnswer()).contentEquals("failed"))
					throw new Exception("Request returned " + ((String) p_request.getAnswer()));
				
				if(attempts > 0) setFailedAttempts(p_apiIndex, 0);
				
				break;
			} catch(Exception e) {
				setStalled(p_apiIndex, true);
				
				int nextAttemptDelay = Constants.FIBONACCI[Math.min(Constants.FIBONACCI.length - 1, attempts)] * 1000;
				Log.log(Level.WARNING, "Retrying o!" + (p_apiIndex >= 0 ? "api (" + p_apiIndex + ")" : "html") + " request in " + nextAttemptDelay + "s\n" +
									   "Request: " + p_request.getName() + " Ex: " + e.getMessage());
				
				GeneralUtils.sleep(nextAttemptDelay);
			}
			
			addFailedAttempt(p_apiIndex);
			++attempts;
			++currentRequestAttempts;
		}
		
		setStalled(p_apiIndex, false);
	}
	
	private void setStalled(int p_apiIndex, boolean p_stalled) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		if(p_apiIndex >= 0) stats.setOsuApiStalled(p_apiIndex, p_stalled);
		else stats.setOsuHtmlStalled(p_stalled);
	}
	
	private void addRequestSent(int p_apiIndex) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		if(p_apiIndex >= 0) stats.addOsuApiRequestSent(p_apiIndex);
		else stats.addOsuHtmlRequestSent();
	}
	
	private int getFailedAttempts(int p_apiIndex) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		return p_apiIndex >= 0 ? stats.getOsuApiFailedAttempts(p_apiIndex) : stats.getOsuHtmlFailedAttempts();
	}
	
	private void addFailedAttempt(int p_apiIndex) {
		setFailedAttempts(p_apiIndex, getFailedAttempts(p_apiIndex) + 1);
	}
	
	private void setFailedAttempts(int p_apiIndex, int amount) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		if(p_apiIndex >= 0) stats.setOsuApiFailedAttempts(p_apiIndex, amount);
		else stats.setOsuHtmlFailedAttempts(amount);
	}
	
	public Object sendRequestSync(OsuRequest p_request, int p_timeout, boolean p_priority) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		sendRequest(p_request, p_timeout, p_priority);
		
		int timeElapsed = 0;
		
		while(p_request.getAnswer() == null) {
			if(timeElapsed >= p_request.getTimeout()) {
				if(p_request.getType() == OsuRequestTypes.API) {
					m_apiRequests.remove(p_request);
					stats.addOsuApiRequestFailed(p_request.getHandlerIndex());
				} else {
					m_htmlRequests.remove(p_request);
					stats.addOsuHtmlRequestFailed();
				}
				
				break;
			}
			
			GeneralUtils.sleep(25);
			timeElapsed += 25;
		}
		
		return p_request.getAnswer();
	}
	
	public Future<Object> sendRequestAsync(OsuRequest p_request, int p_timeout, boolean p_priority) {
		return ThreadingManager.getInstance().executeAsync(new Callable<Object>() {
			public Object call() {
				return sendRequestSync(p_request, p_timeout - 500, p_priority);
			}
		}, p_timeout, false);
	}
	
	private void sendRequest(OsuRequest p_request, int p_timeout, boolean p_priority) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		p_request.setTimeout(p_timeout > 0 ? p_timeout : 30000);
		
		// assign a type in advance
		if(p_request.getType() == OsuRequestTypes.BOTH) {
			OsuRequestTypes type = OsuRequestTypes.API;
			
			// if we're stalled, use the other
			if(stats.areAllOsuApisStalled() && !stats.isOsuHtmlStalled()) type = OsuRequestTypes.HTML;
			
			p_request.setType(type);
		}
		
		p_request.setSentTime();
		
		if(p_request.getType() == OsuRequestTypes.API) {
			if(p_priority) m_apiRequests.addFirst(p_request);
			else m_apiRequests.add(p_request);
		} else {
			if(p_priority) m_htmlRequests.addFirst(p_request);
			else m_htmlRequests.add(p_request);
		}
	}
	
	private void startLoadTimer() {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				ThreadingManager.getInstance().executeSync(new Runnable() {
					public void run() {
						calculateLoads();
					}
				}, 1000);
			}
		}, 1000, 1000);
	}
	
	private void calculateLoads() {
		ApplicationStats stats = ApplicationStats.getInstance(); 
		long delay = System.currentTimeMillis() - lastLoadRefresh;
		
		// 0 = html, rest are api clients
		for(int i = 0; i < Constants.OSU_API_CLIENTS_AUTHENTICATED + 1; ++i) {
			int requestsSent = i == 0 ? stats.getOsuHtmlRequestsSent() : stats.getOsuApiRequestsSent(i - 1);
			
			if(delay >= 60000) {
				lastLoadRefresh = System.currentTimeMillis();
				
				m_lastRefreshRequestsCount.set(i, requestsSent);
			} else {
				int requests = requestsSent - m_lastRefreshRequestsCount.get(i);
				double timeSliceMult = 60000.0 / (double) delay;
				
				if (i == 0)
					stats.setOsuHtmlLoad(((double) requests * timeSliceMult) / (double) Constants.OSU_HTML_REQUESTS_PER_MINUTE);
				else stats.setOsuApiLoad(i - 1, ((double) requests * timeSliceMult) / (double) Constants.OSU_API_REQUESTS_PER_MINUTE_PER_KEY);
			}
		}
	}
}