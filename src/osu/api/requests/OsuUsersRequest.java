package osu.api.requests;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import osu.api.OsuApiManager;
import osu.api.OsuRequest;
import osu.api.OsuRequestTypes;

public class OsuUsersRequest extends OsuRequest {

	public OsuUsersRequest(String... p_arguments) {
		super("users", OsuRequestTypes.API, p_arguments);
	}

	@Override
	public void send(int p_apiIndex) throws Exception {
		String[] args = getArguments();

		if(args.length < 1 || args.length > 50) {
			setAnswer("invalid arguments");
			return;
		}

		sendApi(p_apiIndex, args);
	}

	private void sendApi(int p_apiIndex, String[] p_args) throws Exception {
		List<String> userIdList = new ArrayList<>();
		
		for(String userId : p_args)
			userIdList.add("ids[]=" + userId);
		
		String post = OsuApiManager.getInstance().sendApiRequest(p_apiIndex, "users", userIdList.toArray(new String[]{}));
		
		setAnswer(new JSONObject(post));
	}
}
