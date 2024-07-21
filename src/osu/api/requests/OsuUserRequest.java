package osu.api.requests;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.json.JSONObject;

import data.Log;
import osu.api.OsuApiManager;
import osu.api.OsuRequest;
import osu.api.OsuRequestTypes;
import utils.GeneralUtils;
import utils.HtmlUtils;

public class OsuUserRequest extends OsuRequest {

	public OsuUserRequest(String... p_arguments) {
		super("user", OsuRequestTypes.BOTH, p_arguments);
	}

	public OsuUserRequest(OsuRequestTypes p_type, String... p_arguments) {
		super("user", p_type, p_arguments);
	}

	@Override
	public void send(int p_apiIndex) throws Exception {
		String[] args = getArguments();

		if(args.length < 2) {
			setAnswer("invalid arguments");
			return;
		}

		if(p_apiIndex >= 0)
			sendApi(p_apiIndex, args);
		else
			sendHtml(args);
	}

	private void sendApi(int p_apiIndex, String[] p_args) throws Exception {
		String type = "id";

		if(p_args.length >= 3) {
			type = p_args[2];
		}
		
		String userId = URLEncoder.encode(p_args[0], StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		String mode = "osu";
		String requestArgsString = "key=" + type;
		String post = OsuApiManager.getInstance().sendApiRequest(p_apiIndex, "users/" + userId + "/" + mode, requestArgsString.split("\\|"));
		
		if(!checkForEmptyPost(post, userId)) {
			setAnswer(new JSONObject(post));
		}
	}

	private void sendHtml(String[] p_args) throws Exception {
		String[] htmlPage = HtmlUtils.getHTMLPage("https://osu.ppy.sh/pages/include/profile-general.php?u=" + p_args[0] + "&m=" + p_args[1]);
		JSONObject user = new JSONObject();
		user.put("id", GeneralUtils.stringToInt(p_args[0]));
		user.put("general_page", htmlPage);

		if(!HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "No information recorded").isEmpty()) {
			setAnswer("restricted");
			return;
		}
		
		String userLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "&find=");
		String playCountLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Play Count</b>");
		
		try {
			if(!userLine.isEmpty()) {
				if(userLine.contains("&find=")) {
					user.put("username", userLine.split("&find=")[1].split("&")[0]);
					user.put("country", userLine.split("&c=")[1].split("&find=")[0]);
				}
			}

			JSONObject statisticsObject = new JSONObject();

			if(!playCountLine.isEmpty())
				statisticsObject.put("play_count", playCountLine.split("Play Count<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			user.put("statistics", statisticsObject);
		} catch (Exception e) { 
			Log.log(Level.SEVERE, "Error parsing user html", e);
			setAnswer("failed");
			return;
		}

		setAnswer(user);
	}
}
