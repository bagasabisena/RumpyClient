package co.rumpy.client.android;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import de.tavendo.autobahn.WebSocket;

public class Ping {
	
	private static final long PING_TIMEOUT = 2 * 1000 * 60;
	
	public enum Error {
		NULL, TIMEOUT, BUSYPING
	}
	
	public static ConcurrentHashMap<byte[], Ping> activePings = new ConcurrentHashMap<byte[], Ping>();
	
	public static Ping container = null;
	
	private PingHandler handler;
	private Timer pingTimeout;
	private TimerTask pingTimeoutTask;
	private byte[] pingID;
	
	public interface PingHandler {
		
		public void onSuccess();
		public void onFailure(Ping.Error error);
		
	}
	
	public Ping(final PingHandler handler) {
		this.handler = handler;
		pingTimeout = new Timer();
		pingID = randomByte();
		//Log.d("Byte Ping ID2", pingID.toString());
		pingTimeoutTask = new TimerTask() {
			
			@Override
			public void run() {
				handler.onFailure(Ping.Error.TIMEOUT);
				activePings.remove(pingID);
				container = null;
			}
		};
	}
	
	
	public void send(WebSocket webSocket) {
		
		if (webSocket == null) {
			// ping fail null socket
			handler.onFailure(Ping.Error.NULL);
			return;
		}
		
		if (container != null) {
			handler.onFailure(Ping.Error.BUSYPING);
			return;
		}
		
		//Log.d("Byte Ping ID1", pingID.toString());
		
		webSocket.sendPingMessage(pingID);
		container = this;
		activePings.put(pingID, this);
		pingTimeout.schedule(pingTimeoutTask, PING_TIMEOUT);
		
	}
	
	public void pingReceived() {
		handler.onSuccess();
		cleanup();
	}
	
	private void cleanup() {
		
		if (pingTimeout != null) {
			pingTimeout.cancel();
		}
		
		container = null;
		activePings.remove(pingID);
		
		
	}
	
	private byte[] randomByte() {
		
		byte[] b = new byte[2];
		new Random().nextBytes(b);
		return b;
	}

}
