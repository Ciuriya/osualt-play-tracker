package osu.tracking;

import org.json.JSONObject;

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
		
		for(;;) {
			if(m_usersToRefresh.size() == 0) break;

			OsuTrackedUser user = m_usersToRefresh.removeFirst();
			int usersLeft = m_usersToRefresh.size();
			long currentTime = System.currentTimeMillis();
			long nextDelay = (currentTime + m_runnableRefreshDelay - m_lastStartTime) / (usersLeft == 0 ? 1 : usersLeft);

			if(!user.isFetching()) {
				user.setIsFetching(true);

				OsuUserRequest userRequest = new OsuUserRequest(String.valueOf(user.getUserId()), "0");
				Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 30000, false);

				if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
					JSONObject userJson = (JSONObject) userObject;
					int updatedPlaycount = userJson.has("playcount") ? userJson.getInt("playcount") : 0;
					
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
		
		long time = System.currentTimeMillis();
		long expectedEndTime = m_lastStartTime + m_runnableRefreshDelay ;
		if(expectedEndTime > time) {
			GeneralUtils.sleep((int) (expectedEndTime - time));
		}
		
		callStart();
	}
}
