package co.rumpy.client.android.structure;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import co.rumpy.client.android.utils.Constants;
import co.rumpy.client.android.utils.StringUtils;
import co.rumpy.stanza.Stanza;

public class StanzaQueue {
	
	private static final String TAG = StanzaQueue.class.getSimpleName();
	
	private String id;
	public static StanzaQueue queueInstance = null;
	private LinkedBlockingQueue<Stanza> queue = null;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	
	public static StanzaQueue getInstance() {
		
		if (queueInstance == null) {
			queueInstance = new StanzaQueue();
		}
		
		return queueInstance;
	}
	
	private StanzaQueue() {
		Log.d(TAG, "Stanza queue instance created");
		this.id = StringUtils.randomStringGenerator(4);
		this.queue = new LinkedBlockingQueue<Stanza>();
		queueInstance = this;
		//preferences = context.getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
		//editor = preferences.edit();
		
	}
	
	public String getId() {
		return id;
	}
	
	public LinkedBlockingQueue<Stanza> getQueue() {
		return queue;
	}
	
	public boolean queueStanza(Stanza stanza) {
		return queue.add(stanza);
	}
	
	public ArrayList<Stanza> dequeStanza() {
		ArrayList<Stanza> a = new ArrayList<Stanza>();
		queue.drainTo(a);
		queueInstance = null;
		return a;
	}
	
	
	
	
	
	
	
	

}
