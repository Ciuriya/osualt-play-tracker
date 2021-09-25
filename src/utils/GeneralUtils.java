package utils;

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
}
