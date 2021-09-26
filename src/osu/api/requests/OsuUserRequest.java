package osu.api.requests;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import osu.api.OsuRequest;
import osu.api.OsuRequestTypes;
import utils.Constants;
import utils.HtmlUtils;

public class OsuUserRequest extends OsuRequest {

	public OsuUserRequest(String... p_arguments) {
		super("user", OsuRequestTypes.BOTH, p_arguments);
	}

	public OsuUserRequest(OsuRequestTypes p_type, String... p_arguments) {
		super("user", p_type, p_arguments);
	}

	@Override
	public void send(boolean p_api) throws Exception {
		String[] args = getArguments();

		if(args.length < 2) {
			setAnswer("invalid arguments");
			return;
		}

		if(p_api)
			sendApi(args);
		else
			sendHtml(args);
	}

	private void sendApi(String[] p_args) throws Exception {
		String type = "id";

		if(p_args.length >= 3) {
			type = p_args[2];
		}

		String post = HtmlUtils.sendPost(Constants.OSU_API_ENDPOINT_URL, "get_user?k=" + Constants.OSU_API_KEY + 
																		 "&u=" + URLEncoder.encode(p_args[0], StandardCharsets.UTF_8) +
																		 "&m=" + p_args[1] +
																		 "&type=" + type +
																		 "&event_days=1", "");
		
		if(post.isBlank() || !post.contains("{"))
			setAnswer("failed");
		else {
			setAnswer(new JSONObject(post));
		}
	}

	private void sendHtml(String[] p_args) throws Exception {
		String[] htmlPage = HtmlUtils.getHTMLPage("https://osu.ppy.sh/pages/include/profile-general.php?u=" + p_args[0] + "&m=" + p_args[1]);
		JSONObject user = new JSONObject();
		user.put("user_id", p_args[0]);
		user.put("general_page", htmlPage);

		String ppLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "Performance</a>: ");
		String userLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "&find=");
		String countryRankLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 3, "&find=");
		String rankedScoreLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Ranked Score</b>");
		String totalScoreLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Total Score</b>");
		String accLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Hit Accuracy</b>");
		String playCountLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Play Count</b>");
		String playTimeLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Play Time</b>");
		String levelLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Current Level</b>");
		String levelPercentageLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "levelPercent");
		String hitsLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Total Hits</b>");
		String maxComboLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Maximum Combo</b>");
		String kudosuLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "Kudosu</a> Earned</b>");
		String replaysLine = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "<b>Replays Watched by Others</b>");
		String SSline = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "images/X.png'>");
		String Sline = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "images/S.png'>");
		String Aline = HtmlUtils.getFirstMatchingLineFromHtmlPage(htmlPage, 0, "images/A.png'>");
		
		try {
			if(!ppLine.isEmpty()) {
				if(ppLine.contains("pp")) {
					user.put("pp_raw", ppLine.split("Performance<\\/a>: ")[1].split("pp ")[0].replaceAll(",", ""));
					user.put("pp_rank", ppLine.split("pp \\(#")[1].split("\\)<\\/b>")[0].replaceAll(",", ""));
				}
			}
			
			if(!userLine.isEmpty()) {
				if(userLine.contains("&find=")) {
					user.put("username", userLine.split("&find=")[1].split("&")[0]);
					user.put("country", userLine.split("&c=")[1].split("&find=")[0]);
				}
			}
			
			if(!countryRankLine.isEmpty())
				if(userLine.contains("&find="))
					user.put("pp_country_rank", countryRankLine.substring(1).replaceAll(",", ""));
			
			if(!rankedScoreLine.isEmpty())
				user.put("ranked_score",
						rankedScoreLine.split("Ranked Score<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(!totalScoreLine.isEmpty())
				user.put("total_score",
						totalScoreLine.split("Total Score<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(!accLine.isEmpty())
				user.put("accuracy", accLine.split("Hit Accuracy<\\/b>: ")[1].split("%<\\/div>")[0]);
			
			if(!playCountLine.isEmpty())
				user.put("playcount", playCountLine.split("Play Count<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(!playTimeLine.isEmpty())
				user.put("playtime", playTimeLine.split("Play Time<\\/b>: ")[1].split(" hours<\\/div>")[0].replaceAll(",", ""));
			
			if(!levelLine.isEmpty() && !levelPercentageLine.isEmpty()) {
				String level = levelLine.split("Current Level<\\/b>: ")[1].split("<\\/div>")[0];
				level += "." + levelPercentageLine.split("align=right>")[1].split("%")[0];
				user.put("level", level);
			}
			
			if(!hitsLine.isEmpty())
				user.put("total_hits", hitsLine.split("Total Hits<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(!maxComboLine.isEmpty())
				user.put("max_combo", maxComboLine.split("Maximum Combo<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(!kudosuLine.isEmpty())
				user.put("kudosu_earned", kudosuLine.split("Earned<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(!replaysLine.isEmpty())
				user.put("replays_watched", replaysLine.split("Replays Watched by Others<\\/b>: ")[1].split(" times<\\/div>")[0].replaceAll(",", ""));
			
			if(!SSline.isEmpty())
				user.put("count_rank_ss", SSline.split("<td width='50'>")[1].split("<\\/td>")[0].replaceAll(",", ""));
			
			if(!Sline.isEmpty())
				user.put("count_rank_s", Sline.split("<td width='50'>")[1].split("<\\/td>")[0].replaceAll(",", ""));
			
			if(!Aline.isEmpty())
				user.put("count_rank_a", Aline.split("<td width='50'>")[1].split("<\\/td>")[0].replaceAll(",", ""));
		} catch (Exception e) { }

		setAnswer(user);
	}
}
