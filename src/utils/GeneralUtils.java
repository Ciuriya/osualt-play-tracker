package utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class GeneralUtils {
	
	public static void sleep(int p_milliseconds) {
		try {
			Thread.sleep(p_milliseconds);
		} catch(InterruptedException e) { }
	}
	
	public static int stringToInt(String str){
		try {
			return Integer.parseInt(str);
		} catch(Exception e) {
			return -1;
		}
	}
	
	public static String df(double num, double decimals) {
		String format = "#";
		
		if(decimals > 0) {
			format += ".";
			
			for(int i = 0; i < decimals; i++)
				format += "#";
		}
		
		if(num - (int) num == 0.0) format = "#";
		
		DecimalFormat df = new DecimalFormat(format, new DecimalFormatSymbols(Locale.US));
		df.setNegativePrefix("-");
		
		return df.format(num);
	}
}
