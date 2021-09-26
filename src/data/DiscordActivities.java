package data;

import java.util.concurrent.Callable;

import managers.ApplicationStats;
import managers.ThreadingManager;
import osu.tracking.OsuRefreshRunnable;
import osu.tracking.OsuTrackingManager;
import utils.TimeUtils;

public enum DiscordActivities {
	
	UPTIME(new Callable<String>() {
		public String call() {
			return "for " + TimeUtils.toDuration(ApplicationStats.getInstance().getUptime(), false);
		}
	}),
	TRACKED(new Callable<String>() {
		public String call() {
			return "with " + OsuTrackingManager.getInstance().getLoadedRegisteredUsers() + " osu! players";
		}
	}),
	REFRESH(new Callable<String>() {
		public String call() {
			OsuRefreshRunnable trackingRunnable = OsuTrackingManager.getInstance().getRefreshRunnable(0);
			
			if(trackingRunnable != null) {
				long averageRefreshDelay = trackingRunnable.getAverageUserRefreshDelay();
				
				if(averageRefreshDelay > 0) {
					long averageLoopLength = trackingRunnable.getInitialUserListSize() * averageRefreshDelay;
					return "tracking every " + TimeUtils.toDuration(averageLoopLength, false);
				}
			}
			
			return "Loading tracking...";
		}
	});
	
	private Callable<String> m_activityFetchingCallable;
	
	private DiscordActivities(Callable<String> p_activityFetchingCallable) {
		m_activityFetchingCallable = p_activityFetchingCallable;
	}

	public String getActivity() {
		String activity = ThreadingManager.getInstance().executeSync(m_activityFetchingCallable, 5000);
		
		return activity == null ? "" : activity;
	}
}
