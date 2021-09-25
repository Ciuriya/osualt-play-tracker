package data;

import java.awt.Color;

import utils.Constants;

public enum CommandCategory {
	
	GENERAL("General", Constants.DEFAULT_EMBED_COLOR),
	OSU("osu!", new Color(1.0f, 102.0f / 255.0f, 170.0f / 255.0f)), // pink color
	ADMIN("Admin", Color.GRAY);
	
	private String m_displayName;
	private Color m_color;
	
	private CommandCategory(String p_displayName, Color p_color) {
		m_displayName = p_displayName;
		m_color = p_color;
	}

	public String getName() {
		return m_displayName;
	}
	
	public Color getColor() {
		return m_color;
	}
}
