package utils;

import java.awt.Color;

public class Constants {
	
	// how big the log files get before they stop being used
	public static long MAX_LOG_SIZE = 1048576;
	
	// the global prefix used by the bot
	public static String DEFAULT_PREFIX = "-";
	
	// the size of the thread pool managed by ThreadingManager
	public static int THREAD_POOL_SIZE = 32;
	
	// the default color used on embeds
	public static Color DEFAULT_EMBED_COLOR = Color.CYAN;
	
	// the link to the official support server
	public static String SUPPORT_SERVER_LINK = "https://discord.gg/VZWRZZXcW4";
	
	// the time between activity switches on discord, in seconds
	public static int ACTIVITY_ROTATION_INTERVAL = 600;
	
	// how many osu! api requests can we attempt to send per minute
	public static int OSU_API_REQUESTS_PER_MINUTE = 60;
	
	// how many osu! html scrapes can we attempt per minute
	public static int OSU_HTML_REQUESTS_PER_MINUTE = 50;

	// the fibonacci sequence up to 21
	public static int[] FIBONACCI = new int[] { 1, 1, 2, 3, 5, 8, 13, 21 };
	
}
