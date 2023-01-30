package osu.api;

import utils.GeneralUtils;

public abstract class OsuRequest {
	
	private String m_name;
	private OsuRequestTypes m_type;
	private long m_sentTime;
	private int m_timeout;
	private String[] m_arguments;
	private Object m_answer;
	
	public OsuRequest(String p_name, OsuRequestTypes p_type, String... p_args) {
		m_name = p_name;
		m_type = p_type;
		m_arguments = p_args;
	}
	
	public String getName() {
		return m_name;
	}
	
	public OsuRequestTypes getType() {
		return m_type;
	}
	
	public long getTimeSent() {
		return m_sentTime;
	}
	
	public int getTimeout() {
		return m_timeout;
	}
	
	public String[] getArguments() {
		return m_arguments;
	}
	
	public Object getAnswer() {
		return m_answer;
	}
	
	public void setType(OsuRequestTypes p_type) {
		m_type = p_type;
	}
	
	public void setSentTime() {
		m_sentTime = System.currentTimeMillis();
	}
	
	public void setTimeout(int p_timeout) {
		m_timeout = p_timeout;
	}
	
	public void setAnswer(Object p_answer) {
		m_answer = p_answer;
	}

	public abstract void send(boolean p_api) throws Exception;
	
	protected boolean checkForEmptyPost(String post, String userId) {
		if(post.isBlank() || !post.contains("{")) {
			int intPost = GeneralUtils.stringToInt(post);
			
			setAnswer("failed: " + intPost);
			
			return true;
		}
		
		return false;
	}
}