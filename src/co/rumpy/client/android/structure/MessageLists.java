package co.rumpy.client.android.structure;

import java.util.ArrayList;

public class MessageLists {
	
	public ArrayList<String> idList;
	public ArrayList<ChatDetail> detailList;
	public ArrayList<String> unreadMessage;
	
	public MessageLists() {
		idList = new ArrayList<String>();
		detailList = new ArrayList<ChatDetail>();
		unreadMessage = new ArrayList<String>();
	}

}
