package commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import data.CommandCategory;
import data.Log;
import managers.DatabaseManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.Constants;
import utils.DiscordChatUtils;
import utils.GeneralUtils;
import utils.OsuUtils;

public class OsuSetProfileCommand extends Command {

	public OsuSetProfileCommand() {
		super(null, false, true, CommandCategory.OSU, 
			  new String[]{"osuset", "setosu", "setprofile", "osusetprofile", "ign-set", "ignset"}, 
			  "Links osu! profile to discord user.", 
			  "Allows users to link their osu! profile to their discord user to simplify osu!-related commands.", 
			  new String[]{"osuset <osu! user>", "Sets the discord user's profile to the given osu! username.\n" +
			  									 "Example: **`{prefix}osuset im a fancy lad`**"});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] args) {
		if(args.length == 0) {
			sendInvalidArgumentsError(p_event.getChannel());
			return;
		}
		
		String username = "";
		
		for(int i = 0; i < args.length; ++i) {
			username += " " + args[i];
		}
		
		username = username.substring(1);
		
		int fetchedPlayerId = GeneralUtils.stringToInt(OsuUtils.getOsuPlayerIdFromUsernameWithApi(username, true));
		String userDisplay = username + " (<https://osu.ppy.sh/users/" + fetchedPlayerId + ">)";
		
		if(fetchedPlayerId == -1) {
			DiscordChatUtils.message(p_event.getChannel(), "This username is invalid.");
			return;
		}
		
		String discordId = p_event.getAuthor().getId();
		int storedPlayerId = GeneralUtils.stringToInt(OsuUtils.getOsuPlayerIdFromDiscordUserId(discordId));
		
		if(fetchedPlayerId == storedPlayerId) {
			DiscordChatUtils.message(p_event.getChannel(), "Your osu! profile is already set to " + userDisplay);
			return;
		}
		
		String databaseErrorMessage = "A database error occured, please try again later!\n" +
				   					  "If this keeps occuring, make sure to contact Smc#2222 (-Skye on osu!)";
		Connection conn = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME).getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO `osu-user` (`id`, `username`) " +
								   "VALUES (?, ?) ON DUPLICATE KEY UPDATE `username`=?");
			
			st.setInt(1, fetchedPlayerId);
			st.setString(2, username.toLowerCase());
			st.setString(3, username.toLowerCase());
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not insert/update osu! user profile (" + fetchedPlayerId + ") SQL", e);
			DiscordChatUtils.message(p_event.getChannel(), databaseErrorMessage);
			return;
		}
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO `discord-user` (`id`, `osu-id`) " +
								   "VALUES (?, ?) ON DUPLICATE KEY UPDATE `osu-id`=?");
			
			st.setString(1, p_event.getAuthor().getId());
			st.setInt(2, fetchedPlayerId);
			st.setInt(3, fetchedPlayerId);
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not insert/update discord info (" + fetchedPlayerId + ") SQL", e);
			DiscordChatUtils.message(p_event.getChannel(), databaseErrorMessage);
			return;
		}
		
		DiscordChatUtils.message(p_event.getChannel(), "Your osu! profile was set to " + userDisplay);
	}
}
