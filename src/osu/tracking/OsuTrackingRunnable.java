package osu.tracking;

import utils.GeneralUtils;

public class OsuTrackingRunnable extends OsuRefreshRunnable {

	public OsuTrackingRunnable(int p_activityCycle, long p_runnableRefreshDelay) {
		super(p_activityCycle, p_runnableRefreshDelay);
	}

	@Override
	public void refreshUser(OsuTrackedUser p_user) {
		GeneralUtils.sleep(2500);
	}
}
