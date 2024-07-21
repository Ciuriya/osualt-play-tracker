package commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import data.CommandCategory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import osu.api.OsuApiManager;
import utils.DiscordChatUtils;

public class OsuConvertOAuthCodeToRefreshTokenCommand extends Command {

	public OsuConvertOAuthCodeToRefreshTokenCommand() {
		super(null, true, true, CommandCategory.ADMIN, new String[]{"oauth"}, 
			  "Converts a one-time api authorization code to a refresh token that can be used as an api key", 
			  "Converts a one-time api authorization code to a refresh token that can be used as an api key", 
			  new String[]{"oauth <code>", "Returns refresh token given by the api"});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		if(p_args.length < 1) {
			sendInvalidArgumentsError(p_event.getChannel());
			return;
		}
		
		try {
			String refreshToken = OsuApiManager.getInstance().fetchRefreshTokenWithCode(p_args[0]);
			DiscordChatUtils.message(p_event.getChannel(), "Refresh token: " + refreshToken);
		} catch(Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			e.printStackTrace(pw);
			DiscordChatUtils.message(p_event.getChannel(), "Exception: " + e.getMessage() + "\nStack Trace: " + sw.toString());
			return;
		}
	}
}
