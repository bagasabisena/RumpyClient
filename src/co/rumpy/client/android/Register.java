package co.rumpy.client.android;

import java.util.Timer;
import java.util.TimerTask;

import co.rumpy.stanza.stream.Stream;

public class Register {
	
	public enum Error {
		CHALLENGED, UNREGISTERED, INTERNAL_SERVER_ERROR, TIMEOUT
	}
	
public static Register container = null;
	
	private RegisterHandler handler;
	private Timer registerTimeout;
	private TimerTask registerTimeoutTask;
	private Stream stream;
	
	public interface RegisterHandler {
		
		public void onSuccess();
		public void onFailure(Register.Error error);
		
	}
	
	public Register(final RegisterHandler handler) {
		
		this.handler = handler;
		registerTimeout = new Timer();
		
		registerTimeoutTask = new TimerTask() {
			
			@Override
			public void run() {
				handler.onFailure(Register.Error.TIMEOUT);
				//activePings.remove(pingID);
				container = null;
			}
		};
	}
	
	

}
