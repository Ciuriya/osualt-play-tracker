package osu.api;

import java.util.ArrayList;
import java.util.List;

public enum Mods{
	
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
	
	Mods(int p_bit, String p_shortName) {
		this.m_bit = p_bit;
		this.m_shortName = p_shortName;
	}
	
	public long getBit(){
		return m_bit;
	}
	
	public String getShortName(){
		return m_shortName;
	}
	
	public static long getBitFromShortNames(List<String> p_modShortNames) {
		long bits = 0;
		
		for(String sMod : p_modShortNames)
			for(Mods mod : Mods.values())
				if(mod.getShortName().equalsIgnoreCase(sMod) ||
					mod.name().equalsIgnoreCase(sMod))
					bits += mod.getBit();
			
		
		return bits;
	}

	public static long getBitFromString(String sMods){
		long bits = 0;
		
		for(String sMod : sMods.split(","))
			for(Mods mod : Mods.values())
				if(mod.getShortName().equalsIgnoreCase(sMod) ||
					mod.name().equalsIgnoreCase(sMod))
					bits += mod.getBit();
			
		
		return bits;
	}
	
	public static List<Mods> getModsFromBit(long p_modsUsed){
		List<Mods> mods = new ArrayList<>();
		long used = p_modsUsed;
		
		if(used == 0) return mods;
		
		for(long i = 1073741824; i >= 1; i /= 2) {
			Mods mod = Mods.getMod(i);
			
			if(used >= i) {
				mods.add(mod);
				used -= i;
			}
		}
		
		if(mods.contains(Mods.None)) mods.remove(Mods.None);
		if(mods.contains(Mods.Nightcore)) mods.remove(Mods.DoubleTime);
		if(mods.contains(Mods.Perfect)) mods.remove(Mods.SuddenDeath);
		
		return mods;
	}
	
	public static long getBitFromMods(List<Mods> p_mods) {
		long modsUsed = 0;
		
		for(Mods mod : p_mods)
			modsUsed += mod.getBit();
		
		return modsUsed;
	}
	
	public static String getModDisplay(List<Mods> p_mods) {
		String display = "";
		
		for(Mods mod : p_mods)
			display = mod.getShortName() + display;
		
		return display.length() == 0 ? "" : "+" + display;
	}
	
	public static Mods getMod(long p_bit) {
		for(Mods mod : Mods.values())
			if(mod.getBit() == p_bit) return mod;
		
		return Mods.None;
	}
}