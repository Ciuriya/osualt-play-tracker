package main;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONObject;

import commands.Command;
import data.Log;
import listeners.MessageListener;
import managers.ApplicationStats;
import managers.DatabaseManager;
import managers.DiscordActivityManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import osu.api.OsuRequestRegulator;
import utils.FileUtils;
import utils.TimeUtils;

public class Main {
	
	public static JDA discordApi; // PLEASE avoid using this if you can
	
	public static void main(String[] p_args) {
		new Main();
	}

	public Main() {
		init();
	}
	
	private void init() {
		ApplicationStats stats = ApplicationStats.getInstance(); // setup the application stat collector
		Log.init(""); // setup the logging system
		FileUtils.writeToFile(new File("codes.txt"), "0", false); // update the wrapper
		
		Log.log(Level.INFO, "Setting up database...");
		
		// connect to the sql database
		JSONObject loginInfo = new JSONObject(FileUtils.readFile(new File("login.txt")));
		DatabaseManager.getInstance().setup("discord", "jdbc:mysql://localhost/osualt-tracker", 
											loginInfo.getString("sqlUser"), loginInfo.getString("sqlPass"));
		
		Log.log(Level.INFO, "Logging into discord...");
		
		// log into discord
		try {
			discordApi = JDABuilder.createDefault(loginInfo.getString("discordToken"))
						 .addEventListeners(new MessageListener())
						 .build();
			
			discordApi.awaitReady();
			
			Log.log(Level.INFO, "Discord logged in!");
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not initialize the JDA object", e);
		}
		
		Log.log(Level.INFO, "Setting up osu!");
		
		OsuRequestRegulator.getInstance();
		
		Log.log(Level.INFO, "Setting up commands...");
		
		Command.registerCommands();
		DiscordActivityManager.getInstance(); // start the activity cycling
		
		long startupTime = stats.getUptime();
		stats.setStartupTime(startupTime);
		
		Log.log(Level.INFO, "Startup complete! Startup time: " + 
				TimeUtils.toDuration(startupTime, true));
	}
	
	public static void stop(int p_code) {
		DatabaseManager.getInstance().close(); // close databases
		FileUtils.writeToFile(new File("codes.txt"), String.valueOf(p_code), false); // update the wrapper
		
		discordApi.shutdown(); // log out of discord
		
		// delayed shutdown to give everything time to close
		new Timer().schedule(new TimerTask() {
			public void run() {
				System.exit(0);
			}
		}, 1000);
	}
}
