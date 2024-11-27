package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

import data.Database;
import data.Log;
import managers.ApplicationStats;
import managers.DatabaseManager;
import osu.api.LegacyMods;
import osu.api.Mods;
import utils.Constants;
import utils.GeneralUtils;

public class OsuTrackUploadManager {

	private static OsuTrackUploadManager instance;
	
	private List<OsuPlay> m_playsToUpload;
	
	public static OsuTrackUploadManager getInstance() {
		if(instance == null) instance = new OsuTrackUploadManager();
		
		return instance;
	}
	
	public OsuTrackUploadManager() {
		m_playsToUpload = new ArrayList<>();
		m_playsToUpload.addAll(OsuPlay.getPlaysToUpload());
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if(!m_playsToUpload.isEmpty())
					uploadQueuedPlays();
			}
		}, 5000, Constants.OSU_PLAY_UPLOAD_INTERVAL * 1000);
		
		//tempUploadLocalDiscordOsuUserLinks();
		/*
		ThreadingManager.getInstance().executeAsync(new Runnable() {
			public void run() {
				fixMissingPPValuesInDB();
			}
		}, 999999999, false);
		*/
	}
	
	public void addPlaysToUpload(List<OsuPlay> p_plays) {
		m_playsToUpload.addAll(p_plays);
	}

	/*
	private void tempUploadLocalDiscordOsuUserLinks() {
		Database localDb = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Database remoteDb = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection localConn = localDb.getConnection();
		Connection remoteConn = remoteDb.getConnection();

		try {
			PreparedStatement localSt = localConn.prepareStatement(
			  		 					"SELECT * FROM `discord-user`");
			PreparedStatement remoteSt = remoteConn.prepareStatement(
					   					 "INSERT INTO discorduser (discord_id, user_id, username) " +
					   					 "VALUES (?, ?, ?) ON CONFLICT (discord_id) DO UPDATE SET user_id=?, username=?");
			
			ResultSet rs = localSt.executeQuery();
	
			while(rs.next()) {
				PreparedStatement usernameFetchSt = localConn.prepareStatement(
													"SELECT `username` FROM `osu-user` WHERE `id`=?");
	
				usernameFetchSt.setInt(1, rs.getInt(2));
				
				ResultSet usernameFetchRs = usernameFetchSt.executeQuery();
				String username = "";
				if(usernameFetchRs.next()) username = usernameFetchRs.getString(1);
				
				usernameFetchRs.close();
				usernameFetchSt.close();

				remoteSt.setString(1, rs.getString(1));
				remoteSt.setInt(2, rs.getInt(2));
				remoteSt.setString(3, username);
				remoteSt.setInt(4, rs.getInt(2));
				remoteSt.setString(5, username);
				
				remoteSt.addBatch();
			}
			
			rs.close();
			localSt.close();
			
			remoteSt.executeBatch();
			remoteSt.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "import blew up uh oh", e);
		} finally {
			localDb.closeConnection(localConn);
			remoteDb.closeConnection(remoteConn);
		}
	}
	*/
	
	/*
	private void fixMissingPPValuesInDB() {
		Database trackerDB = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Database osualtDB = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection trackerConn = trackerDB.getConnection();
		Connection osualtConn = osualtDB.getConnection();

		List<OsuPlay> localPlays = new ArrayList<>();
		try {
			PreparedStatement localPlaySt = trackerConn.prepareStatement("SELECT * FROM `osu-play` WHERE `pp`=0 ORDER BY `date_played` ASC");
			ResultSet rs = localPlaySt.executeQuery();

			while(rs.next()) {
				OsuPlay localPlay = new OsuPlay(rs);

				if(!localPlay.isUploaded()) continue;
				
				localPlays.add(localPlay);
			}
			
			rs.close();
			localPlaySt.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to load missing pp value plays from db", e);
		} finally {
			trackerDB.closeConnection(trackerConn);
			osualtDB.closeConnection(osualtConn);
		}
		
		while(!localPlays.isEmpty()) {
			List<OsuPlay> subset = localPlays.subList(0, localPlays.size() > 50 ? 50 : localPlays.size());
			
			if(updateFailedPlaysInOsuAlt(subset))
				localPlays.removeAll(subset);
			else break;
		}
	}
	
	private boolean updateFailedPlaysInOsuAlt(List<OsuPlay> p_plays) {
		Database trackerDB = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Database osualtDB = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection trackerConn = trackerDB.getConnection();
		Connection osualtConn = osualtDB.getConnection();
		boolean result = false;
		
		try {
			PreparedStatement osualtSt = osualtConn.prepareStatement("UPDATE scores SET score = ?, count300 = ?, count100 = ?, count50 = ?, countmiss = ?, combo = ?, perfect = ?, enabled_mods = ?, date_played = ?, rank = ?, " +
					 												 "pp = ?, replay_available = ?, is_hd = ?, is_hr = ?, is_dt = ?, is_fl = ?, is_ht = ?, is_ez = ?, is_nf = ?, is_nc = ?, is_td = ?, is_so = ?, is_sd = ?, is_pf = ? " +
					 												 "WHERE user_id = ? AND beatmap_id = ?");
			PreparedStatement localPlayUpdateSt = trackerConn.prepareStatement("UPDATE `osu-play` SET `pp`=? WHERE `score_id`=?");
			
			Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
			for(OsuPlay localPlay : p_plays) {
				String post = OsuApiManager.getInstance().sendApiRequest("beatmaps/" + localPlay.getBeatmapId() + "/scores/users/" + localPlay.getUserId() + "/all", new String[]{});
				JSONArray scores = new JSONObject(post).optJSONArray("scores");
				
				if(scores != null) {
					for(int i = 0; i < scores.length(); ++i) {
						OsuPlay play = new OsuPlay(scores.getJSONObject(i));
						
						if(play.getPP() != 0.0 && 
						   localPlay.getEnabledMods() == play.getEnabledMods() && 
						   localPlay.getScore() == play.getScore() &&
						   localPlay.getAccuracy() == play.getAccuracy()) {
							List<Mods> mods = Mods.getModsFromBit(play.getEnabledMods());
							
							osualtSt.setLong(1, play.getScore());
							osualtSt.setInt(2, play.getCount300());
							osualtSt.setInt(3, play.getCount100());
							osualtSt.setInt(4, play.getCount50());
							osualtSt.setInt(5, play.getCountMiss());
							osualtSt.setInt(6, play.getCombo());
							osualtSt.setInt(7, play.isPerfect() ? 1 : 0);
							osualtSt.setString(8, play.getEnabledMods() + "");
							osualtSt.setTimestamp(9, play.getDatePlayed(), calendar);
							osualtSt.setString(10, play.getRank());
							osualtSt.setDouble(11, play.getPP());
							osualtSt.setInt(12, play.isReplayAvailable() ? 1 : 0);
							osualtSt.setBoolean(13, mods.contains(Mods.Hidden));
							osualtSt.setBoolean(14, mods.contains(Mods.HardRock));
							osualtSt.setBoolean(15, mods.contains(Mods.DoubleTime));
							osualtSt.setBoolean(16, mods.contains(Mods.Flashlight));
							osualtSt.setBoolean(17, mods.contains(Mods.HalfTime));
							osualtSt.setBoolean(18, mods.contains(Mods.Easy));
							osualtSt.setBoolean(19, mods.contains(Mods.NoFail));
							osualtSt.setBoolean(20, mods.contains(Mods.Nightcore));
							osualtSt.setBoolean(21, mods.contains(Mods.TouchDevice));
							osualtSt.setBoolean(22, mods.contains(Mods.SpunOut));
							osualtSt.setBoolean(23, mods.contains(Mods.SuddenDeath));
							osualtSt.setBoolean(24, mods.contains(Mods.Perfect));
							osualtSt.setInt(25, GeneralUtils.stringToInt(localPlay.getUserId()));
							osualtSt.setLong(26, localPlay.getBeatmapId());
							localPlayUpdateSt.setDouble(1, play.getPP());
							localPlayUpdateSt.setLong(2, localPlay.getScoreId());
							
							osualtSt.addBatch();
							localPlayUpdateSt.addBatch();
						}
					}
				}
				
				GeneralUtils.sleep(3000);
			}
			
			
			int[] osualtOutputValues = osualtSt.executeBatch();
			int[] localOutputValues = localPlayUpdateSt.executeBatch();
			int osualtUpdatedValues = 0;
			int localUpdatedValues = 0;
			
			for(int output : osualtOutputValues)
				if(output == 1) 
					osualtUpdatedValues++;
			
			for(int output : localOutputValues)
				if(output == 1) 
					localUpdatedValues++;

			osualtSt.close();
			localPlayUpdateSt.close();

			Log.log(Level.INFO, "Updated " + osualtUpdatedValues + " missing pp values in db (" + (osualtOutputValues.length - osualtUpdatedValues) + " failed to update into osualt)\n" + 
								"Tracker had " + p_plays.size() + " 0pp plays locally, " + localUpdatedValues + " were updated.");
			result = true;
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to update missing pp values", e);
		} finally {
			trackerDB.closeConnection(trackerConn);
			osualtDB.closeConnection(osualtConn);
		}
		
		return result;
	}
	*/
	
	public void uploadQueuedPlays() {
		Database db = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO scores VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
								   "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
								   "ON CONFLICT ON CONSTRAINT scores_pkey DO UPDATE SET " + 
								   "score = EXCLUDED.score, count300 = EXCLUDED.count300, " +
								   "count100 = EXCLUDED.count100, count50 = EXCLUDED.count50, " + 
								   "countmiss = EXCLUDED.countmiss, combo = EXCLUDED.combo, " + 
								   "perfect = EXCLUDED.perfect, enabled_mods = EXCLUDED.enabled_mods, " + 
								   "date_played = EXCLUDED.date_played, rank = EXCLUDED.rank, " + 
								   "pp = EXCLUDED.pp, replay_available = EXCLUDED.replay_available, " + 
								   "is_hd = EXCLUDED.is_hd, is_hr = EXCLUDED.is_hr, is_dt = EXCLUDED.is_dt, " + 
								   "is_fl = EXCLUDED.is_fl, is_ht = EXCLUDED.is_ht, is_ez = EXCLUDED.is_ez, " + 
								   "is_nf = EXCLUDED.is_nf, is_nc = EXCLUDED.is_nc, is_td = EXCLUDED.is_td, " + 
								   "is_so = EXCLUDED.is_so, is_sd = EXCLUDED.is_sd, is_pf = EXCLUDED.is_pf " + 
								   "WHERE EXCLUDED.score > scores.score");
			
			PreparedStatement modSt = conn.prepareStatement(
									  "INSERT INTO scoresmods (user_id, beatmap_id, mods, date_played, statistics, maximum_statistics) " +
									  "VALUES (?, ?, ?::jsonb, ?, ?::jsonb, ?::jsonb) " +
									  "ON CONFLICT ON CONSTRAINT scoresmods_pkey DO UPDATE SET " +
									  "mods = EXCLUDED.mods, date_played = EXCLUDED.date_played");
			
			List<OsuPlay> excluded = new ArrayList<>();
			Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
			
			for(OsuPlay play : m_playsToUpload) {
				if(play.getCount300() == 0 && play.getCount100() == 0 && play.getCount50() == 0 && play.getCountMiss() == 0) {
					excluded.add(play);
					continue;
				}
				
				List<Mods> newMods = Mods.getModsFromJson(play.getMods());
				long legacyModsBit = LegacyMods.getBitFromNewMods(newMods);
				List<LegacyMods> mods = LegacyMods.getModsFromBit(legacyModsBit);
				
				st.setInt(1, GeneralUtils.stringToInt(play.getUserId()));
				st.setLong(2, play.getBeatmapId());
				st.setLong(3, play.getScore());
				st.setInt(4, play.getCount300());
				st.setInt(5, play.getCount100());
				st.setInt(6, play.getCount50());
				st.setInt(7, play.getCountMiss());
				st.setInt(8, play.getCombo());
				st.setInt(9, play.isPerfect() ? 1 : 0);
				st.setLong(10, legacyModsBit);
				st.setTimestamp(11, play.getDatePlayed(), calendar);
				st.setString(12, play.getRank());
				st.setDouble(13, play.getPP());
				st.setInt(14, play.isReplayAvailable() ? 1 : 0);
				st.setBoolean(15, mods.contains(LegacyMods.Hidden));
				st.setBoolean(16, mods.contains(LegacyMods.HardRock));
				st.setBoolean(17, mods.contains(LegacyMods.DoubleTime));
				st.setBoolean(18, mods.contains(LegacyMods.Flashlight));
				st.setBoolean(19, mods.contains(LegacyMods.HalfTime));
				st.setBoolean(20, mods.contains(LegacyMods.Easy));
				st.setBoolean(21, mods.contains(LegacyMods.NoFail));
				st.setBoolean(22, mods.contains(LegacyMods.Nightcore));
				st.setBoolean(23, mods.contains(LegacyMods.TouchDevice));
				st.setBoolean(24, mods.contains(LegacyMods.SpunOut));
				st.setBoolean(25, mods.contains(LegacyMods.SuddenDeath));
				st.setBoolean(26, mods.contains(LegacyMods.Perfect));

				st.addBatch();
			}
			
			int[] output = st.executeBatch();
			st.close();

			m_playsToUpload.removeAll(excluded);
			
			for(int i = 0; i < output.length; ++i) {
				OsuPlay play = m_playsToUpload.get(i);
				
				if(output[i] > 0 || output[i] == PreparedStatement.SUCCESS_NO_INFO) {
					modSt.setInt(1, GeneralUtils.stringToInt(play.getUserId()));
					modSt.setLong(2, play.getBeatmapId());
					modSt.setString(3, play.getMods());
					modSt.setTimestamp(4, play.getDatePlayed(), calendar);
					modSt.setString(5, play.getStatistics());
					modSt.setString(6, play.getMaximumStatistics());
					
					modSt.addBatch();
				}
			}
			
			modSt.executeBatch();
			
			if(output.length != m_playsToUpload.size()) {
				new Timer().schedule(new TimerTask() {
					public void run() {
						uploadQueuedPlays();
					}
				}, 5000);
			} else {
				ApplicationStats.getInstance().addScoresUploaded(m_playsToUpload.size());
				OsuPlay.setUploaded(m_playsToUpload.stream().collect(Collectors.toList()), output);
				
				m_playsToUpload.clear();
			}
			
			modSt.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to upload " + m_playsToUpload.size() + " plays to the remote database", e);
		} finally {
			db.closeConnection(conn);
		}
	}
}
