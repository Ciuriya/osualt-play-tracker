package commands;

import data.CommandCategory;
import main.Main;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.DiscordChatUtils;

public class StopCommand extends Command {

	public StopCommand() {
		super(null, true, true, CommandCategory.ADMIN, new String[]{"stop", "restart", "update"}, 
			  "Stops, restarts or updates the bot depending on alias used.", 
			  "Stops, restarts or updates the bot depending on alias used.", 
			  new String[]{"stop", "Stops the bot."},
			  new String[]{"restart", "Restarts the bot."},
			  new String[]{"update", "Updates the bot."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		String alias = p_event.getMessage().getContentRaw().toLowerCase();
		int code = 1;
		
		if(alias.contains("restart")) code = 2;
		else if(alias.contains("update")) code = 3;
		
		DiscordChatUtils.message(p_event.getChannel(), getCodeMessage(code));
		Main.stop(code);
	}
	
	private String getCodeMessage(int p_code) {
		switch(p_code) {
			case 2: return "Restarting...";
			case 3: return "Updating...";
			default: return "Shutting down...";
		}
	}
}
