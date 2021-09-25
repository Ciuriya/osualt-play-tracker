package data;

import net.dv8tion.jda.api.entities.User;

public enum BotAdmins {
	
	Smc("91302128328392704");
	
	private String m_discordId;
	
	private BotAdmins(String p_discordId) {
		m_discordId = p_discordId;
	}

	public static boolean isAdmin(User p_user) {
		for(BotAdmins admin : BotAdmins.values())
			if(admin.m_discordId.equals(p_user.getId()))
				return true;
		
		return false;
	}
}
