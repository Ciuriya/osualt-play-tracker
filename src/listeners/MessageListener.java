package listeners;

import java.util.logging.Level;

import commands.Command;
import data.Log;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Constants;
import utils.DiscordChatUtils;

public class MessageListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent p_event) {
		if(p_event.getAuthor().isBot()) return;
		
		String message = p_event.getMessage().getContentRaw();
		
		// if we have a prefix, strip it, otherwise it's not a command (unless it's a dm)
		if(message.startsWith(Constants.DEFAULT_PREFIX))
			message = message.substring(Constants.DEFAULT_PREFIX.length());
		else if(p_event.isFromGuild()) return;
		
		// find and run command
		if(Command.handleCommand(p_event, message)) {
			Log.log(Level.INFO, "{Command received in " + DiscordChatUtils.getChannelLogString(p_event.getChannel()) + 
					" sent by " + p_event.getAuthor().getId() + "\n" +
					message);
		}
	}
}
