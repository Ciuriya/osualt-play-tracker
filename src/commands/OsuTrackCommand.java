package commands;

import data.CommandCategory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import osu.tracking.OsuRefreshRunnable;
import osu.tracking.OsuTrackedUser;
import osu.tracking.OsuTrackingManager;
import utils.DiscordChatUtils;
import utils.OsuUtils;

public class OsuTrackCommand extends Command {

	public OsuTrackCommand() {
		super(null, false, true, CommandCategory.OSU, new String[]{"osutrack", "track"}, 
			  "Lets you manually go into live tracking.", 
			  "Allows users to skip the activity refresh and jump straight into the live tracking cycle.",
			  new String[]{"osutrack", "Moves the osu! player linked to the discord user using this command to the live tracking cycle."},
			  new String[]{"osutrack <osu! name>", "Moves the given osu! player to the live tracking cycle.\n" +
												   "Example: **`{prefix}osutrack im a super blue cat`**"});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		String userId = getUserIdFromArgsSimple(p_event, p_args);
		
		if(userId.isEmpty()) return;
		
		OsuTrackingManager osuTrackManager = OsuTrackingManager.getInstance();
		OsuTrackedUser user = osuTrackManager.getUser(userId);
		
		if(user == null) {
			DiscordChatUtils.message(p_event.getChannel(), "This osu! player isn't registered!");
			return;
		}
		
		OsuRefreshRunnable refreshRunnable = osuTrackManager.getRefreshRunnable(user.getActivityCycle());
		
		if(refreshRunnable != null)
			refreshRunnable.removeUser(userId);
		
		user.setLastActiveTime();
		user.forceSetActivityCycle(0);
		
		DiscordChatUtils.message(p_event.getChannel(), "Moved " + OsuUtils.getOsuPlayerUsernameFromIdWithApi(userId, true) + 
				   									   " to the live tracking cycle!");
		
		if(!user.updateDatabaseEntry())
			DiscordChatUtils.message(p_event.getChannel(), "An error occured while updating the database, please tell Smc#2222 (-Skye on osu!)");
	}
}
