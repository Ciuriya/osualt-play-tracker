package osu.tracking;

import org.json.JSONObject;

import osu.api.OsuRequestRegulator;
import osu.api.requests.OsuUserRequest;
import utils.OsuUtils;

public class OsuActivityRunnable extends OsuRefreshRunnable {
	
	public OsuActivityRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		super(p_activityCycle, p_runnableRefreshDelay);
	}

	@Override
	public void refreshUser(OsuTrackedUser p_user) {
		OsuUserRequest userRequest = new OsuUserRequest(String.valueOf(p_user.getUserId()), "0");
		Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 30000, false);

		if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
			JSONObject userJson = (JSONObject) userObject;
			int updatedPlaycount = userJson.optInt("playcount", 0);
			
			if(p_user.getPlaycount() < updatedPlaycount) {
				p_user.setPlaycount(updatedPlaycount);
				p_user.setLastActiveTime();
			}
		}
	}
}
