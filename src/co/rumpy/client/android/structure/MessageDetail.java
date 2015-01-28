package co.rumpy.client.android.structure;

public class MessageDetail {
	
	private int messageType;
	private String signum;
	private String messageBody;
	private long timestamp;
	
	public static final int MESSAGE_CHAT = 1;
	public static final int MESSAGE_GROUP = 2;
	//more type to come
	
	public MessageDetail(int messageType, String signum, String messageBody, long timestamp) {
		
		this.messageType = messageType;
		this.signum = signum;
		this.messageBody = messageBody;
		this.timestamp = timestamp;
		
	}
	
	public int getMessageType() {
		return this.messageType;
	}
	
	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}
	
	public String getSignum() {
		return this.signum;
	}
	
	public void setSignum(String signum) {
		this.signum = signum;
	}
	
	public String getMessageBody() {
		return this.messageBody;
	}
	
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
