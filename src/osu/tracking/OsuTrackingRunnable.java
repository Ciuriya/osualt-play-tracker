package osu.tracking;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import osu.api.OsuRequestRegulator;
import osu.api.OsuRequestTypes;
import osu.api.requests.OsuScoresRequest;
import osu.api.requests.OsuUserRequest;
import utils.Constants;
import utils.OsuUtils;

public class OsuTrackingRunnable extends OsuRefreshRunnable {

	public OsuTrackingRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		super(p_activityCycle, p_runnableRefreshDelay);
	}

	@Override
	public boolean refreshUser(OsuTrackedUser p_user) {
		if(getActivityCycle() == 0) {
			OsuScoresRequest request = new OsuScoresRequest(p_user.getUserId(), "recent", 
															String.valueOf(Constants.OSU_RECENT_PLAYS_LIMIT), 
															"0", p_user.justMovedCycles() ? "false" : "true");
			Object requestObject = OsuRequestRegulator.getInstance().sendRequestSync(request, 30000, true);
			//boolean isMe = p_user.getUserId().contentEquals("4886116");

			if(OsuUtils.isAnswerValid(requestObject, JSONArray.class)) {
				JSONArray array = (JSONArray) requestObject;
				
				List<OsuPlay> playsToUpload = new ArrayList<>();
				Timestamp lastFetchedDate = p_user.getLastUpdateTime();
				Timestamp latestPlayDate = p_user.getLastUpdateTime();
				int addedPlaycount = 0;

				if(!array.isEmpty()) {
					for(int i = array.length() - 1; i >= 0; --i) {
						OsuPlay play = new OsuPlay(array.optJSONObject(i));
						Timestamp datePlayed = play.getDatePlayed();
						
						//if(isMe) Log.log(Level.INFO, "datePlayed: " + datePlayed);
						
						if(datePlayed != null && datePlayed.after(latestPlayDate)) {
							++addedPlaycount;
							
							//if(isMe) Log.log(Level.INFO, "added pc");
							
							if(play.getScoreId() != 0 && play.getBestId() != 0 && !play.getRank().contentEquals("F") && play.canUploadRankedStatus())
								playsToUpload.add(play);
						}
						
						if(datePlayed != null) lastFetchedDate = datePlayed;
					}
				}
				
				p_user.setLastUpdateTime(lastFetchedDate);
				
				if(p_user.justMovedCycles())
					updateUserPlaycount(p_user);
				else updateUserActivity(p_user, p_user.getPlaycount() + addedPlaycount);
				
				//if(isMe) Log.log(Level.INFO, "added: " + addedPlaycount + " new pc: " + p_user.getPlaycount());
				
				if(!playsToUpload.isEmpty()) {
					OsuPlay.saveToDatabase(playsToUpload);
					OsuTrackUploadManager.getInstance().addPlaysToUpload(playsToUpload);
					p_user.addPlaysToCache(playsToUpload);
				}
				
				return true;
			}
			
			if(p_user.justMovedCycles())
				return updateUserPlaycount(p_user);
			
			return false;
		} 
		
		return updateUserPlaycount(p_user);
	}
	
	private boolean updateUserPlaycount(OsuTrackedUser p_user) {
		OsuUserRequest userRequest = new OsuUserRequest(OsuRequestTypes.HTML, String.valueOf(p_user.getUserId()), "0");
		Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 30000, false);

		if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
			JSONObject userJson = (JSONObject) userObject;
			JSONObject statisticsObject = userJson.optJSONObject("statistics");
			
			return updateUserActivity(p_user, statisticsObject != null ? statisticsObject.optInt("play_count", 0) : 0);
		}
		
		return false;
	}
	
	private boolean updateUserActivity(OsuTrackedUser p_user, int p_playcount) {
		if(p_user.getPlaycount() < p_playcount) {
			p_user.setPlaycount(p_playcount);
			p_user.setLastActiveTime();
			
			return true;
		} else if(p_playcount < p_user.getPlaycount()) {
			p_user.setPlaycount(p_playcount);
			
			return true;
		}
		
		return false;
	}
}
