package commands;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import data.CommandCategory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import osu.api.OsuApiManager;
import utils.DiscordChatUtils;
import utils.FileUtils;
import utils.GeneralUtils;

public class OsuApiOutputCommand extends Command {

	public OsuApiOutputCommand() {
		super(null, true, true, CommandCategory.ADMIN, new String[]{"api"}, 
			  "Pings the api and returns the output verbatim.", 
			  "Pings the api and returns the output verbatim.", 
			  new String[]{"api user <userId>", "Returns apiv2 get user call."},
			  new String[]{"api users <userId> <userId> <userId> <userId>...", "Returns apiv2 get users call, limit of 50 user ids."},
			  new String[]{"api scores <userId> <best/recent/first> <limit> <offset> <include fails>", "Returns apiv2 get user scores call."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		if(p_args.length < 2) {
			sendInvalidArgumentsError(p_event.getChannel());
			return;
		}
		
		try {
			String post = "";
			if(p_args[0].equalsIgnoreCase("user")) 
				post = onUserCall(p_event, p_args);
			else if(p_args[0].equalsIgnoreCase("users"))
				post = onUsersCall(p_event, p_args);
			else if(p_args[0].equalsIgnoreCase("scores")) 
				post = onScoresCall(p_event, p_args);
			
			if(!post.isEmpty()) {
				File file = new File("request.txt");
				FileUtils.writeToFile(file, post, false);
				
				p_event.getChannel().sendFiles(FileUpload.fromData(file)).complete();
				
				file.delete();
				return;
			}
		} catch(Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			e.printStackTrace(pw);
			DiscordChatUtils.message(p_event.getChannel(), "Exception: " + e.getMessage() + "\nStack Trace: " + sw.toString());
			return;
		}
		
		DiscordChatUtils.message(p_event.getChannel(), "Probably timed out");
	}
	
	private String onUserCall(MessageReceivedEvent p_event, String[] p_args) throws Exception {
		String userId = URLEncoder.encode(p_args[1], StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		return OsuApiManager.getInstance().sendApiRequest(0, "users/" + userId + "/osu", new String("key=id").split("\\|"));
	}
	
	private String onUsersCall(MessageReceivedEvent p_event, String[] p_args) throws Exception {
		List<String> userIdList = new ArrayList<>();
		
		for(String userId : p_args)
			userIdList.add("ids[]=" + userId);
		
		return OsuApiManager.getInstance().sendApiRequest(0, "users", userIdList.toArray(new String[]{}));
	}
	
	private String onScoresCall(MessageReceivedEvent p_event, String[] p_args) throws Exception {
		if(p_args.length < 3) {
			sendInvalidArgumentsError(p_event.getChannel());
			return null;
		}
		
		String userId = URLEncoder.encode(p_args[1], StandardCharsets.UTF_8);
		String mode = "osu";
		String scoreType = p_args[2];
		
		if(!scoreType.equalsIgnoreCase("best") && !scoreType.equalsIgnoreCase("first")) 
			scoreType = "recent";
		
		String limit = p_args.length >= 4 ? (GeneralUtils.stringToInt(p_args[3]) > 0 ? p_args[3] : "100") : "100";
		String offset = p_args.length >= 5 ? (GeneralUtils.stringToInt(p_args[4]) > 0 ? p_args[4] : "0") : "0";
		String includeFails = p_args.length >= 6 ? (p_args[5].contentEquals("true") ? "1" : "0") : "0";
		
		String requestArgsString = (scoreType.contentEquals("recent") ? "include_fails=" + includeFails : "");
		requestArgsString += "|mode=" + mode;
		requestArgsString += "|limit=" + limit;
		requestArgsString += "|offset=" + offset;
		
		return OsuApiManager.getInstance().sendApiRequest(0, "users/" + userId + "/scores/" + scoreType, requestArgsString.split("\\|"));
	}
}
