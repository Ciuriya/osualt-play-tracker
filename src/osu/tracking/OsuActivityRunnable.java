package osu.tracking;

import java.util.logging.Level;

import org.json.JSONObject;

import data.Log;
import osu.api.OsuRequestRegulator;
import osu.api.requests.OsuUserRequest;
import utils.GeneralUtils;
import utils.OsuUtils;

public class OsuActivityRunnable extends OsuRefreshRunnable {
	
	public OsuActivityRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		super(p_activityCycle, p_runnableRefreshDelay);
	}
	
	@Override
	public void run() {
		super.run();
		
		try {
			for(;;) {
				if(m_usersToRefresh.size() == 0) break;
	
				OsuTrackedUser user = m_usersToRefresh.removeFirst();
				int usersLeft = m_usersToRefresh.size();
				long currentTime = System.currentTimeMillis();
				long nextDelay = (m_lastStartTime + m_runnableRefreshDelay - currentTime) / (usersLeft == 0 ? 1 : usersLeft);
	
				if(!user.isFetching()) {
					user.setIsFetching(true);
	
					OsuUserRequest userRequest = new OsuUserRequest(String.valueOf(user.getUserId()), "0");
					Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 30000, false);
	
					if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
						JSONObject userJson = (JSONObject) userObject;
						int updatedPlaycount = userJson.optInt("playcount", 0);
						
						if(user.getPlaycount() < updatedPlaycount) {
							user.setPlaycount(updatedPlaycount);
							user.setLastActiveTime();
							user.setLastUpdateTime();
							user.updateDatabaseEntry();
						}
					}
					
					user.updateActivityCycle();
					user.setIsFetching(false);
				}
				
				long updatedDelay = nextDelay - (System.currentTimeMillis() - currentTime);
	
				if(updatedDelay > 0) {
					GeneralUtils.sleep((int) updatedDelay);
				}
			}
		} catch(Exception e) {
			Log.log(Level.SEVERE, "osu!activity runnable exception", e);
		}
		
		long time = System.currentTimeMillis();
		long expectedEndTime = m_lastStartTime + m_runnableRefreshDelay;
		if(expectedEndTime > time) {
			GeneralUtils.sleep((int) (expectedEndTime - time));
		}
		
		callStart();
	}
}
