package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import data.Log;

public class HtmlUtils {

	public static String[] getHTMLPage(String p_url) {
		BufferedReader reader = null;
		String[] htmlPage = new String[]{};
		
		try{
			URL url = new URL(p_url.replaceAll(" ", "%20"));
			StringBuffer page = new StringBuffer();
			
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Accept-Language", "en-US");
			
			if(p_url.contains("ppy.sh")) connection.addRequestProperty("Cookie", "osu_site_v=old");
			
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			
			char[] buffer = new char[1024];
			int charsRead = 0;
			
			while((charsRead = reader.read(buffer, 0, 1024)) != -1)
				page.append(buffer, 0, charsRead);
			
			htmlPage = page.toString().split("\\n");
		} catch(Exception e) {
			Log.log(Level.INFO, "getHTML Exception: " + e.getMessage());
		} finally {
			try {
				if(reader != null) reader.close();
			} catch(Exception e) {
				Log.log(Level.WARNING, "Could not close getHTML BufferedReader: " + e.getMessage());
			}
		}
		
		return htmlPage;
	}
	
	public static String sendPost(String p_url, String p_params, String p_query) throws Exception {
		String answer = "";
		
		try {
			URL url = new URL(p_url + p_params);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");	
			connection.setConnectTimeout(2500);
			connection.setReadTimeout(2500);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("charset", "utf-8");
			connection.setRequestProperty("Content-Length", "" + Integer.toString(p_params.getBytes().length));
			connection.setDoOutput(true);
			
			if(p_query.length() > 0) connection.getOutputStream().write(p_query.getBytes("UTF8"));
			
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer response = new StringBuffer();
			char[] buffer = new char[1024];
			int charsRead = 0;
			
			while((charsRead = inputStream.read(buffer, 0, 1024)) != -1)
				response.append(buffer, 0, charsRead);
			
			inputStream.close();
			
			response = response.deleteCharAt(0);
			response = response.deleteCharAt(response.length() - 1);
			
			answer = response.toString();
		} catch(Exception e) {
			throw e;
		}
		
		return answer;
	}
	
	public static String getFirstMatchingLineFromHtmlPage(String[] p_htmlPage, int p_offsetLines, String... p_matchers) {
		for(int i = 0; i < p_htmlPage.length; ++i)
			for(String matcher : p_matchers)
				if(p_htmlPage[i].contains(matcher)) {
					try {
						return p_htmlPage[i + p_offsetLines];
					} catch(Exception e) {
						Log.log(Level.WARNING, "getNextLineFromHtmlPage error: " + e.getMessage());
					}
					
					break;
				}
		
		return "";
	}
	
	public static List<String> getAllMatchingLinesFromHtmlPage(String[] p_htmlPage, int p_offsetLines, String... p_matchers) {
		List<String> matchedLines = new ArrayList<String>();
		
		for(int i = 0; i < p_htmlPage.length; ++i)
			for(String matcher : p_matchers)
				if(p_htmlPage[i].contains(matcher)) {
					try {
						matchedLines.add(p_htmlPage[i + p_offsetLines]);
					} catch(Exception e) {
						Log.log(Level.WARNING, "getNextLineFromHtmlPage error: " + e.getMessage());
					}
				}
		
		return matchedLines;
	}
}
