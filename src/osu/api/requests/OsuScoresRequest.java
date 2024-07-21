package osu.api.requests;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;

import osu.api.OsuApiManager;
import osu.api.OsuRequest;
import osu.api.OsuRequestTypes;

public class OsuScoresRequest extends OsuRequest {

	public OsuScoresRequest(String... p_arguments){
		super("scores", OsuRequestTypes.API, p_arguments);
	}

	@Override
	public void send(int p_apiIndex) throws Exception {
		String[] args = getArguments();

		if(args.length < 1) {
			setAnswer("invalid arguments");
			return;
		}
		
		String userId = URLEncoder.encode(args[0], StandardCharsets.UTF_8);
		String mode = "osu";
		String scoreType = args.length >= 2 ? args[1] : "recent";
		String limit = args.length >= 3 ? args[2] : "100";
		String offset = args.length >= 4 ? args[3] : "0";
		String includeFails = args.length >= 5 ? (args[4].contentEquals("true") ? "1" : "0") : "0";
		
		String requestArgsString = (scoreType.contentEquals("recent") ? "include_fails=" + includeFails : "");
		requestArgsString += "|mode=" + mode;
		requestArgsString += "|limit=" + limit;
		requestArgsString += "|offset=" + offset;
		
		String post = OsuApiManager.getInstance().sendApiRequest(p_apiIndex, "users/" + userId + "/scores/" + scoreType, requestArgsString.split("\\|"));
		
		if(!checkForEmptyPost(post, userId)) {
			setAnswer(new JSONArray(post));
		}
	}
}
