package co.rumpy.client.android;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import co.rumpy.stanza.iq.IQ;
import de.tavendo.autobahn.WebSocket;

public class IQOperation {
	
	public static ConcurrentHashMap<String, IQOperation> activeIQs = new ConcurrentHashMap<String, IQOperation>();
	private IQ iqRequest;
	private IQ iqResponse;
	private IQHandler handler;
	private Timer iqTimeout;
	private TimerTask iqTimeoutTask;
	private String id;
	private static final long IQ_TIMEOUT = 2*60*1000;
	
	public interface IQHandler {
		
		public void onResultResponse(IQ iqResponse);
		public void onErrorResponse();
		public void onFailure(Failure reason);
		
	}
	
	public enum Failure {
		TIMEOUT, NULL
	}
	
	public IQOperation(IQ iqRequest, final IQHandler handler) {
		
		this.iqRequest = iqRequest;
		id = iqRequest.getId();
		this.handler = handler;
		iqTimeout = new Timer();
		iqTimeoutTask = new TimerTask() {
			
			@Override
			public void run() {
				handler.onFailure(Failure.TIMEOUT);
				activeIQs.remove(id);
			}
		};
		
	}
	
	public void send() {
		
		WebSocket webSocket = RumpyService.webSocket;
		if (webSocket == null) {
			// iq fail null socket
			handler.onFailure(Failure.NULL);
			return;
		}
		
		String jIqRequest = iqRequest.toJSON();
		webSocket.sendTextMessage(jIqRequest);
		activeIQs.put(id, this);
		iqTimeout.schedule(iqTimeoutTask, IQ_TIMEOUT);
	}
	
	public void iqResponseReceived(IQ iqResponse) {
		
		this.iqResponse = iqResponse;
		String type = iqResponse.getType();
		
		if (type.equals("result")) {
			handler.onResultResponse(iqResponse);
			
		} else if (type.equals("error")) {
			handler.onErrorResponse();
		}
		
		cleanup();
		
	}

	private void cleanup() {
		
		if (iqTimeout != null) {
			iqTimeout.cancel();
		}
		
		activeIQs.remove(id);
	}
	
	

}
