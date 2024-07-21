package osu.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import utils.Constants;
import utils.FileUtils;

public class OsuApiManager {

	private static OsuApiManager instance;
	
	// if you need people to give access tokens, give them this link and ask for the code given in the url
	// then add it to the list in login.txt
	// https://osu.ppy.sh/oauth/authorize?client_id=10018&response_type=code&scope=public
	private LinkedList<String> m_accessTokens;
	private LinkedList<String> m_refreshTokens;
	private JSONObject m_loginInfo;
	
	private String m_clientSecret;
	
	public static OsuApiManager getInstance() {
		if(instance == null) instance = new OsuApiManager();
		
		return instance;
	}
	
	public OsuApiManager() {
		m_accessTokens = new LinkedList<>();
		m_refreshTokens = new LinkedList<>();
	}
	
	public void authenticateAllClients(JSONObject p_loginInfo) {
		m_loginInfo = p_loginInfo;
		loadRefreshTokensFromLoginInfo();
		
		m_clientSecret = m_loginInfo.getString("osuApiV2ClientSecret");
		
		for (String refreshToken : m_refreshTokens) {
			try {
				authenticate(refreshToken, -1);
			} catch (Exception e) {}
		}			
		
		try {
			authenticate("", -1);
		} catch (Exception e) {}
		
		Constants.OSU_API_CLIENTS_AUTHENTICATED = m_accessTokens.size();
	}
	
	private void loadRefreshTokensFromLoginInfo() {
		m_refreshTokens.clear();
		
		JSONArray refreshTokenArray = m_loginInfo.getJSONArray("osuApiRefreshTokens");
		
		for (int i = 0; i < refreshTokenArray.length(); ++i) {
			String refreshToken = refreshTokenArray.optString(i);
			
			if (refreshToken.length() > 0)
				m_refreshTokens.add(refreshToken);
		}
	}
	
	private void updateRefreshTokensInLoginInfo() {
		if (m_loginInfo != null) {
			m_loginInfo.put("osuApiRefreshTokens", m_refreshTokens);
			FileUtils.writeToFile(new File("login.txt"), m_loginInfo.toString(), false);
		}
	}
	
	public String sendApiRequest(int p_apiIndex, String p_requestUrl, String[] p_args) throws Exception {
		String url = Constants.OSU_API_ENDPOINT_URL + "v2/" + p_requestUrl;
		
		for(int i = 0; i < p_args.length; ++i) {
			if(i == 0) url += "?";
			else url += "&";
			
			url += p_args[i];
		}
		
		return sendApiPost(p_apiIndex, "GET", url, "");
	}
	
	public void authenticate(String p_refreshToken, int p_apiIndex) throws Exception {
		int apiIndex = p_apiIndex == -1 ? m_accessTokens.size() : p_apiIndex;
		JSONObject bodyJson = new JSONObject();
		
		bodyJson.put("client_id", 10018);
		bodyJson.put("client_secret", m_clientSecret);
		bodyJson.put("grant_type", p_refreshToken.isEmpty() ? "client_credentials" : "refresh_token");
		bodyJson.put("scope", "public");
		
		if(!p_refreshToken.isEmpty()) bodyJson.put("refresh_token", p_refreshToken);
		
		String body = bodyJson.toString();
		String response = sendApiPost(apiIndex, "POST", "https://osu.ppy.sh/oauth/token", body);
		
		if(!response.isEmpty()) {
			JSONObject apiResponse = new JSONObject(response);
		
			String accessToken = apiResponse.optString("access_token", "");
			String refreshToken = apiResponse.optString("refresh_token", "");
			long expiryDelay = apiResponse.optLong("expires_in", 86400);
			long refreshDelay = expiryDelay * 1000;
			
			if(!accessToken.isEmpty()) {
				if (apiIndex == m_accessTokens.size()) m_accessTokens.add(accessToken);
				if (!refreshToken.isEmpty()) {
					m_refreshTokens.set(apiIndex, refreshToken);
					updateRefreshTokensInLoginInfo();
				}
				
				new Timer().schedule(new TimerTask() {
					public void run() {
						try {
							authenticate(refreshToken, apiIndex);
						} catch (Exception e) {}
					}
				}, refreshDelay);
			}
		}
	}
	
	public String fetchRefreshTokenWithCode(String p_code) throws Exception {		
		JSONObject bodyJson = new JSONObject();
	
		bodyJson.put("client_id", 10018);
		bodyJson.put("client_secret", m_clientSecret);
		bodyJson.put("code", p_code);
		bodyJson.put("grant_type", "authorization_code");
		bodyJson.put("scope", "public");
		
		String body = bodyJson.toString();
		String response = sendApiPost(0, "POST", "https://osu.ppy.sh/oauth/token", body);
		
		if(!response.isEmpty()) {
			JSONObject apiResponse = new JSONObject(response);
			return apiResponse.optString("refresh_token", "");
		}
		
		return "";
	}
	
	private String sendApiPost(int p_apiIndex, String p_requestMethod, String p_url, String p_body) throws Exception {
		URL url = new URL(p_url);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod(p_requestMethod);	
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", Integer.toString(p_body.getBytes().length));
		connection.setRequestProperty("x-api-version", "20240529");
		
		if(m_accessTokens.size() > p_apiIndex)
			connection.setRequestProperty("Authorization", "Bearer " + m_accessTokens.get(p_apiIndex));
		
		connection.setDoInput(true);
		connection.setDoOutput(!p_body.isEmpty());

		if(!p_body.isEmpty()) connection.getOutputStream().write(p_body.getBytes("UTF8"));

		try {
			connection.getInputStream();
		} catch (FileNotFoundException e) {
			return "" + connection.getResponseCode();
		}
		
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuffer response = new StringBuffer();
		char[] buffer = new char[1024];
		int charsRead = 0;
		
		while((charsRead = inputStream.read(buffer, 0, 1024)) != -1)
			response.append(buffer, 0, charsRead);
		
		inputStream.close();
		
		return response.toString();
	}
}
