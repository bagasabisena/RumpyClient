/**
 * 
 * This database is not efficient
 * remote_id in messages should be foreign key to _id in contacts not signum
 * This design will result in performance hit when join operation is done
 * 
 * if you want to done migration, all message related operation
 * - getMessages
 * - getMessageState
 * - getRecentChats
 * - updateMessageState
 * 
 * must be changed (will involve join operation - test first from sqliteman)
 */

package co.rumpy.client.android.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.rumpy.client.android.structure.ChatDetail;
import co.rumpy.client.android.structure.ContactDetail;
import co.rumpy.client.android.structure.MessageLists;
import co.rumpy.client.android.structure.RecentChatDetail;
import co.rumpy.stanza.iq.Roster;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {
	
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "RumpyDatabase.db";
 
    // Contacts table name
    public static final String TABLE_CONTACTS = "contacts";
    
    // Message table name
    public static final String TABLE_MESSAGES = "messages";
 
    // Contacts Table Columns names
    public static final String KEY_ID = "_id";
    public static final String KEY_SIGNUM = "signum";
    public static final String KEY_NAME = "fullname";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_PRESENCE = "presence";
    public static final String KEY_NEW = "new";
    
    // Message table column name
    /**
     * integer primary key (index=0)
     */
    public static final String KEY_MESSAGE_ID = "_id"; // integer primary key
    
    /**
     *  integer, the signum of the destination (index=1)
     */
    public static final String KEY_MESSAGE_REMOTE_ID = "remote_id"; // text, the signum of the destination
    
    /**
     * text, the message string (index=2)
     */
    public static final String KEY_MESSAGE_MESSAGE = "message"; // text, the message string
    
    /**
     * integer, 0 means sent, 1 means delivered, 2 means read (index=3)
     */
    public static final String KEY_MESSAGE_MESSAGE_STATE = "message_state"; //integer, 0 means sent, 1 means delivered, 2 means read
    
    /**
     * text, the unique id of message, used for identification (index=4)
     */
    public static final String KEY_MESSAGE_MESSAGE_ID = "message_id"; //text, the unique id of message, used for identification
    
    /**
     * int (long), timestamp in long integer, unix epoch time (index=5)
     */
    public static final String KEY_MESSAGE_TIMESTAMP = "timestamp"; //int (long), timestamp in long integer, unix epoch time
    
    /**
     * integer, 1 means the message is from myself (index=6)
     */
    public static final String KEY_MESSAGE_FROM_ME = "from_me"; // integer, 1 means the message is from myself
    
    // static instance for singleton 
 	private static Database mInstance = null;
 	private Context mCtx;
 	
 	public static Database getInstance(Context ctx) {
 		
        /** 
         * use the application context as suggested by CommonsWare.
         * this will ensure that you dont accidentally leak an Activitys
         * context (see this article for more information: 
         * http://developer.android.com/resources/articles/avoiding-memory-leaks.html)
         */
        if (mInstance == null) {
            mInstance = new Database(ctx.getApplicationContext());
        }
        return mInstance;
    }
 	
 	/**
     * constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    public Database (Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	this.mCtx = context;
    }
    
    @Override
	public void onCreate(SQLiteDatabase db) {
		
		String CREATE_TABLE_CONTACT = "CREATE TABLE " + TABLE_CONTACTS + "(" 
										+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
										+ KEY_SIGNUM + " TEXT, " 
										+ KEY_NAME + " TEXT, " 
										+ KEY_IMAGE + " TEXT, "
										+ KEY_PRESENCE + " TEXT, " 
										+ KEY_NEW + " INTEGER);" ;
		db.execSQL(CREATE_TABLE_CONTACT);
		
		String CREATE_TABLE_MESSAGE = "CREATE TABLE " + TABLE_MESSAGES + "(" + 
				KEY_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				KEY_MESSAGE_REMOTE_ID + " INTEGER, " + 
				KEY_MESSAGE_MESSAGE + " TEXT, " + 
				KEY_MESSAGE_MESSAGE_STATE + " INTEGER, " + 
				KEY_MESSAGE_MESSAGE_ID + " TEXT, " + 
				KEY_MESSAGE_TIMESTAMP + " INTEGER, " + 
				KEY_MESSAGE_FROM_ME + " INTEGER);" ;
		
		db.execSQL(CREATE_TABLE_MESSAGE);
		
	}
    
    @Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
		 
        // Create tables again
        onCreate(db);
		
	}
    
    public void close(Cursor cursor, SQLiteDatabase db) {
    	
    	if (cursor != null) {
			cursor.close();
		}
		
		if (db != null) {
			db.close();
		}
		
    }
    
    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */
	
	public void addContact(ContactDetail contact) {
		
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_SIGNUM, contact.getSignum());
		values.put(KEY_NAME, contact.getFullname());
		values.put(KEY_IMAGE, contact.getImageThumbName());
		values.put(KEY_PRESENCE, contact.getLastPresence());
		
		db.insert(TABLE_CONTACTS, null, values);
		
		if (db != null || db.isOpen()) {
			db.close();
		}
		
	}
	
	public void addContact(Roster roster) {
		
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_SIGNUM, roster.getSignum());
		values.put(KEY_NAME, roster.getFullname());
		values.put(KEY_IMAGE, roster.getImage());
		values.put(KEY_PRESENCE, roster.getPresence());
		
		db.insert(TABLE_CONTACTS, null, values);
		
		if (db != null || db.isOpen()) {
			db.close();
		}
	}
	
	public ContactDetail getContact(String signum) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		String query = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_SIGNUM + " = " + "'" + signum + "';";
		Cursor cursor = db.rawQuery(query, null);
		
		if (cursor != null) {
			cursor.moveToFirst();
		}
		
		String fullname = cursor.getString(2);
		String image = cursor.getString(3);
		String presence = cursor.getString(4);
		ContactDetail contact = new ContactDetail(signum, fullname, image, presence);
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db != null || db.isOpen()) {
			db.close();
		}
		
		return contact;
	}
	
	public ArrayList<ContactDetail> getAllContacts() {
		
		SQLiteDatabase db = this.getReadableDatabase();
		ArrayList<ContactDetail> contacts = new ArrayList<ContactDetail>();
		
		String query = "SELECT * FROM " + TABLE_CONTACTS + " ORDER BY " + KEY_NAME + " asc;";
		Cursor cursor = db.rawQuery(query, null);
		
		cursor.moveToFirst();
		
		String signum;
		String fullname;
		String image;
		String presence;
		
		while (!cursor.isAfterLast()) {
			
			signum = cursor.getString(1);
			fullname = cursor.getString(2);
			image = cursor.getString(3);
			presence = cursor.getString(4);
			
			contacts.add(new ContactDetail(signum, fullname, image, presence));
			
			cursor.moveToNext();
			
		}
		
		close(cursor, db);
		
		return contacts;
		
	}
	
	public HashMap<String, ContactDetail> getAllSignumToContactsMap() {
		
		SQLiteDatabase db = this.getReadableDatabase();
		HashMap<String, ContactDetail> map = new HashMap<String, ContactDetail>();
		
		String query = "SELECT * FROM " + TABLE_CONTACTS + ";";
		Cursor cursor = db.rawQuery(query, null);
		
		cursor.moveToFirst();
		
		String signum;
		String fullname;
		String image;
		String presence;
		
		while (!cursor.isAfterLast()) {
			
			signum = cursor.getString(1);
			fullname = cursor.getString(2);
			image = cursor.getString(3);
			presence = cursor.getString(4);
			
			map.put(signum, new ContactDetail(signum, fullname, image, presence));
			
			cursor.moveToNext();
			
		}
		
		close(cursor, db);
		
		return map;
		
	}
	
	
	public List<String> getAllContactFullName() {
		
		SQLiteDatabase db = this.getReadableDatabase();
		List<String> fullnames = new ArrayList<String>();
		
		String query = "SELECT " + KEY_NAME + " FROM " + TABLE_CONTACTS + ";";
		Cursor cursor = db.rawQuery(query, null);
		
		cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			String fullname = cursor.getString(0);
			fullnames.add(fullname);
			cursor.moveToNext();
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db.isOpen()) {
			db.close();
		}
		
		
		//sort the list alphabetically
		Collections.sort(fullnames);
		
		return fullnames;
		
	}
	
	public boolean isContactAvailable(String signum) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		String query = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_SIGNUM + " = " + "'" + signum + "';";
		Cursor cursor = db.rawQuery(query, null);
		
		int rowCount = cursor.getCount();
		
		if (rowCount > 0) {
			//User exists
			return true;
		} else {
			//User doesn't exist
			return false;
		}
	}
	
	public Map<String, String> getAllContactFullnameToSignumMap() {
		
		SQLiteDatabase db = this.getReadableDatabase();
		Map<String, String> contactFullnameToSignumMap = new HashMap<String, String>();
		
		String query = "SELECT " + KEY_SIGNUM + ", " + KEY_NAME +" FROM " + TABLE_CONTACTS + ";";
		Cursor cursor = db.rawQuery(query, null);
		
		cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			String signum = cursor.getString(0);
			String fullname = cursor.getString(1);
			contactFullnameToSignumMap.put(fullname, signum);
			cursor.moveToNext();
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db.isOpen()) {
			db.close();
		}
		
		return contactFullnameToSignumMap;
	}
	
	public Map<String, String> getAllContactSignumToFullnameMap() {
		
		SQLiteDatabase db = this.getReadableDatabase();
		Map<String, String> contactSignumToFullnameMap = new HashMap<String, String>();
		
		String query = "SELECT " + KEY_SIGNUM + ", " + KEY_NAME +" FROM " + TABLE_CONTACTS + ";";
		Cursor cursor = db.rawQuery(query, null);
		
		cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			String signum = cursor.getString(0);
			String fullname = cursor.getString(1);
			contactSignumToFullnameMap.put(signum, fullname);
			cursor.moveToNext();
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db.isOpen()) {
			db.close();
		}
		
		return contactSignumToFullnameMap;
	}
	
	public void addMessage (ChatDetail chatDetail, String remoteID) {
		
		/*
		 * List of column:
		 * MESSAGE_ID
		 * MESSAGE_MESSAGE_ID
		 * MESSAGE_FROM_ID
		 * MESSAGE_FROM_ME
		 * MESSAGE_TIMESTAMP
		 * MESSAGE_STATE
		 * MESSAGE_MESSAGE
		 * 
		 */
		
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		values.put(KEY_MESSAGE_REMOTE_ID, "(SELECT _id FROM contacts WHERE signum='" + remoteID + "')");
		
		if (chatDetail.isFromMe()) {
			values.put(KEY_MESSAGE_FROM_ME, 1);
		} else {
			values.put(KEY_MESSAGE_FROM_ME, 0);
		}
		
		values.put(KEY_MESSAGE_MESSAGE, chatDetail.getMessage());
		values.put(KEY_MESSAGE_MESSAGE_ID, chatDetail.getMessageID());
		values.put(KEY_MESSAGE_TIMESTAMP, chatDetail.getTimestamp());
		values.put(KEY_MESSAGE_MESSAGE_STATE, chatDetail.getState());

		
		db.insert(TABLE_MESSAGES, null, values);
		
		if (db != null || db.isOpen()) {
			db.close();
		}
		
	}
	
	public MessageLists getMessages(int numOfMessages) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		MessageLists lists = new MessageLists();
		
		/*
		 * message (String) | messageID (String) | from_me (int) | message_state (int) | timestamp (long)
		 */
		
		String query = "SELECT " + KEY_MESSAGE_MESSAGE + ", " + KEY_MESSAGE_MESSAGE_ID + ", " + KEY_MESSAGE_FROM_ME + 
						", " + KEY_MESSAGE_MESSAGE_STATE + ", " + KEY_MESSAGE_TIMESTAMP +  " FROM " + TABLE_MESSAGES + 
						" ORDER BY " + KEY_MESSAGE_TIMESTAMP + " desc LIMIT " + numOfMessages;
		Cursor cursor = db.rawQuery(query, null);
		
		//Start the cursor from the last, so that newest message is at the bottom at the list
		cursor.moveToLast();
		
		while (!cursor.isBeforeFirst()) {
			
			String message = cursor.getString(0);
			String messageID = cursor.getString(1);
			
			boolean fromMe = false;
			switch (cursor.getInt(2)) {
			case 0:
				fromMe = false;
				break;
				
			case 1:
				fromMe = true;
				break;
			}
			
			int messageState = cursor.getInt(3);
			long timestamp = cursor.getLong(4);
			
			//put all value from cursor to ChatDetail object and insert to a list
			lists.detailList.add(new ChatDetail(message, messageState, fromMe, messageID, timestamp));
			
			//put message id to different list
			lists.idList.add(messageID);
			
			//put unread message id to another list
			if (messageState == ChatDetail.STATE_UNREAD && fromMe == false) {
				lists.unreadMessage.add(messageID);
			}
			
			cursor.moveToPrevious();
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db.isOpen()) {
			db.close();
		}
		
		return lists;
		
		
	}
	
	public MessageLists getMessages(String remoteSignum, int numOfMessages) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		MessageLists lists = new MessageLists();
		
		/*
		 * message (String) | messageID (String) | from_me (int) | message_state (int) | timestamp (long)
		 */
		
		/*String query = "SELECT " + KEY_MESSAGE_MESSAGE + ", " + KEY_MESSAGE_MESSAGE_ID + ", " + KEY_MESSAGE_FROM_ME + 
						", " + KEY_MESSAGE_MESSAGE_STATE + ", " + KEY_MESSAGE_TIMESTAMP +  " FROM " + TABLE_MESSAGES + 
						" WHERE " + KEY_MESSAGE_REMOTE_ID + "='" + remoteSignum + "'" + " ORDER BY " + KEY_MESSAGE_TIMESTAMP + 
						" desc LIMIT " + numOfMessages;*/
		
		String query = "SELECT m.message, m.message_id, m.from_me, m.message_state, m.timestamp, c.fullname " +
				"FROM messages AS m JOIN contacts c ON m.remote_id = c._id " +
				"WHERE remote_id IN (SELECT _id FROM contacts WHERE signum='" + remoteSignum + "') " +
				"ORDER BY m.timestamp DESC LIMIT " + numOfMessages;
		
		Cursor cursor = db.rawQuery(query, null);
		
		//Start the cursor from the last, so that newest message is at the bottom at the list
		cursor.moveToLast();
		
		while (!cursor.isBeforeFirst()) {
			
			String message = cursor.getString(0);
			String messageID = cursor.getString(1);
			
			boolean fromMe = false;
			switch (cursor.getInt(2)) {
			case 0:
				fromMe = false;
				break;
				
			case 1:
				fromMe = true;
				break;
			}
			
			int messageState = cursor.getInt(3);
			long timestamp = cursor.getLong(4);
			
			//put all value from cursor to ChatDetail object and insert to a list
			lists.detailList.add(new ChatDetail(message, messageState, fromMe, messageID, timestamp));
			
			//put message id to different list
			lists.idList.add(messageID);
			
			//put unread message id to another list
			if (messageState == ChatDetail.STATE_UNREAD && fromMe == false) {
				lists.unreadMessage.add(messageID);
			}
			
			cursor.moveToPrevious();
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db.isOpen()) {
			db.close();
		}
		
		return lists;
		
		
	}
	
	public void updateMessageState(String messageID, int currentMessageState) {
		
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MESSAGE_MESSAGE_STATE, currentMessageState);
		
		String whereClause = KEY_MESSAGE_MESSAGE_ID + "=?" ;
		String[] whereArgs = {messageID};
		db.update(TABLE_MESSAGES, values, whereClause, whereArgs);
		
		if (db != null || db.isOpen()) {
			db.close();
		}
	}
	
	public void updateMessageState(List<String> messageIDs, int currentMessageState) {
		
		SQLiteDatabase db = this.getWritableDatabase();
		
		for (String messageID : messageIDs) {
			
			ContentValues values = new ContentValues();
			values.put(KEY_MESSAGE_MESSAGE_STATE, currentMessageState);
			
			String whereClause = KEY_MESSAGE_MESSAGE_ID + "=?" ;
			String[] whereArgs = {messageID};
			db.update(TABLE_MESSAGES, values, whereClause, whereArgs);
			
			/*String query = "UPDATE " + TABLE_MESSAGES + " SET " + KEY_MESSAGE_MESSAGE_STATE + "=" 
							+ currentMessageState + " WHERE " + KEY_MESSAGE_MESSAGE_ID + "='" + messageID + "'";
			db.rawQuery(query, null); */

		}
		
		if (db != null || db.isOpen()) {
			db.close();
		}
		
		
	}
	
	/**
	 * get the state of the message. If query failed it will return -1
	 */
	
	public Integer getMessageState (String messageID) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		String query = "SELECT " + KEY_MESSAGE_MESSAGE_STATE + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_MESSAGE_ID
				+ "='" + messageID + "'" ;
		
		Cursor cursor = db.rawQuery(query, null);
		cursor.moveToFirst();
		
		int rowCount = cursor.getCount();
		if (rowCount == 0) {
			return -1;
		}
		
		int messageState = cursor.getInt(0);
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (db.isOpen()) {
			db.close();
		}
		
		return messageState;
		
	}
	
	public ArrayList<RecentChatDetail> getRecentChats() {
		
		SQLiteDatabase db = this.getReadableDatabase();
		ArrayList<RecentChatDetail> chats = new ArrayList<RecentChatDetail>();
		
		String query = "SELECT tmp.signum, tmp.fullname, tmp.image, tmp.message, tmp.timestamp, tmp.presence " +
				"FROM (SELECT c.signum, m.message, m.timestamp, c.fullname, c.image, c.presence FROM messages AS m " +
				"JOIN contacts AS c ON m.remote_id = c._id ORDER BY m.timestamp ASC) AS tmp " +
				"GROUP BY tmp.signum ORDER BY tmp.timestamp DESC";
		
		Cursor cursor = db.rawQuery(query, null);
		cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			
			String signum = cursor.getString(0);
			String fullname = cursor.getString(1);
			String image = cursor.getString(2);
			String lastChat = cursor.getString(3);
			long timestamp = cursor.getLong(4);
			String presence = cursor.getString(5);
			
			chats.add(new RecentChatDetail(signum, fullname, image, lastChat, timestamp, presence));
			
			cursor.moveToNext();
			
		}
		
		close();
		
		return chats;
	}

}
