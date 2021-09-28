package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import data.Log;

public class TimeUtils {
	
	public static String toDuration(long p_time, boolean p_displayMs){
		long millis = p_time;
		
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);
        
        String display = "";
        
        if(days > 0) display += days + "d";
        if(hours > 0) display += hours + "h";
        if(minutes > 0) display += minutes + "m";
        if(seconds > 0) display += seconds + "s";
        if(millis > 0 && p_displayMs) display += millis + "ms";
        
        if(display.isEmpty()) display = p_displayMs ? "0ms" : "0s";
        
        return display;
	}
	
	public static String toDate(long time) {
		return toDate(time, "yyyy-MM-dd HH:mm:ss");
	}
	
	public static String toDate(long time, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
		
		calendar.setTimeInMillis(time);
		
		sdf.setCalendar(calendar);
		sdf.setTimeZone(Constants.DEFAULT_TIMEZONE);
		
		return sdf.format(time);
	}
	
	public static long toTime(String date){
		return toTime(date, "yyyy-MM-dd HH:mm:ss");
	}
	
	public static long toTime(String date, String format){
		long time = -1;
		
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(Constants.DEFAULT_TIMEZONE);
		
		try{
			time = sdf.parse(date).getTime();
		}catch(ParseException e){
			Log.log(Level.SEVERE, e.getMessage(), e);
		}
		
		return time;
	}
	
	public static long timezoneOffsetToTime(String p_timezoneOffset) {
		String cleanOffsetString = p_timezoneOffset.replace(":", "");
		
		int hours = 0;
		int minutes = 0;
		for(int i = 0; i < cleanOffsetString.length(); ++i) {
			char digitCharacter = cleanOffsetString.charAt(i);
			int digit = Integer.parseInt(String.valueOf(digitCharacter));
			
			if(i < 2) {
				int mult = (1 - i) * 10;
				hours += digit * (mult == 0 ? 1 : mult);
			} else {
				int mult = (3 - i) * 10;
				minutes += digit * (mult == 0 ? 1 : mult);
			}
		}
		
		return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes);
	}
}
