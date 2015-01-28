package co.rumpy.client.android.structure;

public class ChatDetail {
	
	private String message;
	private int state;
	private boolean fromMe;
	private String messageID;
	private long timestamp;
	
	//Static constant about the message state
	public static final int STATE_SENT = 0;
	public static final int STATE_DELIVERED = 1;
	public static final int STATE_READ = 2;
	public static final int STATE_UNREAD = 3;
	
	public ChatDetail (String message, int state, boolean fromMe, String messageID, long timestamp) {
		this.message = message;
		this.state = state;
		this.fromMe = fromMe;
		this.messageID = messageID;
		this.timestamp = timestamp;
	}
	
	public void setMessage (String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public void setState (int state) {
		this.state = state;
	}
	
	public int getState() {
		return this.state;
	}
	
	public void setFromMe (boolean fromMe) {
		this.fromMe = fromMe;
	}
	
	public boolean isFromMe() {
		return this.fromMe;
	}
	
	public void setMessageID (String messageID) {
		this.messageID = messageID;
	}
	
	public String getMessageID() {
		return this.messageID;
	}
	
	public void setTimestamp (long timestamp) {
		this.timestamp = timestamp;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}

}
