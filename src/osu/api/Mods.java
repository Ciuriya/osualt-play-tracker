package osu.api;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public enum Mods{
	
	None(""),
	Easy("EZ"),
	NoFail("NF"),
	HalfTime("HT"),
	Daycore("DC"),
	HardRock("HR"),
	SuddenDeath("SD"),
	Perfect("PF"),
	DoubleTime("DT"),
	Nightcore("NC"),
	Hidden("HD"),
	Flashlight("FL"),
	Blinds("BL"),
	StrictTracking("ST"),
	AccuracyChallenge("AC"),
	TargetPractice("TP"),
	DifficultyAdjust("DA"),
	Classic("CL"),
	Random("RD"),
	Mirror("MR"),
	Alternate("AL"),
	SingleTap("SG"),
	Autoplay("AT"),
	Cinema("CN"),
	Relax("RX"),
	Autopilot("AP"),
	SpunOut("SO"),
	Transform("TR"),
	Wiggle("WG"),
	SpinIn("SI"),
	Grow("GR"),
	Deflate("DF"),
	WindUp("WU"),
	WindDown("WD"),
	Traceable("TC"),
	BarrelRoll("BR"),
	ApproachDifferent("AD"),
	Muted("MU"),
	NoScope("NS"),
	Magnetised("MG"),
	Repel("RP"),
	AdaptiveSpeed("AS"),
	FreezeFrame("FR"),
	Bubbles("BU"),
	Synesthesia("SY"),
	Depth("DP"),
	TouchDevice("TD"),
	ScoreV2("SV2");
	
	String m_acronym;
	
	Mods(String p_acronym) {
		this.m_acronym = p_acronym;
	}
	
	public String getAcronym() {
		return m_acronym;
	}
	
	public static List<Mods> getModsFromJson(String arrayStr) {
		return getModsFromJson(new JSONArray(arrayStr));
	}
	
	public static List<Mods> getModsFromJson(JSONArray array) {
		List<Mods> mods = new ArrayList<Mods>();
		
		if (array == null || array.length() == 0) return mods;
		
		for(int i = 0; i < array.length(); ++i) {
			JSONObject modObj = array.optJSONObject(i);
			
			if (modObj != null && modObj.has("acronym"))
				mods.add(getMod(modObj.getString("acronym")));
		}
		
		return mods;
	}
	
	public static Mods getMod(String p_acronym) {
		for(Mods mod : Mods.values())
			if(mod.getAcronym().contentEquals(p_acronym)) return mod;
		
		return Mods.None;
	}
	
	public static String getModDisplay(List<Mods> p_mods) {
		String display = "";
		
		List<Mods> displayMods = new ArrayList<>(p_mods);
		
		if(displayMods.contains(Mods.Nightcore))
			displayMods.remove(Mods.DoubleTime);
		
		if(displayMods.contains(Mods.Perfect))
			displayMods.remove(Mods.SuddenDeath);
			
		for(Mods mod : displayMods)
			display = mod.getAcronym() + display;
		
		return display.length() == 0 ? "" : "+" + display;
	}
}