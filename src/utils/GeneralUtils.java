package utils;

import java.text.DecimalFormat;

public class GeneralUtils {
	
	public static void sleep(int p_milliseconds) {
		try {
			Thread.sleep(p_milliseconds);
		} catch(InterruptedException e) { }
	}
	
	public static int stringToInt(String p_str){
		try {
			return Integer.parseInt(p_str);
		} catch(Exception e) {
			return -1;
		}
	}
	
	public static String toFormattedNumber(double num) {
        DecimalFormat format = new DecimalFormat("#.##");
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
        
        return format.format(num);
	}
}
