package help;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class HelpBlurb {
	
	public static List<HelpBlurb> blurbs = new ArrayList<>();
	
	private String[] m_names;
	private String m_description;

	public HelpBlurb(String[] p_names, String p_description) {
		m_names = p_names;
		m_description = p_description;
		blurbs.add(this);
	}
	
	public String[] getNames() {
		return m_names;
	}
	
	public String getDescription() {
		return m_description;
	}
	
	public static HelpBlurb findBlurb(String p_name) {
		return blurbs.stream().filter(b -> Arrays.asList(b.m_names).stream()
											 .anyMatch(n -> n.equalsIgnoreCase(p_name)))
											 .findFirst().orElse(null);
	}
	
	public static void registerBlurbs() {
		new CycleBlurb();
	}
}
