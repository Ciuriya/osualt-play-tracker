package data;

import java.util.concurrent.Callable;

import managers.ApplicationStats;
import managers.ThreadingManager;
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
