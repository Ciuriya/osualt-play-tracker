package utils;

import java.awt.Color;
import java.util.TimeZone;

public class Constants {
	
	// how big the log files get before they stop being used
	public static final long MAX_LOG_SIZE = 1048576;
	
	// the global prefix used by the bot
	public static final String DEFAULT_PREFIX = "-";
	
	// the size of the thread pool managed by ThreadingManager
	public static final int THREAD_POOL_SIZE = 32;
	
	// the tracking database's name
	public static final String TRACKER_DATABASE_NAME = "osualt-tracker";
	
	// the osu!alt db name
	public static final String OSUALT_REMOTE_DB_NAME = "osu";
	
	// the osu!alt db url
	public static final String OSUALT_REMOTE_DB_URL = "postgresql://home.smcmax.com:5432";
	
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
	
	// interval between osu registered user refreshes
	public static final int OSU_REGISTERED_USER_REFRESH_INTERVAL = 600;
	
	// activity cycles mapped with the cutoff in seconds before you switch to the next cycle
	// and the refresh frequency of the cycle
	public static final long[][] OSU_ACTIVITY_CYCLES = new long[][] {
		new long[] {1800, 0},        // 30m / not needed
		new long[] {7200, 900},      // 2h  / 15m
		new long[] {43800, 1800},    // 12h / 30m
		new long[] {86400, 2700},    // 1d  / 45m
		new long[] {172800, 3600},   // 2d  / 1h
		new long[] {259200, 5400},   // 3d  / 1h30m
		new long[] {604800, 7200},   // 7d  / 2h
		new long[] {1209600, 10800}, // 14d / 3h
		new long[] {2592000, 14400}  // 31d / 4h
	};
	
	// the osu!api's endpoint url
	public static final String OSU_API_ENDPOINT_URL = "https://osu.ppy.sh/api/";
	
	// the default timezone used everywhere to do time comparisons
	public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC");
	
	// how many plays can the bot fetch from the api in one call
	public static final int OSU_RECENT_PLAYS_LIMIT = 100;
	
	// amount of latest plays to cache per user
	public static final int OSU_CACHED_LATEST_PLAYS_AMOUNT = 5;
	
	// time in seconds before plays are uploaded to the remote server
	public static final int OSU_PLAY_UPLOAD_INTERVAL = 60;
	
	// time between pruning calls
	public static final int OSU_PLAY_PRUNING_LOOP_INTERVAL = 3600;
	
	// days before a play is DELETED from the db
	public static final int OSU_PLAY_PRUNE_DELAY = 7;
}
