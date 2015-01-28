package co.rumpy.client.android;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import co.rumpy.client.android.adapter.ConversationArrayAdapter;
import co.rumpy.client.android.adapter.ConversationCursorAdapter;
import co.rumpy.client.android.db.Database;
import co.rumpy.client.android.db.MessageProvider;
import co.rumpy.client.android.structure.ChatDetail;
import co.rumpy.client.android.structure.MessageLists;
import co.rumpy.client.android.utils.Constants;
import co.rumpy.client.android.utils.StringUtils;
import co.rumpy.client.android.utils.XML;
import co.rumpy.stanza.message.Message;
import co.rumpy.stanza.message.Message.Type;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ConversationActivity extends SherlockFragmentActivity implements LoaderCallbacks<Cursor> {
	
		// initiate arraylist for list adapter
		ArrayList<ChatDetail> chatDetails;
		ArrayList<String> messageIDs;
		ArrayList<String> unreadMessages;
		
		// initiate database helper class
		Database db;
		
		XML xml;
		
		// this is a data structure for containing above arraylists
		MessageLists messageLists;
		
		// initiate adapter
		ConversationArrayAdapter adapter;
		IntentFilter filter;
		IntentFilter receiptFilter;
		IncomingMessageReceiver receiver;
		IncomingReceiptReceiver receiptReceiver;
		
		// ---------------------------------------------------------
		
		// initiate UI (View) elements
		EditText editText;
		ListView listView;
		Button sendButton;
		ActionBar actionBar;
		
		SharedPreferences sP;
		
		String remoteSignum;
		String remoteFullname;
		String remotePresence;
		String remoteImageThumb;
		String mySignum;
		String myFullname;
		String myBareSignum;
		
		// Constant for loader in this activity
		public static final int LOADER_CONVERSATION = 0;
		private static final String TAG = ConversationActivity.class.getSimpleName();
		
		ConversationCursorAdapter cursorAdapter;
		ContentResolver resolver;
		NotificationManager notifMgr;
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			
			super.onCreate(savedInstanceState);
			
			setContentView(R.layout.layout_list);
			
			//Initiate UI widgets
	        editText = (EditText) findViewById(R.id.new_edittext);
	        listView = (ListView) findViewById(R.id.new_list);
	        sendButton = (Button) findViewById(R.id.new_button);
	        sendButton.setOnClickListener(sendOnClickListener);
	        //editText.setOnClickListener(editOnClickListener);
	        
	        //XML
	        xml = new XML();
	        
	        // init broadcast receiver about new message
	 		filter = new IntentFilter(RumpyService.NEW_MESSAGE);
	 		receiver = new IncomingMessageReceiver();
	 		registerReceiver(receiver, filter);
	 		
	 		// init broadcast receiver about new receipt
			receiptFilter = new IntentFilter(RumpyService.NEW_RECEIPT);
			receiptReceiver = new IncomingReceiptReceiver();
			registerReceiver(receiptReceiver, receiptFilter);
	        
	        // Database
	        //db = RumpyService.db;
			
			// --------------------------------------------------------------------
	        
	        // Shared Preference
	        sP = getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
	        
	        
			
			// get my signum and remote contact detail
			mySignum = sP.getString("signum", "");
			myBareSignum = sP.getString(Constants.PREF_TAG_BARESIGNUM, "");
			
			remoteSignum = getIntent().getStringExtra("signum");
			remoteFullname = getIntent().getStringExtra("fullname");
	        remotePresence = getIntent().getStringExtra("presence");
	        remoteImageThumb = getIntent().getStringExtra("image");
	        
	        // set actionbar
	        actionBar = getSupportActionBar();
	        actionBar.setTitle(remoteFullname);
	        actionBar.setSubtitle(remotePresence);
	        // should set image based on image file name and retrieve the image
	        // from internal storage
	        // this just a dummy which get image from drawable
	        if (remoteImageThumb.equals("bagas.jpg")) {
	        	actionBar.setIcon(R.drawable.bagas);
	        } else if (remoteImageThumb.equals("diska.jpg")) {
	        	actionBar.setIcon(R.drawable.diska);
	        } else {
	        	actionBar.setIcon(R.drawable.default_thumb);
	        }
	        
	        getSupportLoaderManager().initLoader(LOADER_CONVERSATION, null, this);
	        
	        resolver = getContentResolver();
	        cursorAdapter = new ConversationCursorAdapter(ConversationActivity.this, null, 0);
	        listView.setAdapter(cursorAdapter);
	        
	        notifMgr = (NotificationManager) ConversationActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
	        
		}
		
		private void refreshList() {
			
			db = Database.getInstance(this);
			messageLists = db.getMessages(remoteSignum, 50);
	        chatDetails = messageLists.detailList;
	        messageIDs = messageLists.idList;
	        unreadMessages = messageLists.unreadMessage;
	        adapter = new ConversationArrayAdapter(this, chatDetails);
	        listView.setAdapter(adapter);
			
		}
		
		@Override
		protected void onPause() {
			
			super.onPause();
			
			try {
			    unregisterReceiver(receiver);
			    unregisterReceiver(receiptReceiver);
			} catch (IllegalArgumentException e) {
			    if (e.getMessage().contains("Receiver not registered")) {
			        // Ignore this exception. This is exactly what is desired
			    } else {
			        // unexpected, re-throw
			        throw e;
			    }
			}
		}
		
		@Override
		protected void onResume() {
			
			super.onResume();
			
			Log.d("state", "onResume");
			
			registerReceiver(receiver, filter);
			registerReceiver(receiptReceiver, receiptFilter);
			
			//refreshList();
			notifMgr.cancel(RumpyService.NotificationService.NOTIFICATION_CHAT);
		}
		
		@Override
		protected void onDestroy() {
			
			super.onDestroy();
			
			try {
			    unregisterReceiver(receiver);
			    unregisterReceiver(receiptReceiver);
			} catch (IllegalArgumentException e) {
			    if (e.getMessage().contains("Receiver not registered")) {
			        // Ignore this exception. This is exactly what is desired
			    } else {
			        // unexpected, re-throw
			        throw e;
			    }
			}
		}
		
		private class IncomingMessageReceiver extends BroadcastReceiver {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				Bundle bundle = intent.getExtras();
				String receivedSignum = bundle.getString("from");
				String messageBody = bundle.getString("body");
				String messageID = bundle.getString("id");
				long timestamp = bundle.getLong("timestamp");
				
				if (receivedSignum.equalsIgnoreCase(remoteSignum)) {
					
					ChatDetail cd = new ChatDetail(messageBody, ChatDetail.STATE_READ, false, messageID, timestamp);
					chatDetails.add(cd);
					messageIDs.add(messageID);
					adapter.notifyDataSetChanged();
					
					notifMgr.cancel(RumpyService.NotificationService.NOTIFICATION_CHAT);
					
					Message readReceipt = new Message();
					readReceipt.setTo(receivedSignum);
					readReceipt.setFrom(mySignum);
					readReceipt.setId(StringUtils.randomStringGenerator(6));
					ArrayList<String> readIDs = new ArrayList<String>();
					readIDs.add(messageID);
					readReceipt.setReadReceipt(readIDs);
					
					//RumpyService.sendStanza(readReceipt);
					RumpyService.sendStanza(readReceipt, ConversationActivity.this);
					
					/*byte[] readReceiptInXML = null;
					try {
						readReceiptInXML = xml.messageRead(mySignum, receivedSignum, messageID);
					} catch (IllegalArgumentException e) {
						
					} catch (IllegalStateException e) {
						
					} catch (IOException e) {
						
					}
					
					try {
						RumpyService.messageQueue.put(readReceiptInXML);
					} catch (InterruptedException e) {
						
					} */
					
					Database.getInstance(ConversationActivity.this).updateMessageState(messageID, ChatDetail.STATE_READ);
				}
				
			}
			
			
		}
		
		private class IncomingReceiptReceiver extends BroadcastReceiver {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				Bundle bundle = intent.getExtras();
				String receivedSignum = bundle.getString("from");
				int receiptType = bundle.getInt("receipt_type");
				ArrayList<String> receiverMessageIDs = bundle.getStringArrayList("id");
				
				if (receivedSignum.equalsIgnoreCase(remoteSignum)) {
					
					for (String messageID : receiverMessageIDs) {
						int index = messageIDs.indexOf(messageID);
						if (index != -1) {
							ChatDetail cd = chatDetails.get(index);
							cd.setState(receiptType);
							chatDetails.set(index, cd);
							//adapter.notifyDataSetChanged();
							//updateMessageState(messageID, receiptType);
						}
					}
					
					adapter.notifyDataSetChanged();
					
				}
				
			}
			
			
		}
		
		private OnClickListener sendOnClickListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				String message = editText.getText().toString();
				editText.setText("");
				
				//Set random String for message ID
				String randomCode = StringUtils.randomStringGenerator(6);
				
				// Set timestamp
				long timestamp = System.currentTimeMillis();
				
				// create chatdetails object, update the list, to update the UI
				ChatDetail cd = new ChatDetail(message, ChatDetail.STATE_SENT, true, randomCode, timestamp);
				//chatDetails.add(cd);
				//messageIDs.add(randomCode);
				//adapter.notifyDataSetChanged();
				
				// record my chat into database for persistency
				//Database.getInstance(ConversationActivity.this).addMessage(cd, remoteSignum);
				
				ContentValues values = new ContentValues();
				values.put(Database.KEY_MESSAGE_REMOTE_ID, remoteSignum);
				
				if (cd.isFromMe()) {
					values.put("from_me", 1);
				} else {
					values.put("from_me", 0);
				}
				
				values.put("message", cd.getMessage());
				values.put("message_id", cd.getMessageID());
				values.put("timestamp", cd.getTimestamp());
				values.put("message_state", cd.getState());
				
				resolver.insert(MessageProvider.CONTENT_URI, values);
				
				Message messageSend = new Message();
				messageSend.setTo(remoteSignum);
				messageSend.setFrom(myBareSignum);
				messageSend.setId(randomCode);
				messageSend.setType(Type.CHAT);
				messageSend.setBody(message);
				messageSend.setReceiptRequested(true);
				
				//RumpyService.sendStanza(messageSend);
				RumpyService.sendStanza(messageSend, ConversationActivity.this);
				
				/*byte[] messageInXML = null;
				
				try {
					messageInXML = xml.messageSend(mySignum, remoteSignum, message, randomCode, XML.MESSAGE_CHAT);
				} catch (IllegalArgumentException e) {
					
				} catch (IllegalStateException e) {
					
				} catch (IOException e) {
					
				}
				
				try {
					RumpyService.messageQueue.put(messageInXML);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} */
				
				//adapter.notifyDataSetChanged();
				
			}
		};
		
		private OnClickListener editOnClickListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (unreadMessages.size() > 0) {
					
					Message readReceipt = new Message();
					readReceipt.setFrom(mySignum);
					readReceipt.setTo(remoteSignum);
					readReceipt.setId(StringUtils.randomStringGenerator(6));
					ArrayList<String> receipts = new ArrayList<String>();
					receipts = unreadMessages;
					readReceipt.setReadReceipt(receipts);
					
					//RumpyService.sendStanza(readReceipt);
					RumpyService.sendStanza(readReceipt, ConversationActivity.this);
					
					/*byte[] readMessageInXML = null;
					try {
						readMessageInXML = xml.messageReadMulti(mySignum, remoteSignum, unreadMessages);
					} catch (IllegalArgumentException e) {
						
					} catch (IllegalStateException e) {
						
					} catch (IOException e) {
						
					}
					
					try {
						RumpyService.messageQueue.put(readMessageInXML);
					} catch (InterruptedException e) {
						
					}*/
					Database.getInstance(ConversationActivity.this).updateMessageState(unreadMessages, ChatDetail.STATE_READ);
					unreadMessages.clear();
				}
				
			}
		};

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			Uri uri = Uri.withAppendedPath(MessageProvider.CONTENT_URI, remoteSignum);
			CursorLoader loader = new CursorLoader(this, uri, null, null, null, null);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
			
			cursorAdapter.swapCursor(cursor);
			
			int messageState = 0;
			boolean fromMe = false;
			String messageID;
						
			ArrayList<String> readReceipts = new ArrayList<String>();
			
			cursor.moveToFirst();
			
			while(!cursor.isAfterLast()) {
				
				messageState = cursor.getInt(3);
				
				switch (cursor.getInt(6)) {
				case 0:
					fromMe = false;
					break;
					
				case 1:
					fromMe = true;
					break;
				}
				
				messageID = cursor.getString(4);
				
				if (messageState == ChatDetail.STATE_UNREAD && fromMe == false) {
					readReceipts.add(messageID);
					
					ContentValues values = new ContentValues();
					values.put(Database.KEY_MESSAGE_MESSAGE_STATE, ChatDetail.STATE_READ);
					
					String whereClause = Database.KEY_MESSAGE_MESSAGE_ID + "=?" ;
					String[] whereArgs = {messageID};
					
					Uri uri = Uri.withAppendedPath(MessageProvider.CONTENT_URI, "item/" + messageID);
					
					resolver.update(uri, values, null, null);
				}
				
				cursor.moveToNext();
			}
			
			if (readReceipts.size() > 0) {
				
				Message readReceipt = new Message();
				readReceipt.setFrom(myBareSignum);
				readReceipt.setTo(remoteSignum);
				readReceipt.setId(StringUtils.randomStringGenerator(6));
				readReceipt.setReadReceipt(readReceipts);
				
				//RumpyService.sendStanza(readReceipt);
				RumpyService.sendStanza(readReceipt, ConversationActivity.this);
				
			}
			
			
		}

		@Override
		public void onLoaderReset(Loader<Cursor> cursor) {
			cursorAdapter.swapCursor(null);			
		}


}
