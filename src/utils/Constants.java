package utils;

import java.awt.Color;

public class Constants {
	
	// how big the log files get before they stop being used
	public static final long MAX_LOG_SIZE = 1048576;
	
	// the global prefix used by the bot
	public static final String DEFAULT_PREFIX = "-";
	
	// the size of the thread pool managed by ThreadingManager
	public static final int THREAD_POOL_SIZE = 32;
	
	// the tracking database's name
	public static final String TRACKER_DATABASE_NAME = "osualt-tracker";
	
	// the default color used on embeds
	public static final Color DEFAULT_EMBED_COLOR = Color.CYAN;
	
	// the default footer used throughout the bot
	public static final String DEFAULT_FOOTER = "Made by Smc#2222 (-Skye on osu!)";
	
	// the link to the official support server
	public static final String SUPPORT_SERVER_LINK = "https://discord.gg/VZWRZZXcW4";
	
	// the time between activity switches on discord, in seconds
	public static final int ACTIVITY_ROTATION_INTERVAL = 600;
	
	public static String OSU_API_KEY;
	
	// how many osu! api requests can we attempt to send per minute
	public static final int OSU_API_REQUESTS_PER_MINUTE = 60;
	
	// how many osu! html scrapes can we attempt per minute
	public static final int OSU_HTML_REQUESTS_PER_MINUTE = 50;

	// the fibonacci sequence up to 21
	public static final int[] FIBONACCI = new int[] { 1, 1, 2, 3, 5, 8, 13, 21 };
	
	public static final String OSU_API_ENDPOINT_URL = "https://osu.ppy.sh/api/";
	
}
