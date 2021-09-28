package osu.tracking;

import org.json.JSONObject;

import osu.api.OsuRequestRegulator;
import osu.api.requests.OsuUserRequest;
import utils.GeneralUtils;
import utils.OsuUtils;

public class OsuTrackingRunnable extends OsuRefreshRunnable {

	public OsuTrackingRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		super(p_activityCycle, p_runnableRefreshDelay);
	}

	@Override
	public void refreshUser(OsuTrackedUser p_user) {
		if(getActivityCycle() == 0) {
			GeneralUtils.sleep(2500);
			
			//if(p_user.justMovedCycles() && has 100 recent plays somehow)
			/*
			OsuScoresRequest request = new OsuScoresRequest(p_user.getUserId(), "recent", "100", "0", "false");
			Object requestObject = OsuRequestRegulator.getInstance().sendRequestSync(request, 30000, true);
			
			if(OsuUtils.isAnswerValid(requestObject, JSONArray.class)) {
				JSONArray array = (JSONArray) requestObject;
				
				Log.log(Level.INFO, "total: " + array.length() + " beatmap id: " + array.getJSONObject(array.length() - 1).getJSONObject("beatmap").optInt("id", 0));
			}
			*/
		} else {
			OsuUserRequest userRequest = new OsuUserRequest(String.valueOf(p_user.getUserId()), "0");
			Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 30000, false);
	
			if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
				JSONObject userJson = (JSONObject) userObject;
				JSONObject statisticsObject = userJson.optJSONObject("statistics");
				
				updateUserActivity(p_user, statisticsObject != null ? statisticsObject.optInt("play_count", 0) : 0);
			}
		}
	}
	
	private void updateUserActivity(OsuTrackedUser p_user, int p_playcount) {
		if(p_user.getPlaycount() < p_playcount) {
			p_user.setPlaycount(p_playcount);
			p_user.setLastActiveTime();
		}
	}
}
