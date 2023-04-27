package osu.tracking;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import osu.api.OsuRequestRegulator;
import osu.api.requests.OsuScoresRequest;
import osu.api.requests.OsuUsersRequest;
import utils.Constants;
import utils.OsuUtils;

public class OsuTrackingRunnable extends OsuRefreshRunnable {

	public OsuTrackingRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		super(p_activityCycle, p_runnableRefreshDelay);
	}

	@Override
	public boolean refreshUser(OsuTrackedUser p_user) {
		OsuScoresRequest request = new OsuScoresRequest(p_user.getUserId(), "recent", 
														String.valueOf(Constants.OSU_RECENT_PLAYS_LIMIT), 
														"0", "true");
		Object requestObject = OsuRequestRegulator.getInstance().sendRequestSync(request, 30000, false);

		if(OsuUtils.isAnswerValid(requestObject, JSONArray.class)) {
			JSONArray array = (JSONArray) requestObject;
			
			List<OsuPlay> playsToUpload = new ArrayList<>();
			Timestamp lastFetchedDate = p_user.getLastUpdateTime();
			Timestamp latestPlayDate = p_user.getLastUpdateTime();
			Timestamp lastActiveDate = p_user.getLastActiveTime();
			int addedPlaycount = 0;

			if(!array.isEmpty()) {
				List<OsuPlay> lastCachedPlays = p_user.getCachedLatestPlays(Constants.OSU_CACHED_LATEST_PLAYS_AMOUNT);
				Timestamp oldestCachedPlayTime = latestPlayDate;
				
				if(!lastCachedPlays.isEmpty()) {
					oldestCachedPlayTime = lastCachedPlays.get(lastCachedPlays.size() - 1).getDatePlayed();
				}
				
				for(int i = array.length() - 1; i >= 0; --i) {
					OsuPlay play = new OsuPlay(array.optJSONObject(i));
					Timestamp datePlayed = play.getDatePlayed();
					
					if(datePlayed != null) {
						boolean canUploadPlay = false;
						boolean isPlayUploadable = play.getScoreId() != 0 && play.getBestId() != 0 && !play.getRank().contentEquals("F") && play.canUploadRankedStatus();
						
						if(datePlayed.after(latestPlayDate)) {
							canUploadPlay = true;
						} else if(!lastCachedPlays.isEmpty() && !lastCachedPlays.contains(play) && datePlayed.after(oldestCachedPlayTime) && isPlayUploadable) {
							canUploadPlay = true;
						}
						
						if(canUploadPlay) {
							if(datePlayed.after(lastActiveDate)) ++addedPlaycount;
							
							if(isPlayUploadable)
								playsToUpload.add(play);
						}
						
						lastFetchedDate = datePlayed;
					}
				}
			}
			
			p_user.setLastUpdateTime(lastFetchedDate);
			
			updateUserActivity(p_user, p_user.getPlaycount() + addedPlaycount);
			
			if(!playsToUpload.isEmpty()) {
				OsuPlay.saveToDatabase(playsToUpload);
				OsuTrackUploadManager.getInstance().addPlaysToUpload(playsToUpload);
				p_user.addPlaysToCache(playsToUpload);
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean updateUserActivities(List<OsuTrackedUser> p_users) {
		Map<String, OsuTrackedUser> userIdMap = new HashMap<>();
		
		for(OsuTrackedUser user : p_users) {
			user.setIsFetching(true);
			userIdMap.put(String.valueOf(user.getUserId()), user);
		}
		
		OsuUsersRequest usersRequest = new OsuUsersRequest(userIdMap.keySet().toArray(new String[]{}));
		Object usersObject = OsuRequestRegulator.getInstance().sendRequestSync(usersRequest, 30000, false);
		boolean isValidFetch = false;
		
		if(OsuUtils.isAnswerValid(usersObject, JSONObject.class)) {
			JSONObject usersJson = (JSONObject) usersObject;
			JSONArray usersArray = usersJson.optJSONArray("users");
			
			if(usersArray == null) return false;
			
			for(int i = 0; i < usersArray.length(); ++i) {
				JSONObject userObject = usersArray.optJSONObject(i);
				
				if(userObject != null) {
					OsuTrackedUser user = userIdMap.get(userObject.optString("id"));
					
					if(user != null) {
						JSONObject statisticsObject = userObject.optJSONObject("statistics_rulesets");
						
						if(statisticsObject != null) {
							JSONObject osuObject = statisticsObject.optJSONObject("osu"); // TODO: support other modes
							
							if(osuObject != null) {
								int playcount = osuObject.optInt("play_count");
								
								if(playcount > 0) {
									if(updateUserActivity(user, playcount)) {
										user.forceSetActivityCycle(0);
									} else {
										user.updateActivityCycle();
									}

									user.updateDatabaseEntry();
								}
							}
						}
					}
				}
			}
			
			isValidFetch = true;
		}
		
		for(OsuTrackedUser user : p_users) {
			user.setLastRefreshTime();
			user.setIsFetching(false);
		}
		
		return isValidFetch;
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
