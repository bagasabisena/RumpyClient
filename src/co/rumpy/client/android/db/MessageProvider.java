package co.rumpy.client.android.db;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class MessageProvider extends ContentProvider {
	
	Database databaseHelper = null;
	
	// public constants for client development
	public static final String AUTHORITY = "co.rumpy.client.android.db.MessageProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Message.CONTENT_PATH);
	public static final Uri CONTENT_ITEM_URI = Uri.parse("content://" + AUTHORITY + "/" + Message.CONTENT_ITEM_PATH);
	
	// helper constants for use with the UriMatcher
	private static final int MESSAGE_LIST_ALL = 1;
	private static final int MESSAGE_LIST_SIGNUM = 2;
	private static final int MESSAGE_ITEM_ID = 3;
	private static final int MESSAGE_ITEM_MESSAGEID = 4;
	private static final int MESSAGE_LIST_RECENT = 5;
	private static final UriMatcher URI_MATCHER;
	
	/**
	* Column and content type definitions for the MessageProvider.
	*/
	
	public static interface Message extends BaseColumns {
		
		public static final Uri CONTENT_URI = MessageProvider.CONTENT_URI;
		public static final String COLUMN_REMOTE_ID = "remote_id";
		public static final String COLUMN_MESSAGE = "message";
		public static final String COLUMN_MESSAGE_STATE = "message_state";
		public static final String COLUMN_MESSAGE_ID = "message_id";
		public static final String COLUMN_TIMESTAMP = "timestamp";
		public static final String COLUMN_FROM_ME = "from_me";
	
		public static final String CONTENT_PATH = "message";
		public static final String CONTENT_ITEM_PATH = "item";
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.abisena.message";
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.abisena.message";
		public static final String[] PROJECTION_ALL = {_ID, COLUMN_REMOTE_ID, COLUMN_FROM_ME, COLUMN_MESSAGE, 
		   COLUMN_TIMESTAMP, COLUMN_MESSAGE_ID, COLUMN_MESSAGE_STATE};
		public static final String SORT_ORDER_DEFAULT = COLUMN_TIMESTAMP + " ASC";
		
	}
	
	static {
		   URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		   URI_MATCHER.addURI(AUTHORITY, Message.CONTENT_PATH + "/item/#", MESSAGE_ITEM_ID);
		   URI_MATCHER.addURI(AUTHORITY, Message.CONTENT_PATH + "/item/*", MESSAGE_ITEM_MESSAGEID);
		   URI_MATCHER.addURI(AUTHORITY, Message.CONTENT_PATH + "/recent", MESSAGE_LIST_RECENT);
		   URI_MATCHER.addURI(AUTHORITY, Message.CONTENT_PATH + "/*", MESSAGE_LIST_SIGNUM);
		   URI_MATCHER.addURI(AUTHORITY, Message.CONTENT_PATH, MESSAGE_LIST_ALL);
		   
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		
		switch (URI_MATCHER.match(uri)) {
		case MESSAGE_LIST_ALL:
			return Message.CONTENT_TYPE;
			
		case MESSAGE_LIST_SIGNUM:
			return Message.CONTENT_TYPE;
			
		case MESSAGE_ITEM_ID:
			return Message.CONTENT_ITEM_TYPE;
			
		case MESSAGE_ITEM_MESSAGEID:
			return Message.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri.toString());
		}
		
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		if (URI_MATCHER.match(uri) != MESSAGE_LIST_ALL) { 
		      throw new IllegalArgumentException("Unsupported URI for insertion: " + uri); 
		}
		
		String signum = values.getAsString("remote_id");
		Cursor c = db.rawQuery("SELECT _id FROM contacts WHERE signum='" + signum + "';" , null);
		c.moveToNext();
		values.put("remote_id", c.getInt(0));
		
		long id = db.insert(Database.TABLE_MESSAGES, null, values);
		
		if (id > 0) {
			// notify all listeners of changes and return itemUri: 
		    Uri itemUri = ContentUris.withAppendedId(uri, id); 
		    getContext().getContentResolver().notifyChange(uri, null);
		    return itemUri; 
		}
		
		return null;
	}

	@Override
	public boolean onCreate() {
		databaseHelper = new Database(getContext());
		
		if (databaseHelper == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteDatabase db = databaseHelper.getReadableDatabase();
		Cursor cursor = null;
		String query;
		
		
		switch (URI_MATCHER.match(uri)) {
		
		case MESSAGE_LIST_ALL:
			query = "SELECT * FROM messages ORDER BY timestamp ASC";
			cursor = db.rawQuery(query, null);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			break;
			
		case MESSAGE_LIST_SIGNUM:
			//String signum = new String(selection);
			String signum = uri.getLastPathSegment();
			query = "SELECT * FROM (SELECT * FROM messages " +
						"WHERE remote_id=(SELECT _id FROM contacts WHERE signum='" + signum + "') " +
						"ORDER BY timestamp DESC limit 50) ORDER BY timestamp ASC;";
			
			cursor = db.rawQuery(query, null);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			
			break;
			
		case MESSAGE_ITEM_MESSAGEID:
			String _selection = Database.KEY_MESSAGE_MESSAGE_ID + "=?";
			String[] _selectionArgs = {uri.getLastPathSegment()};
			//String sql = "SELECT message_state FROM messages WHERE message_id='" + uri.getLastPathSegment() + "';";
			cursor = db.query(Database.TABLE_MESSAGES, projection, _selection, _selectionArgs, null, null, null);
			//cursor = db.rawQuery(sql, null);
			break;
			
		case MESSAGE_LIST_RECENT:
			query = "SELECT tmp.signum, tmp.fullname, tmp.image, tmp.presence, tmp.message, tmp.timestamp, tmp.message_state, tmp._id " +
					"FROM (SELECT c.signum, m.message, m.timestamp, c.fullname, c.image, c.presence, m.message_state, m._id FROM messages AS m " +
					"JOIN contacts AS c ON m.remote_id = c._id ORDER BY m.timestamp ASC) AS tmp " +
					"GROUP BY tmp.signum ORDER BY tmp.timestamp DESC;";
			
			cursor = db.rawQuery(query, null);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			
			break;
			

		default:
			break;
		}
		
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int updateCount = 0;
		
		switch (URI_MATCHER.match(uri)) {
		case MESSAGE_LIST_ALL:
			updateCount = db.update(Database.TABLE_MESSAGES, values, selection, selectionArgs);
			break;
			
		case MESSAGE_ITEM_MESSAGEID:
			String _selection = Database.KEY_MESSAGE_MESSAGE_ID + "=?";
			String[] _selectionArgs = {uri.getLastPathSegment()};
			updateCount = db.update(Database.TABLE_MESSAGES, values, _selection, _selectionArgs);

		default:
			break;
		}
		
		if (updateCount > 0) {
			getContext().getContentResolver().notifyChange(CONTENT_URI, null);
		}
		
		return updateCount;
	}
	
	
	
	
	
	

}
