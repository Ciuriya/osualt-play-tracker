package osu.api;

import java.util.ArrayList;
import java.util.List;

public enum LegacyMods {
	
	None(0, "NM"), 
	NoFail(1, "NF"), 
	Easy(2, "EZ"),
	TouchDevice(4, "TD"),
	Hidden(8, "HD"), 
	HardRock(16, "HR"), 
	SuddenDeath(32, "SD"), 
	DoubleTime(64, "DT"),
	Relax(128, "RL"), 
	HalfTime(256, "HT"), 
	Nightcore(512, "NC"), 
	Flashlight(1024, "FL"), 
	Autoplay(2048, "AU"),
	SpunOut(4096, "SO"), 
	Autopilot(8192, "AP"),
	Perfect(16384, "PF"),
	Key4(32768, "4K"),
	Key5(65536, "5K"),
	Key6(131072, "6K"),
	Key7(262144, "7K"),
	Key8(524288, "8K"),
	FadeIn(1048576, "FI"),
	Random(2097152, "RA"),
	Cinema(4194304, "CN"),
	Target(8388608, "TP"),
	Key9(16777216, "9K"),
	Key10(33554432, "10K"),
	Key1(67108864, "1K"),
	Key3(134217728, "3K"),
	Key2(268435456, "2K"),
	ScoreV2(536870912, "V2"),
	Mirror(1073741824, "MR");
	
	long m_bit;
	String m_shortName;
	
	LegacyMods(long p_bit, String p_shortName) {
		this.m_bit = p_bit;
		this.m_shortName = p_shortName;
	}
	
	public long getBit() {
		return m_bit;
	}
	
	public String getShortName() {
		return m_shortName;
	}
	
	public static long getBitFromShortNames(List<String> p_modShortNames) {
		long bits = 0;
		
		for(String sMod : p_modShortNames)
			for(LegacyMods mod : LegacyMods.values())
				if(mod.getShortName().equalsIgnoreCase(sMod) ||
					mod.name().equalsIgnoreCase(sMod))
					bits += mod.getBit();
			
		
		return bits;
	}

	public static long getBitFromString(String sMods){
		long bits = 0;
		
		for(String sMod : sMods.split(","))
			for(LegacyMods mod : LegacyMods.values())
				if(mod.getShortName().equalsIgnoreCase(sMod) ||
					mod.name().equalsIgnoreCase(sMod))
					bits += mod.getBit();
			
		
		return bits;
	}
	
	public static List<LegacyMods> getModsFromBit(long p_modsUsed){
		List<LegacyMods> mods = new ArrayList<>();
		long used = p_modsUsed;
		
		if(used == 0) return mods;
		
		for(long i = 1073741824; i >= 1; i /= 2) {
			LegacyMods mod = LegacyMods.getMod(i);
			
			if(used >= i) {
				mods.add(mod);
				used -= i;
			}
		}
		
		if(mods.contains(LegacyMods.None)) mods.remove(LegacyMods.None);
		
		return mods;
	}
	
	public static long getBitFromMods(List<LegacyMods> p_mods) {
		long modsUsed = 0;
		
		for(LegacyMods mod : p_mods)
			modsUsed += mod.getBit();
		
		return modsUsed;
	}
	
	public static String getModDisplay(List<LegacyMods> p_mods) {
		String display = "";
		
		List<LegacyMods> displayMods = new ArrayList<>(p_mods);
		
		if(displayMods.contains(LegacyMods.Nightcore))
			displayMods.remove(LegacyMods.DoubleTime);
		
		if(displayMods.contains(LegacyMods.Perfect))
			displayMods.remove(LegacyMods.SuddenDeath);
			
		for(LegacyMods mod : displayMods)
			display = mod.getShortName() + display;
		
		return display.length() == 0 ? "" : "+" + display;
	}
	
	public static LegacyMods getMod(long p_bit) {
		for(LegacyMods mod : LegacyMods.values())
			if(mod.getBit() == p_bit) return mod;
		
		return LegacyMods.None;
	}
	
	public static long getBitFromNewMods(List<Mods> newMods) {
		long modsUsed = 0;
		
		for(Mods mod : newMods)
			modsUsed += getBitFromString(mod.getAcronym());
		
		return modsUsed;
	}
}