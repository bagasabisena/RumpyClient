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
import android.provider.ContactsContract.Contacts.Data;

public class RosterProvider extends ContentProvider {
	
	Database databaseHelper = null;
	
	// public constants for client development
	public static final String AUTHORITY = "co.rumpy.client.android.db.RosterProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + Roster.CONTENT_PATH);
	
	// helper constants for use with the UriMatcher
	private static final int ROSTER_LIST = 1;
	private static final int ROSTER_ITEM_ID = 2;
	private static final int ROSTER_ITEM_SIGNUM = 3;
	private static final UriMatcher URI_MATCHER;
	
	/**
	* Column and content type definitions for the RosterProvider.
	*/
	
	public static interface Roster extends BaseColumns {
		
		public static final Uri CONTENT_URI = RosterProvider.CONTENT_URI;
		public static final String COLUMN_SIGNUM = "signum";
		public static final String COLUMN_FULLNAME = "fullname";
		public static final String COLUMN_IMAGE = "image";
		public static final String COLUMN_PRESENCE = "presence";
		public static final String COLUMN_NEW = "new";
	
		public static final String CONTENT_PATH = "roster";
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.abisena.roster";
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.abisena.roster";
		public static final String[] PROJECTION_ALL = {_ID, COLUMN_SIGNUM, COLUMN_FULLNAME, COLUMN_IMAGE, 
		   COLUMN_PRESENCE, COLUMN_NEW};
		public static final String SORT_ORDER_DEFAULT = COLUMN_FULLNAME + " ASC";
		
	}
	
	static {
		
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, Roster.CONTENT_PATH, ROSTER_LIST);
		URI_MATCHER.addURI(AUTHORITY, Roster.CONTENT_PATH + "/#", ROSTER_ITEM_ID);
		URI_MATCHER.addURI(AUTHORITY, Roster.CONTENT_PATH + "/*", ROSTER_ITEM_SIGNUM);

	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		
		switch (URI_MATCHER.match(uri)) {
		case ROSTER_LIST:
			return Roster.CONTENT_TYPE;
			
		case ROSTER_ITEM_ID:
			return Roster.CONTENT_ITEM_TYPE;
			
		case ROSTER_ITEM_SIGNUM:
			return Roster.CONTENT_ITEM_TYPE;
			
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri.toString());
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		if (URI_MATCHER.match(uri) != ROSTER_LIST) {
		      throw new IllegalArgumentException("Unsupported URI for insertion: " + uri); 
		}
		
		long id = db.insert(Database.TABLE_CONTACTS, null, values);
		
		if (id > 0) {
			// notify all listeners of changes and return itemUri: 
		    Uri itemUri = ContentUris.withAppendedId(uri, id); 
		    getContext().getContentResolver().notifyChange(itemUri, null);
		    return itemUri; 
		} else {
			return null;
		}

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
		String where = null;
		//String[] whereArgs = null;
		
		switch (URI_MATCHER.match(uri)) {
		case ROSTER_LIST:
			
			cursor = db.query(Database.TABLE_CONTACTS, Roster.PROJECTION_ALL, null, null, null, null, 
					Roster.SORT_ORDER_DEFAULT);
			
			break;
			
		case ROSTER_ITEM_ID:
			
			where = Roster._ID + "=" + uri.getLastPathSegment();
			cursor = db.query(Database.TABLE_CONTACTS, Roster.PROJECTION_ALL, where, null, null, null, 
					Roster.SORT_ORDER_DEFAULT);
			break;
			
		case ROSTER_ITEM_SIGNUM:
			
			where = Roster.COLUMN_SIGNUM + "=" + uri.getLastPathSegment();
			cursor = db.query(Database.TABLE_CONTACTS, Roster.PROJECTION_ALL, where, null, null, null, 
					Roster.SORT_ORDER_DEFAULT);
			break;

		default:
		      throw new IllegalArgumentException("Unsupported URI for query: " + uri); 
		}
		
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int updateCount = 0;
		String where = null;
		
		switch (URI_MATCHER.match(uri)) {
		case ROSTER_ITEM_SIGNUM:
			
			where = Roster.COLUMN_SIGNUM + "=" + uri.getLastPathSegment();
			updateCount = db.update(Database.TABLE_CONTACTS, values, where, null);
			
			break;

		default:
			 throw new IllegalArgumentException("Unsupported URI for query: " + uri); 
		}
		
		if (updateCount > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		
		return updateCount;
	}

}
