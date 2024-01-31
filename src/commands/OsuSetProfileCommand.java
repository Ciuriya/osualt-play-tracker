package commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import data.CommandCategory;
import data.Database;
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
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		if(p_args.length == 0) {
			sendInvalidArgumentsError(p_event.getChannel());
			return;
		}
		
		String username = "";
		
		for(int i = 0; i < p_args.length; ++i) {
			username += " " + p_args[i];
		}
		
		username = username.substring(1);
		
		int fetchedPlayerId = GeneralUtils.stringToInt(OsuUtils.getOsuPlayerIdFromUsernameWithApi(username, true));
		String userDisplay = username + " (<https://osu.ppy.sh/users/" + fetchedPlayerId + ">)";
		
		if(fetchedPlayerId == -1) {
			DiscordChatUtils.message(p_event.getChannel(), "This username is invalid, make sure you use your username with spaces!");
			return;
		}
		
		String discordId = p_event.getAuthor().getId();
		int storedPlayerId = GeneralUtils.stringToInt(OsuUtils.getOsuPlayerIdFromDiscordUserId(discordId));
		
		if(fetchedPlayerId == storedPlayerId) {
			DiscordChatUtils.message(p_event.getChannel(), "Your osu! profile is already set to " + userDisplay);
			return;
		}
		
		String databaseErrorMessage = "A database error occured, please try again later!\n" +
					  				  "If this keeps occuring, make sure to contact Ciuriya";
		
		if(updateSql(p_event, fetchedPlayerId, username.toLowerCase()))
			DiscordChatUtils.message(p_event.getChannel(), "Your osu! profile was set to " + userDisplay);
		else DiscordChatUtils.message(p_event.getChannel(), databaseErrorMessage);
	}
	
	private boolean updateSql(MessageReceivedEvent p_event, int fetchedPlayerId, String username) {
		Database localDb = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection localConn = localDb.getConnection();
		
		try {
			PreparedStatement st = localConn.prepareStatement(
								   "INSERT INTO `osu-user` (`id`, `username`) " +
								   "VALUES (?, ?) ON DUPLICATE KEY UPDATE `username`=?");
			
			st.setInt(1, fetchedPlayerId);
			st.setString(2, username);
			st.setString(3, username);
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			localDb.closeConnection(localConn);
			
			Log.log(Level.SEVERE, "Could not insert/update osu! user profile (" + fetchedPlayerId + ") SQL", e);
			return false;
		}
		
		try {
			PreparedStatement st = localConn.prepareStatement(
								   "INSERT INTO `discord-user` (`id`, `osu-id`) " +
								   "VALUES (?, ?) ON DUPLICATE KEY UPDATE `osu-id`=?");
			
			st.setString(1, p_event.getAuthor().getId());
			st.setInt(2, fetchedPlayerId);
			st.setInt(3, fetchedPlayerId);
			
			st.executeUpdate();
			st.close();
			
			localDb.closeConnection(localConn);
		} catch(Exception e) {
			localDb.closeConnection(localConn);
			
			Log.log(Level.SEVERE, "Could not insert/update discord info (" + fetchedPlayerId + ") SQL", e);
			return false;
		}

		Database remoteDb = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection remoteConn = remoteDb.getConnection();
		
		try {
			PreparedStatement st = remoteConn.prepareStatement(
								   "INSERT INTO discorduser (discord_id, user_id, username) " +
								   "VALUES (?, ?, ?) ON CONFLICT (discord_id) DO UPDATE SET user_id=?, username=?");
			
			st.setString(1, p_event.getAuthor().getId());
			st.setInt(2, fetchedPlayerId);
			st.setString(3, username);
			st.setInt(4, fetchedPlayerId);
			st.setString(5, username);
			
			st.executeUpdate();
			st.close();
			
			remoteDb.closeConnection(remoteConn);
		} catch(Exception e) {
			remoteDb.closeConnection(remoteConn);
			
			Log.log(Level.SEVERE, "Could not insert/update remote db with user profile info (" + fetchedPlayerId + ") SQL", e);
			return false;
		}
		
		return true;
	}
}
