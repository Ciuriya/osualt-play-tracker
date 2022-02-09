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
	
	private int lastRefreshApiRequestsCount = 0;
	private int lastRefreshHtmlRequestsCount = 0;
	private long lastLoadRefresh = 0;
	
	public static OsuRequestRegulator getInstance() {
		if(instance == null) instance = new OsuRequestRegulator();
		
		return instance;
	}
	
	public OsuRequestRegulator() {
		lastLoadRefresh = System.currentTimeMillis();
		
		m_apiRequests = new LinkedList<>();
		m_htmlRequests = new LinkedList<>();
		
		startRequestTimer(true);
		startRequestTimer(false);
		
		startLoadTimer();
	}
	
	// this is for both api and html, so p_isApi is just to distinguish
	// and use the proper linked list
	// they're separate so they can have their own timer on their own speed
	// based on the amount of requests they can do using their request type
	private void startRequestTimer(boolean p_isApi) {
		final ApplicationStats stats = ApplicationStats.getInstance();
		final ThreadingManager threadManager = ThreadingManager.getInstance();
		long delay = (long) (60000.0 / (double) (p_isApi ? Constants.OSU_API_REQUESTS_PER_MINUTE :
														   Constants.OSU_HTML_REQUESTS_PER_MINUTE));
		
		setStalled(p_isApi, false);
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// if we have a request that can't send, stall the timer until it works out
				if((p_isApi && stats.isOsuApiStalled()) || (!p_isApi && stats.isOsuHtmlStalled())) return;
				
				OsuRequest toProcess = null;
				
				try {
					toProcess = p_isApi ? m_apiRequests.getFirst() : m_htmlRequests.getFirst();
					
					if(toProcess != null) {
						if(p_isApi) m_apiRequests.remove(toProcess);
						else m_htmlRequests.remove(toProcess);
					}
				} catch(NoSuchElementException e) { }
				
				if(toProcess != null) {
					final OsuRequest request = toProcess;
					
					threadManager.executeAsync(new Runnable() {
						public void run() {
							int attempts = 0;
							
							while(attempts < Constants.FIBONACCI.length) {
								try {
									if(p_isApi) stats.addOsuApiRequestSent();
									else stats.addOsuHtmlRequestSent();
									
									request.send(p_isApi);
									
									if(request.getAnswer() instanceof String && ((String) request.getAnswer()).contentEquals("failed"))
										throw new Exception("Request returned fail");
									
									break;
								} catch(Exception e) {
									setStalled(p_isApi, true);
									
									int nextAttemptDelay = Constants.FIBONACCI[attempts] * 1000;
									Log.log(Level.WARNING, "Retrying o!" + (p_isApi ? "api" : "html") + " request in " + nextAttemptDelay + "s\n" +
														   "Request: " + request.getName() + " Ex: " + e.getMessage());
									
									GeneralUtils.sleep(nextAttemptDelay);
								}
								
								attempts++;
							}
							
							setStalled(p_isApi, false);
						}
					}, (int) (request.getTimeout() - (System.currentTimeMillis() - request.getTimeSent())), true);
				}
			}
		}, delay, delay);
	}
	
	private void setStalled(boolean p_isApi, boolean p_stalled) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		if(p_isApi) stats.setOsuApiStalled(p_stalled);
		else stats.setOsuHtmlStalled(p_stalled);
	}
	
	public Object sendRequestSync(OsuRequest p_request, int p_timeout, boolean p_priority) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		sendRequest(p_request, p_timeout, p_priority);
		
		int timeElapsed = 0;
		
		while(p_request.getAnswer() == null) {
			if(timeElapsed >= p_request.getTimeout()) {
				if(p_request.getType() == OsuRequestTypes.API) {
					m_apiRequests.remove(p_request);
					stats.addOsuApiRequestFailed();
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
				return p_request.getAnswer();
			}
		}, p_timeout, true);
	}
	
	private void sendRequest(OsuRequest p_request, int p_timeout, boolean p_priority) {
		ApplicationStats stats = ApplicationStats.getInstance();
		
		p_request.setTimeout(p_timeout > 0 ? p_timeout : 30000);
		
		// assign a type in advance
		if(p_request.getType() == OsuRequestTypes.BOTH) {
			OsuRequestTypes type = OsuRequestTypes.API;
			
			// if we're stalled anywhere, use the other
			if(stats.isOsuApiStalled() && !stats.isOsuHtmlStalled()) type = OsuRequestTypes.HTML;
			else if(stats.getOsuApiLoad() > stats.getOsuHtmlLoad()) type = OsuRequestTypes.HTML;
			
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
		
		if(delay >= 60000) {
			lastLoadRefresh = System.currentTimeMillis();
			lastRefreshApiRequestsCount = stats.getOsuApiRequestsSent();
			lastRefreshHtmlRequestsCount = stats.getOsuHtmlRequestsSent();
		} else {
			int apiRequests = stats.getOsuApiRequestsSent() - lastRefreshApiRequestsCount;
			int htmlRequests = stats.getOsuHtmlRequestsSent() - lastRefreshHtmlRequestsCount;
			double timeSliceMult = 60000.0 / (double) delay;
			
			stats.setOsuApiLoad(((double) apiRequests * timeSliceMult) / (double) Constants.OSU_API_REQUESTS_PER_MINUTE);
			stats.setOsuHtmlLoad(((double) htmlRequests * timeSliceMult) / (double) Constants.OSU_HTML_REQUESTS_PER_MINUTE);
		}
	}
}