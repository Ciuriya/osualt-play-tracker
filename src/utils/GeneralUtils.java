package utils;

import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class GeneralUtils {
	
	private static final NavigableMap<Long, String> m_suffixes = new TreeMap<>();
	static {
		m_suffixes.put(1_000L, "k");
		m_suffixes.put(1_000_000L, "m");
		m_suffixes.put(1_000_000_000L, "b");
		m_suffixes.put(1_000_000_000_000L, "t");
		m_suffixes.put(1_000_000_000_000_000L, "p");
		m_suffixes.put(1_000_000_000_000_000_000L, "e");
	}
	
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
	
	public static String formatLargeNumber(long value) {
		if(value == Long.MIN_VALUE) return formatLargeNumber(Long.MIN_VALUE + 1);
		if(value < 0) return "-" + formatLargeNumber(-value);
		if(value < 1000) return Long.toString(value);
		
		Entry<Long, String> e = m_suffixes.floorEntry(value);
		Long divideBy = e.getKey();
		String suffix = e.getValue();
		
		long truncated = value / (divideBy / 10);
		return toFormattedNumber(truncated / (double) 10) + suffix;
	}
	
	public static String addCommasToNumber(long number){
		if(number < 1000 && number > -1000) return String.valueOf(number);
		
		String toDisplay = "";
		int remainder = String.valueOf(number).length() % 3;
		int untilSplit = 3;
		
		if(remainder > 0) untilSplit = remainder;
		
		for(char c : String.valueOf(number).toCharArray()){
			toDisplay += String.valueOf(c);
			
			if(untilSplit == 1){
				toDisplay += ",";
				untilSplit = 3;
			}else untilSplit--;
		}
		
		if(toDisplay.endsWith(",")) toDisplay = toDisplay.substring(0, toDisplay.length() - 1);
		return toDisplay;	
	}
	
	public static String addCommasToNumber(double number){
		String[] splitNumber = String.valueOf(number).split("\\.");
		return addCommasToNumber(stringToInt(splitNumber[0])) + "." + splitNumber[1];
	}
}
