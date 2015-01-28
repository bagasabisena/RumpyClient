package co.rumpy.client.android;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.xmlpull.v1.XmlPullParserException;

import co.rumpy.client.android.IQOperation.Failure;
import co.rumpy.client.android.Ping.Error;
import co.rumpy.client.android.Ping.PingHandler;
import co.rumpy.client.android.db.Database;
import co.rumpy.client.android.db.MessageProvider;
import co.rumpy.client.android.db.RosterProvider;
import co.rumpy.client.android.handler.JSONHandler;
import co.rumpy.client.android.structure.ChatDetail;
import co.rumpy.client.android.structure.ContactDetail;
import co.rumpy.client.android.structure.MessageDetail;
import co.rumpy.client.android.structure.StanzaQueue;
import co.rumpy.client.android.utils.ConnectionLog;
import co.rumpy.client.android.utils.Constants;
import co.rumpy.client.android.utils.StringUtils;
import co.rumpy.client.android.utils.XML;

import co.rumpy.stanza.*;
import co.rumpy.stanza.presence.Presence;
import co.rumpy.stanza.stream.*;
import co.rumpy.stanza.iq.*;
import co.rumpy.stanza.message.*;
import co.rumpy.stanza.message.Message.Type;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class RumpyService extends Service {
	
	/**
	 * Network Attribute and Flags
	 */
	
	private final String hostname = "192.168.0.2";
	private final String port = "9000";
	private final String wsuri = "ws://" + hostname + ":" + port;
	public static WebSocket webSocket = null;
	//private WebSocketConnectionHandler
	private static String serviceStatus ="";
	private ContentResolver resolver = null;
	
	private static final String TAG = RumpyService.class.getSimpleName();
	
	
	private TCPConnection tcpConnection;
	private boolean isServiceStarted;
	private final String serverIP = "49.50.9.73";
	private final int serverPort = 1601;
	private ConnectionLog connLog;
	
	/**
	 * Shared Preferences and Managers
	 */
	
	SharedPreferences sharedPref;
	SharedPreferences.Editor editPref;
	NotificationManager notifMan;
	String ns = Context.NOTIFICATION_SERVICE;
	NotificationService notificationService;
	static ConnectivityManager connectMan;
	String cm = Context.CONNECTIVITY_SERVICE;
	AlarmManager alarmMan;
	String am = Context.ALARM_SERVICE;
	
	// Message Queue outbound
	public static LinkedBlockingQueue<byte[]> messageQueue;
	private static LinkedBlockingQueue<String> stanzaQueue;
	
	XML xml;
	private boolean isRosterAvailable;
	
	String mySignum;
	
	
	// Database
	public static Database db;
	private Map<String, String> rosterMap;
	private HashMap<String, ContactDetail> contactsMap;
	
	// Intervals
	private final long UPDATE_INTERVAL = 5*60*1000;
	private final long CHECK_FOR_SOCKET_INTERVAL = 1*60*1000;
	public static long HEARTBEAT_INTERVAL = 5*60*1000; //5 minutes
	private final long TIMEOUT = 1*60*1000;
	
	String heartbeat_id;
	public static String heartbeatIDFromServer;
	public static String heartbeatIDLocal;
	
	// Broadcast action
	public static final String NEW_MESSAGE = "co.rumpy.rumpy.action.NEW_MESSAGE";
	public static final String NEW_ROSTER = "co.rumpy.rumpy.action.NEW_ROSTER";
	public static final String NEW_RECEIPT = "co.rumpy.rumpy.action.NEW_RECEIPT";
	public static final String REGISTERED = "co.rumpy.client.android.action.REGISTERED";
	public static final String DISCONNECTED = "co.rumpy.client.android.action.DISCONNECTED";
		
	// Pending intent action
	public static final String SERVICE_START = "co.rumpy.rumpy.action.SERVICE_START";
	public static final String SERVICE_HEARTBEAT = "co.rumpy.rumpy.action.SERVICE_HEARTBEAT";
	public static final String SERVICE_RECONNECT = "co.rumpy.rumpy.action.SERVICE_RECONNECT";
	public static final String SERVICE_STOP = "co.rumpy.rumpy.action.SERVICE_STOP";
	
	public static final String PREF_IS_SERVICE_STARTED = "isServiceStarted";
	public static final String PREF_SERVICE_STATUS = "serviceStatus";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		
		super.onCreate();
		
		sharedPref = getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
		editPref = sharedPref.edit();
		messageQueue = new LinkedBlockingQueue<byte[]>();
		stanzaQueue = new LinkedBlockingQueue<String>();
		notifMan = (NotificationManager) getSystemService(ns);
		connectMan = (ConnectivityManager) getSystemService(cm);
		alarmMan = (AlarmManager) getSystemService(am);
		xml = new XML();
		
		try {
			connLog = new ConnectionLog();
			Log.i(Constants.TAG_SERVICE, "opened log at " + connLog.getPath());
		} catch (IOException e) {
			// Log opening error
		}
		
		heartbeat_id = "";
		heartbeatIDFromServer = "";
		heartbeatIDLocal = "";
		
		db = Database.getInstance(this);
		
		rosterMap = db.getAllContactSignumToFullnameMap();
		contactsMap = db.getAllSignumToContactsMap();
		
		notificationService = new NotificationService(this);
		
		mySignum = sharedPref.getString("signum", null);
		
		resolver = getContentResolver();
		
		//handleCrashService();
		
	}
	
	/**
	 * If our process was reaped by the system for any reason we need
	 * to restore our state with merely a call to onCreate.  We record
	 * the last "started" value and restore it here if necessary.
	 */
	
	private void handleCrashService() {
		
		if (wasStarted() == true) {
			
			// do some clean up
			stopHeartbeat();
			
			// attempt to start connection
			start();
		}
		
	}
	
	/**
	 * Logging service event to logcat and local text file
	 */
	
	private void logger(String message) {
		
		Log.i(Constants.TAG_SERVICE, message);
		
		if (connLog != null) {
			
			try {
				connLog.println(message);
			} catch (IOException e) {
				// error in printing the log
			}
		}
	}
	
	
	@Override
	public void onDestroy() {
		
		logger("service destroyed (started = " + isServiceStarted + ")");
		
		if (isServiceStarted == true) {
			stop();
		}
		
		Boolean isStarted = sharedPref.getBoolean(PREF_IS_SERVICE_STARTED, true);
		Log.d("destroy", isStarted.toString());
		
		//Log.d(Constants.TAG_SERVICE, "Service Destroyed");

	}
	
	/**
	 * Always called when startService invoked
	 */
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		logger("service started with intent: " + intent);
		
		super.onStartCommand(intent, flags, startId);
		
		if (intent == null) {
			
			// means from start sticky
			// our service was killed unexpectedly
			
			housekeeping();
			
		} else {
			
			if (intent.getAction().equals(RumpyService.SERVICE_START)) {
				
				start();
				
				//Log.d(Constants.TAG_SERVICE, "ACTION_STOP");
				
			} else if (intent.getAction().equals(RumpyService.SERVICE_STOP)) {
				
				stop();
				stopSelf();
				//Log.d(Constants.TAG_SERVICE, "ACTION_START");
				
			} else if (intent.getAction().equals(RumpyService.SERVICE_RECONNECT)) {
				
				reconnect();
				//reconnectIfNecessary();
				//Log.d(Constants.TAG_SERVICE, "ACTION_RECONNECT");
				
			} else if (intent.getAction().equals(RumpyService.SERVICE_HEARTBEAT)) {
				
				heartbeat();
				//Log.d(Constants.TAG_SERVICE, "ACTION_HEARTBEAT");
				
			}
			
		}
		
		
		
		return START_STICKY;
	}
	
	private void housekeeping() {
		
		stopHeartbeat();
		cancelReconnect();
		// TODO record statistic about unexpected service killing
		
		if (connectivityChanged != null) {
			try {
				//unregisterReceiver(noConnectionReceiver);
				unregisterReceiver(connectivityChanged);
			} catch (IllegalArgumentException e) {
			    if (e.getMessage().contains("Receiver not registered")) {
			        // Ignore this exception. This is exactly what is desired
			    } else {
			        // unexpected, re-throw
			        throw e;
			    }
			}
		}
		
		disconnect();
		start();
		
	}

	/**
	 * method to retrieve persistent service state from shared preferences
	 * @return 	true if service is in running state and persistently saved, false otherwise
	 */
	
	private boolean wasStarted() {
		
		return sharedPref.getBoolean(PREF_IS_SERVICE_STARTED, false);
		
	}
	
	
	
	
	
	/**
	 * Set service state persistently in shared preference
	 * @param isStarted 	true is the service is running
	 */
	
	private void setStarted(boolean isStarted) {
		
		editPref.putBoolean(PREF_IS_SERVICE_STARTED, isStarted);
		editPref.commit();
		
		this.isServiceStarted = isStarted;
		
	}
	
	/**
	 * An attempt to connect to push server
	 */
	
	private synchronized void start() {
		
		Log.d(TAG, "START");
		
		
		if (isServiceStarted == true)
		{
			Log.w(Constants.TAG_SERVICE, "Attempt to start connection that is already active");
			return;
		}
		
		setStarted(true);
		
		//registerReceiver(noConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		registerReceiver(connectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		//Log.d(Constants.TAG_SERVICE, "connecting..");
		logger("connecting...");
		
		/*tcpConnection = new TCPConnection(serverIP, serverPort);
		Thread t = new Thread(tcpConnection);
		t.start(); */
		
		// check for any underlying websocket
		if (webSocket != null) {
			
			//try to ping server
			new Ping(new Ping.PingHandler() {
				
				@Override
				public void onSuccess() {
					register();
				}
				
				@Override
				public void onFailure(Error error) {
					reconnect();
				}
			}).send(webSocket);
			
		} else {
			connect();
		}
		
		
		
		//startWebSocket();
		
	}
	
	private void connect() {
		
		if (webSocket != null) {
			return;
		}
		
		Log.d(TAG, "CONNECT");
		
		boolean connected = connectivityCheck();
		if (connected) {
			open();
		} else {
			reconnect();
		}
		
	}

	private void open() {
		
		Log.d(TAG, "OPEN");
		
		webSocket = new WebSocketConnection();
		
		try {
			
			webSocket.connect(wsuri, new WebSocketConnectionHandler() {
				
				@Override
				public void onOpen() {
					// success!
					Log.d(TAG, "onOpen");
					cancelReconnect();
					register();
				}
				
				

				@Override
				public void onClose(int code, String reason) {
					Log.d(TAG, "onClose: " + reason);
					disconnect();
					scheduleReconnect();
				}
				
				@Override
				public void onTextMessage(String payload) {
					
					Log.d(TAG, "RECEIVE raw JSON");
					Log.d(TAG, payload);
					
					Stanza stanza = JSONHandler.decode(payload);
					process(stanza);
					
				}
				
				@Override
				public void onPongMessage(byte[] payload) {
					
					Ping ping = Ping.container;
	            	//Ping ping = Ping.activePings.get(payload);
	            	if (ping != null) {
	            		ping.pingReceived();
	            	}
	            	
				}
				
			});
		} catch (WebSocketException e) {
			
			// connect attempt failed
			Log.d(TAG, "cannot establish connection");
			reconnect();
			
		}
		
	}
	
	private void register() {
		
		// TODO add observer pattern with onSuccess and onFailure (like ping)
		
		Log.d(TAG, "attempt to register to Rumpy Network");
		String signum = sharedPref.getString("signum", null);
		Stream stream = new Stream();
		/*if (signum == null) {
			stream.setFrom("bagas@rumpy.co");
		} else {
			stream.setFrom(signum);
		}*/
		
		stream.setFrom(signum);
		
		stream.setTo("server.rumpy.co");
		
		String JSON = stream.toJSON();
		//connection.sendTextMessage(JSON);
		//sendStanza(stream);
		webSocket.sendTextMessage(JSON);
		
	}
	
	public static void sendStanza(Stanza stanza, Context context) {
		
		if (shallowCheck(context)) {
			
			String jStanza = stanza.toJSON();
			Log.d(TAG, "SEND raw JSON");
			Log.d(TAG, jStanza);
			webSocket.sendTextMessage(jStanza);
			
		} else {
			
			//check is not pass! queue the message
			Log.d(TAG, "not connected, queue instead");
			StanzaQueue stanzaQueue = StanzaQueue.getInstance();
			stanzaQueue.queueStanza(stanza);
			
		}
	}

	public static void sendStanza(Stanza stanza) {
		
		//TODO queue mechanism
		
		
		// register should use different sending method
		// because shallowCheck will check if the user is registered or not
		if (shallowCheck()) {
			// check pass, stanza is safe to send out
			String jStanza = stanza.toJSON();
			Log.d(TAG, "SEND raw JSON");
			Log.d(TAG, jStanza);
			webSocket.sendTextMessage(jStanza);
			
		} else {
			//check is not pass! queue the message
			Log.d(TAG, "not connected, queue instead");
			StanzaQueue stanzaQueue = StanzaQueue.getInstance();
			stanzaQueue.queueStanza(stanza);
		}
		
	}

	private void reconnect() {
		
		Log.d(TAG, "Attempt to reconnect now...");
		
		if (connectivityCheck()) {
			connect();
		} else {
			Log.d(TAG, "attempt for immediate reconnect failed!!");
			scheduleReconnect();
		}
		
	}
	
	private synchronized void scheduleReconnect() {
		
		Log.d(TAG, "Schedule for reconnection after " + CHECK_FOR_SOCKET_INTERVAL);
		
		Intent i = new Intent();
		i.setClass(this, RumpyService.class);
		i.setAction(SERVICE_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CHECK_FOR_SOCKET_INTERVAL, pi);
		
	}
	
	private void cancelReconnect() {
		
		Log.d(TAG, "Reconnection canceled");
		
		Intent i = new Intent();
		i.setClass(this, RumpyService.class);
		i.setAction(SERVICE_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(pi);
		
	}

	private static boolean connectivityCheck() {
		
		NetworkInfo netInfo = connectMan.getActiveNetworkInfo();
		if (netInfo == null || connectMan == null) {
			return false;
		} else {
			return netInfo != null && netInfo.isConnected();
		}
		
	}
	
	private static boolean connectivityCheck(Context context) {
		
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		
		if (netInfo == null || connectMan == null) {
			return false;
		} else {
			return netInfo != null && netInfo.isConnected();
		}
		
	}

	
	/**
	 * An attempt to stop the connection between client and the server
	 */
	
	private synchronized void stop() {
		
		if (isServiceStarted == false) {
			Log.w(Constants.TAG_SERVICE, "Attempt to stop connection not active.");
			return;
		}
		
		setStarted(false);
		
		try {
			//unregisterReceiver(noConnectionReceiver);
			unregisterReceiver(connectivityChanged);
		} catch (IllegalArgumentException e) {
		    if (e.getMessage().contains("Receiver not registered")) {
		        // Ignore this exception. This is exactly what is desired
		    } else {
		        // unexpected, re-throw
		        throw e;
		    }
		}
		
		cancelReconnect();
		stopHeartbeat();
		disconnect();
		
		
		/*if (tcpConnection != null) {
			tcpConnection.abort();
			tcpConnection = null;
		} */
	}
	
	private void disconnect() {
		
		/*if (connection != null) {
			connection.disconnect();
			connection = null;
		} */
		
		Log.d(TAG, "DISCONNECT");
		
		if (webSocket != null) {
			webSocket.disconnect();
			webSocket = null;
		}
		
		notifyApplication(DISCONNECTED);
		stopHeartbeat();
	}
	
	/**
	 * Send heartbeat message to server
	 */
	
	private synchronized void heartbeat() {
		
		// TODO more accurate heartbeat
		
		Log.d(TAG, "Attempt to send heartbeat");
		
		boolean isConnected = connectivityCheck();
		
		if (!isConnected) {
			Log.d(TAG, "Heartbeat failed, no connection");
		} else {
			Log.d(TAG, "send ping!!");
			new Ping(new Ping.PingHandler() {
				
				@Override
				public void onSuccess() {
					
					Log.d(TAG, "Heartbeat received");
					
				}
				
				@Override
				public void onFailure(Error error) {
					
					Log.d(TAG, "NO heartbeat received!!");
					Log.d(TAG, "Reason: " + error.toString());
					//Log.d(TAG, "disconnect now...");
					//disconnect();
					//scheduleReconnect();
					
				}
			}).send(webSocket);
		}
		
		/*if (isServiceStarted == true && tcpConnection != null) {
			
			String mySignum = sharedPref.getString("signum", null);
			String heartbeatID = StringUtils.randomStringGenerator(5);
			try {
				byte[] heartbeatXML = xml.createHeartbeat(mySignum, "server.mesh.com", heartbeatID);
				messageQueue.put(heartbeatXML);
				//Log.d(Constants.TAG_SERVICE, "heartbeat sent! id =: " + heartbeatID);
				logger("heartbeat sent! id: " + heartbeatID);
			} catch (IllegalArgumentException e) {
				
			} catch (IllegalStateException e) {
				
			} catch (IOException e) {
				
			} catch (InterruptedException e) {
				
			}
		} */
	}
	
	/**
	 * Alarm for sending heartbeat message at HEARTBEAT_INTERVAL even if the device sleep
	 */
	
	private void startHeartbeat() {
		
		Log.d(TAG, "Schedule for heartbeat after " + HEARTBEAT_INTERVAL);
		
		Intent i = new Intent();
		i.setClass(this, RumpyService.class);
		i.setAction(SERVICE_HEARTBEAT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + TIMEOUT, HEARTBEAT_INTERVAL, pi);
		
	}
	
	/**
	 * Cancel the heartbeat alarm
	 */
	
	private void stopHeartbeat() {
		
		Log.d(TAG, "Heartbeat attempt canceled...");
		
		Intent i = new Intent();
		i.setClass(this, RumpyService.class);
		i.setAction(SERVICE_HEARTBEAT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(pi);
		
	}
	
	/**
	 * Try to reconnect to server
	 */
	
	
	
	private synchronized void reconnectIfNecessary() {
		
		/*if (isServiceStarted == true && connection == null) {
			
			logger("reconnecting..");
			//Log.d(Constants.TAG_SERVICE, "reconnecting..");
			
			tcpConnection = new TCPConnection(serverIP, serverPort);
			Thread t = new Thread(tcpConnection);
			t.start();
			
			startWebSocket();
			
		} */
	}
	
	private void process(Stanza stanza) {
		
		if (stanza instanceof Stream) {
			
			Stream stream = (Stream) stanza;
			Log.d(TAG, "Stream Header Received");
			//logger("Stream Header Received");
			//Log.d(TAG, stream.getId());
			Log.d(TAG, "Connected to Rumpy Network --" + stream.getId());
			notifyApplication(REGISTERED);
			startHeartbeat();
			
			if (StanzaQueue.queueInstance != null) {
				// means there are active queue waiting to be drained
				Log.d(TAG, "Drain queue");
				ArrayList<Stanza> pendingStanzas = StanzaQueue.getInstance().dequeStanza();
				for (Stanza s : pendingStanzas) {
					sendStanza(s);
				}
				
				/*pendingStanzas.clear();
				
				if (pendingStanzas.size() == 0) {
					Log.d(TAG, "draining success, queue is now empty");
					StanzaQueue.queueInstance = null;
				}*/
			}
			
			/*if (stanzaQueue != null && stanzaQueue.size() > 0) {
				
				Iterator<String> it = stanzaQueue.iterator();
				while (it.hasNext()) {
					String JSON = it.next();
					if (connection == null) {
						break;
					} else {
						connection.sendTextMessage(JSON);
						it.remove();
					}
				}
			}*/
			
		} else if (stanza instanceof IQ) {
			
			//logger("IQ Header Received");
			Log.d(TAG, "IQ Header Received");
			IQ iq = (IQ) stanza;
			String id = iq.getId();
			IQOperation iqOps = IQOperation.activeIQs.get(id);
			if (iqOps != null) {
				iqOps.iqResponseReceived(iq);
			}
			
		} else if (stanza instanceof Message) {
			
			//logger("Message Header Received");
			Log.d(TAG, "Message Header Received");
			
			Message message = (Message) stanza;
			Message.Type type = message.getType();
			ArrayList<String> receivedReceipts = message.getReceivedReceipt();
			ArrayList<String> readReceipts = message.getReadReceipt();
			
			
			if (message.getType() != null) {
				
				//System.out.println(type.toString());
				
				if (type.equals(Message.Type.CHAT)) {
					
					//logger("message of CHAT received");
					
					String body = message.getBody();
					int state = ChatDetail.STATE_UNREAD;
					boolean fromMe = false;
					String messageID = message.getId();
					long timestamp = System.currentTimeMillis();
					String from = message.getFrom();
					
					//Database db = Database.getInstance(this);
					//db.addMessage(new ChatDetail(body, state, fromMe, messageID, timestamp), from);
					
					ContentValues values = new ContentValues();
					values.put(Database.KEY_MESSAGE_REMOTE_ID, from);
					
					if (fromMe) {
						values.put(Database.KEY_MESSAGE_FROM_ME, 1);
					} else {
						values.put(Database.KEY_MESSAGE_FROM_ME, 0);
					}
					
					values.put("message", body);
					values.put("message_id", messageID);
					values.put("timestamp", timestamp);
					values.put("message_state", state);
					
					resolver.insert(MessageProvider.CONTENT_URI, values);
					
					Intent intent = new Intent(NEW_MESSAGE);
					intent.putExtra("from", message.getFrom());
					intent.putExtra("body", message.getBody());
					intent.putExtra("id", message.getId());
					//sendBroadcast(intent);
					
					MessageDetail messageDetail = new MessageDetail(MessageDetail.MESSAGE_CHAT, from, body, timestamp);
					notificationService.fireNotification(messageDetail);
					
					// if contains request, means the sender ask for receipt. 
					// Sent message to them that message is delivered (has the received tag)
					
					if (message.isReceiptRequested()) {
						
						//logger("message ask for deliveries receipt. Sending receipt");
						
						Message deliveredReceipt = new Message();
						deliveredReceipt.setFrom(message.getTo());
						deliveredReceipt.setTo(message.getFrom());
						deliveredReceipt.setId(StringUtils.randomStringGenerator(6));
						ArrayList<String> receivedIDs = new ArrayList<String>();
						receivedIDs.add(message.getId());
						deliveredReceipt.setReceivedReceipt(receivedIDs);
						sendStanza(deliveredReceipt);
						// send();
					}
					
				}
				
			} else {
				// no type, means receipt
				
				//logger("message received with NULL type");
				
				// received receipt is not empty, means this is received receipt
				if (receivedReceipts != null) {
					
					//logger("received receipt accepted");
					
					ArrayList<String> receivedIDs = message.getReceivedReceipt();
					
					for (String receivedID : receivedReceipts) {
						
						String[] projection = {Database.KEY_MESSAGE_MESSAGE_STATE};
						String selection = Database.KEY_MESSAGE_MESSAGE_ID + "=?";
						String[] selectionArgs = {receivedID};
												
						//int messageState = Database.getInstance(this).getMessageState(receivedID);
						//Cursor c = resolver.query(Uri.withAppendedPath(MessageProvider.CONTENT_URI, "getID"), 
						//		projection, selection, selectionArgs, null);
						Uri uri = Uri.withAppendedPath(MessageProvider.CONTENT_URI, "item/" + receivedID);
						Cursor c = resolver.query(uri, projection, null, null, null);
						if (c == null) Log.d("CHECK!!!!", "nuuuulll!!");
						c.moveToFirst();
						int messageState = c.getInt(0);
						
						if (messageState != -1) {
							
							if (messageState != ChatDetail.STATE_READ) {
								
								//update the message_state in db
								//Database.getInstance(this).updateMessageState(receivedID, ChatDetail.STATE_DELIVERED);
								ContentValues values = new ContentValues();
								values.put(Database.KEY_MESSAGE_MESSAGE_STATE, ChatDetail.STATE_DELIVERED);
								
								String whereClause = Database.KEY_MESSAGE_MESSAGE_ID + "=?" ;
								String[] whereArgs = {receivedID};
								Uri updateUri = Uri.withAppendedPath(MessageProvider.CONTENT_URI, "item/" + receivedID);
								resolver.update(updateUri, values, null, null);
								
							}
						}
						
					}
					
					//send broadcast to notify that a message is delivered
					Intent intentReceived = new Intent(NEW_RECEIPT);
					intentReceived.putExtra("receipt_type", ChatDetail.STATE_DELIVERED);
					//intentReceived.putExtra("id", receivedID);
					intentReceived.putStringArrayListExtra("id", receivedIDs);
					intentReceived.putExtra("from", message.getFrom());
					//sendBroadcast(intentReceived);
		
				}
				
				if (readReceipts != null) {
				
					//logger("read receipt accepted");
					
					ArrayList<String> readIDs = message.getReadReceipt();
					Intent intentRead = new Intent(NEW_RECEIPT);
					
					//Database.getInstance(this).updateMessageState(readIDs, ChatDetail.STATE_READ);
					
					for (String readID : readIDs) {
						
						ContentValues values = new ContentValues();
						values.put(Database.KEY_MESSAGE_MESSAGE_STATE, ChatDetail.STATE_READ);
						
						String whereClause = Database.KEY_MESSAGE_MESSAGE_ID + "=?" ;
						String[] whereArgs = {readID};
						Uri updateUri = Uri.withAppendedPath(MessageProvider.CONTENT_URI, "item/" + readID);
						
						resolver.update(updateUri, values, null, null);
						
					}
					
					intentRead.putExtra("receipt_type", ChatDetail.STATE_READ);
					intentRead.putStringArrayListExtra("id", readReceipts);
					intentRead.putExtra("from", message.getFrom());
					//sendBroadcast(intentRead);
				}
			}
			
		} else if (stanza instanceof Presence) {
			
			Presence presence = (Presence) stanza;
			String type = presence.getType();
			
			if (type.equals(Presence.TYPE_SUBSCRIBE)) {
				Log.d(TAG, "i got presence with type subscribe");
				notificationService.fireNotification(NotificationService.NOTIFICATION_PRESENCE, presence);
			} else if (type.equals(Presence.TYPE_SUBSCRIBED)) {
				
				String newRoster = presence.getFrom();
				IQ iqSetRoster = new IQ();
				iqSetRoster.setFrom(mySignum);
				iqSetRoster.setTo("server.rumpy.co");
				iqSetRoster.setId(StringUtils.randomStringGenerator(6));
				iqSetRoster.setType("set");
				iqSetRoster.setContent("roster");
				iqSetRoster.setQuery(newRoster);
				
				new IQOperation(iqSetRoster, new IQOperation.IQHandler() {
					
					@Override
					public void onResultResponse(IQ iqResponse) {
						
						ArrayList<Roster> rosters = (ArrayList<Roster>) iqResponse.getQuery();
						
						for (Roster roster : rosters) {
							
							ContentValues values = roster.toContentValues();
							resolver.insert(RosterProvider.CONTENT_URI, values);
							
						}
						
					}
					
					@Override
					public void onFailure(Failure reason) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onErrorResponse() {
						// TODO Auto-generated method stub
						
					}
				}).send();
				
			}
		}
	}
	
	private void notifyApplication(String event) {
		
		Intent intent = new Intent(event);
		sendBroadcast(intent);
		
		editPref.putString(PREF_SERVICE_STATUS, event);
		
		serviceStatus = event;
		
	}
	
	private static boolean shallowCheck(Context context) {
		
		boolean isRegistered = serviceStatus.equals(REGISTERED);
		boolean isConnected = connectivityCheck(context);
		boolean isSocketOpen = (webSocket != null);

		
		if (isRegistered && isConnected && isSocketOpen) {
			return true;
		} else {
			return false;
		}
		
	}
	
	private static boolean shallowCheck() {
		
		boolean isRegistered = serviceStatus.equals(REGISTERED);
		boolean isConnected = connectivityCheck();
		boolean isSocketOpen = (webSocket != null);

		
		if (isRegistered && isConnected && isSocketOpen) {
			return true;
		} else {
			return false;
		}
	}
	
	boolean isPingOkay;
	
	private boolean deepCheck() {
		
		boolean isRegistered = serviceStatus.equals(REGISTERED);
		boolean isConnected = connectivityCheck();
		boolean isSocketAvailable = (webSocket != null);
		
		new Ping(new Ping.PingHandler() {
			
			@Override
			public void onSuccess() {
				isPingOkay = true;
			}
			
			@Override
			public void onFailure(Error error) {
				isPingOkay = false;
			}
		});
		
		if (isRegistered && isConnected && isSocketAvailable && isPingOkay) {
			return true;
		} else {
			return false;
		}
	}

	private void decodeMessage(byte[] xmlIn) {
		
		InputStream inXML = new ByteArrayInputStream(xmlIn);
		InputStream inXML2 = new ByteArrayInputStream(xmlIn);
		HashMap<String, String> xmlContent = new HashMap<String, String>();
		int header = 0;
		
		try {
			header = xml.getHeader(inXML);
		} catch (XmlPullParserException e) {
			
		} catch (IOException e) {
			
		}
		
		switch (header) {
		case XML.STREAM:
			logger("Stream Header Received");
			//Log.d(Constants.TAG_SERVICE, "Stream Header Received");
			try {
				xmlContent = xml.regRecv(inXML2);
			} catch (UnknownHostException e) {
				
			} catch (IOException e) {
				
			} catch (XmlPullParserException e) {
				
			}
			break;
			
		case XML.MESSAGE:
			//Log.d(Constants.TAG_SERVICE, "Message Header Received");
			logger("Message Header Received");
			try {
				xmlContent = xml.parseMessage(inXML2);
			} catch (XmlPullParserException e1) {
				
			} catch (IOException e1) {
				
			}
			
			// incoming message is chat. Fire notification about the incoming message and broadcast across apps
			
			if (xmlContent.get("type") != null) {
				
				String type = xmlContent.get("type");
				if (type.equals(XML.MESSAGE_CHAT)) {
					
					final String message = xmlContent.get("body");
					final int state = ChatDetail.STATE_UNREAD;
					final boolean fromMe = false;
					final String messageID = xmlContent.get("id");
					final long timestamp = System.currentTimeMillis();
					final String from = xmlContent.get("from");
					
					db.addMessage(new ChatDetail(message, state, fromMe, messageID, timestamp), from);
					
					Intent intent = new Intent(NEW_MESSAGE);
					intent.putExtra("from", xmlContent.get("from"));
					intent.putExtra("body", xmlContent.get("body"));
					intent.putExtra("id", xmlContent.get("id"));
					sendBroadcast(intent);
					
					MessageDetail messageDetail = new MessageDetail(MessageDetail.MESSAGE_CHAT, from, message, timestamp);
					//fireNotif(xmlContent.get("from"), xmlContent.get("body"));
					notificationService.fireNotification(messageDetail);
					
					// if contains request, means the sender ask for receipt. 
					// Sent message to them that message is delivered (has the received tag)
					if (xmlContent.containsKey("request")) {
						try {
							byte[] receivedReceiptXML = xml.messageReceived(xmlContent.get("to"), 
									xmlContent.get("from"), xmlContent.get("id"));
							//sendMessage(receivedReceiptXML);
							RumpyService.messageQueue.put(receivedReceiptXML);
						} catch (IllegalArgumentException e) {
							
						} catch (IllegalStateException e) {
							
						} catch (IOException e) {
							
						} catch (InterruptedException e) {
							
						}
					}
				}
			}
			
			
			//received "message delivered" notice
			if (xmlContent.containsKey("received")) {
				
				//retrieve the messageID in which is delivered
				final String receivedID = xmlContent.get("received_id");
				ArrayList<String> receivedIDs = new ArrayList<String>();
				int messageState = db.getMessageState(receivedID);
				receivedIDs.add(receivedID);
				
				if (messageState != -1) {
					
					if (messageState != ChatDetail.STATE_READ) {
						
						//update the message_state in db
						db.updateMessageState(receivedID, ChatDetail.STATE_DELIVERED);
						
						//send broadcast to notify that a message is delivered
						Intent intentReceived = new Intent(NEW_RECEIPT);
						intentReceived.putExtra("receipt_type", ChatDetail.STATE_DELIVERED);
						//intentReceived.putExtra("id", receivedID);
						intentReceived.putStringArrayListExtra("id", receivedIDs);
						intentReceived.putExtra("from", xmlContent.get("from"));
						sendBroadcast(intentReceived);
					}
				}
				
			}
			
			// receive the "message read" notice
			if (xmlContent.containsKey("read")) {
				
				int readIndex = Integer.parseInt(xmlContent.get("readIndex"));
				ArrayList<String> readIDs = new ArrayList<String>();
				Intent intentRead = new Intent(NEW_RECEIPT);
				
				for (int i=0; i < readIndex; i++) {
					String readID = xmlContent.get("read_id" + i);
					readIDs.add(readID);
				}
				
				db.updateMessageState(readIDs, ChatDetail.STATE_READ);
				intentRead.putExtra("receipt_type", ChatDetail.STATE_READ);
				intentRead.putStringArrayListExtra("id", readIDs);
				intentRead.putExtra("from", xmlContent.get("from"));
				sendBroadcast(intentRead);
				
				
			}
			
			break;
			
		case XML.IQ:
			//Log.d(Constants.TAG_SERVICE, "IQ Header Received");
			logger("IQ Header Received");
			try {
				xmlContent = xml.parseIQ(inXML2);
				String type = xmlContent.get("type");
				String query_type = xmlContent.get("query_type");
				
				if (type.equalsIgnoreCase("result") && query_type.equalsIgnoreCase("heartbeat")) {
					//putHeartbeat(xmlContent.get("id"));
					RumpyService.heartbeatIDFromServer = xmlContent.get("id");
					//Log.d(Constants.TAG_SERVICE, "heartbeat ID from server: " + xmlContent.get("id"));
					logger("heartbeat ID from server: " + xmlContent.get("id"));
				} else if (type.equalsIgnoreCase("result") && query_type.equalsIgnoreCase("roster")) {
					processRoster(xmlContent);
					//sendBroadcast(new Intent(NEW_ROSTER));
				}
				
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}
		
	}
	
	private ContactDetail getContact(String signum) {
		return contactsMap.get(signum);
	}
	
	private void fireNotif(String from, String message) {
		
		int icon = R.drawable.ic_launcher;
		String tickerText = "New Message from " + from;
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Context context = getApplicationContext();
		String expandedText = message;
		String expandedTitle = from;
		
		String fullname = rosterMap.get(from);
		
		Intent intent = new Intent(this, ConversationActivity.class);
		intent.putExtra("signum", from);
    	intent.putExtra("fullname", fullname);
		PendingIntent launch = PendingIntent.getActivity(context, 0, intent, 0);
		notification.setLatestEventInfo(context, expandedTitle, expandedText, launch);
		
		notifMan.notify(1, notification);
		
		Log.d("NOTIF", "notif fired");
	}
	
	private BroadcastReceiver connectivityChanged = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			
			boolean hasConnectivity = (info != null && info.isConnected()) ? true : false;
			
			logger("Connecting changed: connected=" + hasConnectivity);
			
			if (hasConnectivity) {
				connect();
				//reconnectIfNecessary();
			} else {
				disconnect();
			}
			
		}
	};
	
	private void processRoster (HashMap<String, String> content) {
		
		int itemCount = Integer.parseInt(content.get("itemCount"));
		
		for (int i=1; i <= itemCount; i++) {
			String signum = content.get("item_signum" + i);
			String fullname = content.get("item_fullname" + i);
			db.addContact(new ContactDetail(signum, fullname, "", ""));
			sendBroadcast(new Intent(NEW_ROSTER));
		}
		
		this.isRosterAvailable = true;
	}
	
	private class TCPConnection implements Runnable {

		private final String serverIP;
		private final int serverPort;
		private final Socket socket;
		XML xml;
		
		private volatile boolean abortOnPurpose = false;
		
		public TCPConnection(String serverIP, int serverPort) {
			
			this.serverIP = serverIP;
			this.serverPort = serverPort;
			this.socket = new Socket();
			xml = new XML();
			
		}
		
		public boolean isConnected() {
			
			return socket.isConnected();
			
		}
		
		public boolean isNetworkAvailable() {
			
			NetworkInfo info = connectMan.getActiveNetworkInfo();
			
			if (info == null) {
				return false;
			}
			
			return info.isConnected();
			
		}
		
		@Override
		public void run() {
			
			Socket s = this.socket;
			
			long startTime = System.currentTimeMillis();
			
			try {
				
				s.connect(new InetSocketAddress(serverIP, serverPort), 20000);
				s.setKeepAlive(true);
				logger("Connection established to " + s.getInetAddress() + " port " + s.getPort());
				//Log.d(Constants.TAG_SERVICE + " - Socket", "Connection established to " + s.getInetAddress() + " port " + s.getPort());
				
				register();
				startHeartbeat();
				
				DataInputStream dataIn = new DataInputStream(s.getInputStream());
				DataOutputStream dataOut = new DataOutputStream(s.getOutputStream());
				
				while (isConnected() == true) {
					
					//Log.d(Constants.TAG_SERVICE, "in while state");
					
					Thread.sleep(100);
					
					//Process incoming data
				
					if (dataIn.available() == 0) {
						//continue;
					} else {
						byte[] xmlIn = new byte[dataIn.available()];
						dataIn.read(xmlIn);
						logger("find incoming stanza");
						//Log.d(Constants.TAG_SERVICE + "- InputStream", "find incoming stanza");
						decodeMessage(xmlIn);
					}
					
					// Process outgoing data
					
					if (messageQueue.size() > 0) {
						
						//Log.d(Constants.TAG_SERVICE, "outgoing check");
						byte[] outgoingData = RumpyService.messageQueue.remove();
						int dataLength = outgoingData.length;
						dataOut.write(outgoingData, 0, dataLength);
						dataOut.flush();
						logger("send outgoing stanza");
						//Log.d(Constants.TAG_SERVICE + "- OutputStream", "send outgoing stanza");
						
					}
					
				}
				
				
			} catch (IOException e) {
				logger("IO exception, fail to connect to server");
				//Log.d(Constants.TAG_SERVICE + " - I/O Exception", "fail to connect to server, server is down?");
			} catch (InterruptedException e) {
				// Thread interrupted
			} finally {
				
				if (abortOnPurpose) {
					//Log.d(Constants.TAG_SERVICE + " - Socket", "Socket closed on purpose");
					logger("Socket closed on purpose");
				} else {
					
					try {
						s.close();
					} catch (IOException e) {
						// fail to close socket
					}
					
					synchronized (RumpyService.this) {
						tcpConnection = null;
					}
					
					
					/* If our local interface is still up then the connection
					 * failure must have been something intermittent.  Try
					 * our connection again later (the wait grows with each
					 * successive failure).  Otherwise we will try to
					 * reconnect when the local interface comes back. */
					
					if (isNetworkAvailable() == true) {
						scheduleReconnect();
					}
					
				}
				
			}
			
		}
		
		public void register() {
			
			String signum = sharedPref.getString("signum", null);
			String myIP = "8.8.8.8"; // value not used
			
			try {
				byte[] registrationXML = xml.regSend("server.mesh.com", signum, myIP);
				messageQueue.put(registrationXML);
			} catch (IllegalArgumentException e) {
				
			} catch (IllegalStateException e) {
				
			} catch (IOException e) {
				
			} catch (InterruptedException e) {
				
			}
			
			
		}
		
		public void abort() {
			
			logger("aborting connection..");
			//Log.d(Constants.TAG_SERVICE + " - Socket", "aborting connection..");
			
			abortOnPurpose = true;
			
			try {
				socket.shutdownOutput();
			} catch (IOException e) {
				// I/O error when closing outputstream
			}
			
			try {
				socket.shutdownInput();
			} catch (IOException e) {
				// I/O error when closing inputstream
			}
			
			try {
				socket.close();
			} catch (IOException e) {
				// I/O error when closing socket
			}
		}
		
	}
	
	class NotificationService {
		
		Context context;
		NotificationManager manager;
		NotificationCompat.Builder builder;
		
		public static final int NOTIFICATION_CHAT = 0;
		public static final int NOTIFICATION_GROUP = 1;
		public static final int NOTIFICATION_PRESENCE = 2;
		
		public NotificationService(Context context) {
			
			this.context = context;
			this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			this.builder = new NotificationCompat.Builder(context);
			
		}
		
		public void fireNotification(int whatToNotify, Stanza stanza) {
			
			switch (whatToNotify) {
			case NOTIFICATION_PRESENCE:
				Presence presence = (Presence) stanza;
				presenceNotification(presence);
				break;

			default:
				break;
			}
		}
		
		public void fireNotification(MessageDetail message) {
			
			
			switch (message.getMessageType()) {
			case MessageDetail.MESSAGE_CHAT:
				chatNotification(message);
				break;

			default:
				break;
			}
			
			
			
			
			
			
		}
		
		private void presenceNotification(Presence presence) {
			
			builder.setContentTitle("New Friend Request");
			builder.setContentText(presence.getFrom());
			builder.setWhen(System.currentTimeMillis());
			builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setTicker("New Friend Request");
			builder.setAutoCancel(true);
			
			Intent i = new Intent();
			i.setClass(context, NewContactActivity.class);
			Bundle bundle = presence.getRoster().toBundle();
			i.putExtras(bundle);
			
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
			builder.setContentIntent(pendingIntent);
			
			Notification notification = builder.getNotification();
			notification.defaults = Notification.DEFAULT_ALL;
			manager.notify(NOTIFICATION_PRESENCE, notification);
		}

		private void chatNotification(MessageDetail message) {
			
			String remoteSignum = message.getSignum();
			String messageBody = message.getMessageBody();
			int messageType = message.getMessageType();
			long timestamp = message.getTimestamp();
			
			ContactDetail contact = getContact(remoteSignum);
			String fullname = contact.getFullname();
			String image = contact.getImageThumbName();
			String presence = contact.getLastPresence();
			
			builder.setContentTitle(fullname);
			builder.setContentText(messageBody);
			builder.setWhen(timestamp);
			builder.setSmallIcon(R.drawable.ic_launcher);
			
			//should get real image from sdcard
			if (image.equals("bagas.jpg")) {
				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bagas);
				builder.setLargeIcon(bitmap);
			} else if (image.equals("diska.jpg")) {
				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.diska);
				builder.setLargeIcon(bitmap);
			} else {
				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_thumb);
				builder.setLargeIcon(bitmap);
			}
			
			builder.setTicker("New message from " + fullname);
			
			Intent i = new Intent();
			i.setClass(context, ConversationActivity.class);
			i.putExtra("signum", remoteSignum);
			i.putExtra("fullname", fullname);
			i.putExtra("presence", presence);
			i.putExtra("image", image);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
			builder.setContentIntent(pendingIntent);
			
			builder.setAutoCancel(true);
			
			Notification notification = builder.getNotification();
			notification.defaults = Notification.DEFAULT_ALL;
			manager.notify(NOTIFICATION_CHAT, notification);
			
			
		}
	}
	

}
