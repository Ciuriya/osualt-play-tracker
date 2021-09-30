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
import managers.DatabaseManager;
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
	}
	
	public void addPlaysToUpload(List<OsuPlay> p_plays) {
		m_playsToUpload.addAll(p_plays);
	}
	
	public void uploadQueuedPlays() {
		Database db = DatabaseManager.getInstance().get(Constants.OSUALT_REMOTE_DB_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO scores VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
								   "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
								   "ON CONFLICT ON CONSTRAINT scores_pkey DO UPDATE SET " + 
								   "score = excluded.score, count300 = EXCLUDED.count300, " +
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
			
			List<OsuPlay> excluded = new ArrayList<>();
			Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
			
			for(OsuPlay play : m_playsToUpload) {
				if(play.getCount300() == 0 && play.getCount100() == 0 && play.getCount50() == 0 && play.getCountMiss() == 0) {
					excluded.add(play);
					continue;
				}
				
				List<Mods> mods = Mods.getModsFromBit(play.getEnabledMods());
				
				st.setInt(1, GeneralUtils.stringToInt(play.getUserId()));
				st.setLong(2, play.getBeatmapId());
				st.setLong(3, play.getScore());
				st.setInt(4, play.getCount300());
				st.setInt(5, play.getCount100());
				st.setInt(6, play.getCount50());
				st.setInt(7, play.getCountMiss());
				st.setInt(8, play.getCombo());
				st.setInt(9, play.isPerfect() ? 1 : 0);
				st.setLong(10, play.getEnabledMods());
				st.setTimestamp(11, play.getDatePlayed(), calendar);
				st.setString(12, play.getRank());
				st.setDouble(13, play.getPP());
				st.setInt(14, play.isReplayAvailable() ? 1 : 0);
				st.setBoolean(15, mods.contains(Mods.Hidden));
				st.setBoolean(16, mods.contains(Mods.HardRock));
				st.setBoolean(17, mods.contains(Mods.DoubleTime));
				st.setBoolean(18, mods.contains(Mods.Flashlight));
				st.setBoolean(19, mods.contains(Mods.HalfTime));
				st.setBoolean(20, mods.contains(Mods.Easy));
				st.setBoolean(21, mods.contains(Mods.NoFail));
				st.setBoolean(22, mods.contains(Mods.Nightcore));
				st.setBoolean(23, mods.contains(Mods.TouchDevice));
				st.setBoolean(24, mods.contains(Mods.SpunOut));
				st.setBoolean(25, mods.contains(Mods.SuddenDeath));
				st.setBoolean(26, mods.contains(Mods.Perfect));

				st.addBatch();
			}
			
			st.executeBatch();
			st.close();

			m_playsToUpload.removeAll(excluded);
			
			OsuPlay.setUploaded(m_playsToUpload.stream().collect(Collectors.toList()));
			m_playsToUpload.clear();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to upload " + m_playsToUpload.size() + " plays to the remote database", e);
		} finally {
			db.closeConnection(conn);
		}
	}
}
