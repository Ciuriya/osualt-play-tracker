package osu.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONObject;

import data.Log;
import utils.Constants;

public class OsuApiManager {

	private static OsuApiManager instance;
	
	private String m_accessToken = "";
	
	public static OsuApiManager getInstance() {
		if(instance == null) instance = new OsuApiManager();
		
		return instance;
	}
	
	public OsuApiManager() {}
	
	public String sendApiRequest(String p_requestUrl, String[] p_args) {
		String url = Constants.OSU_API_ENDPOINT_URL + "v2/" + p_requestUrl;
		
		for(int i = 0; i < p_args.length; ++i) {
			if(i == 0) url += "?";
			else url += "&";
			
			url += p_args[i];
		}
		
		return sendApiPost("GET", url, "");
	}
	
	public void authenticate(String p_clientSecret) {
		JSONObject bodyJson = new JSONObject();
		
		bodyJson.put("client_id", 10018);
		bodyJson.put("client_secret", p_clientSecret);
		bodyJson.put("grant_type", "client_credentials");
		bodyJson.put("scope", "public");
		
		String body = bodyJson.toString();
		String response = sendApiPost("POST", "https://osu.ppy.sh/oauth/token", body);
		
		if(!response.isEmpty()) {
			JSONObject apiResponse = new JSONObject(response);
		
			m_accessToken = apiResponse.optString("access_token", "");
			long expiryDelay = apiResponse.optLong("expires_in", 86400);
			long refreshDelay = expiryDelay * 1000;
			
			new Timer().schedule(new TimerTask() {
				public void run() {
					authenticate(p_clientSecret);
				}
			}, refreshDelay);
		}
		
		if(!m_accessToken.isEmpty())
			Log.log(Level.INFO, "Authenticated with o!api v2 successfully!");
		else Log.log(Level.INFO, "Failed to authenticate with o!api v2!");
	}
	
	private String sendApiPost(String p_requestMethod, String p_url, String p_body) {
		try {
			URL url = new URL(p_url);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	
			connection.setRequestMethod(p_requestMethod);	
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(10000);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("charset", "utf-8");
			connection.setRequestProperty("Content-Length", Integer.toString(p_body.getBytes().length));
			
			if(!m_accessToken.isEmpty())
				connection.setRequestProperty("Authorization", "Bearer " + m_accessToken);
			
			connection.setDoInput(true);
			connection.setDoOutput(!p_body.isEmpty());
	
			if(!p_body.isEmpty()) connection.getOutputStream().write(p_body.getBytes("UTF8"));

			BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer response = new StringBuffer();
			char[] buffer = new char[1024];
			int charsRead = 0;
			
			while((charsRead = inputStream.read(buffer, 0, 1024)) != -1)
				response.append(buffer, 0, charsRead);
			
			inputStream.close();
			
			return response.toString();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "o!api v2 post error", e);
		}
		
		return "";
	}
}
