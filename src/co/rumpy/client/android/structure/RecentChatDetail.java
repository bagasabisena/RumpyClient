package co.rumpy.client.android.structure;

public class RecentChatDetail {
	
	private String signum;
	private String fullname;
	private String imageThumbName;
	private String lastChat;
	private long timestamp;
	private String presence;
	
	public RecentChatDetail(String signum, String fullname, String imageThumbName, String lastChat, long timestamp, String presence) {
		this.signum = signum;
		this.fullname = fullname;
		this.imageThumbName = imageThumbName;
		this.lastChat = lastChat;
		this.timestamp = timestamp;
		this.presence = presence;
	}
	
	public String getSignum() {
		return this.signum;
	}
	
	public void setSignum(String signum) {
		this.signum = signum;
	}
	
	public String getFullname() {
		return this.fullname;
	}
	
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	
	public String getImageThumbName() {
		return this.imageThumbName;
	}
	
	public void setImageThumbName(String imageThumbName) {
		this.imageThumbName = imageThumbName;
	}
	
	public String getLastChat() {
		return this.lastChat;
	}
	
	public void setLastChat(String lastChat) {
		this.lastChat = lastChat;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getPresence() {
		return presence;
	}
	
	public void setPresence(String presence) {
		this.presence = presence;
	}

}
