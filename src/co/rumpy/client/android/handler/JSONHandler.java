package co.rumpy.client.android.handler;

import org.json.JSONException;
import org.json.JSONObject;

import co.rumpy.stanza.Stanza;
import co.rumpy.stanza.iq.IQ;
import co.rumpy.stanza.message.Message;
import co.rumpy.stanza.presence.Presence;
import co.rumpy.stanza.stream.Stream;

public class JSONHandler {
	
	public JSONHandler() {
		// 
	}
	
	public static Stanza decode(String JSON) {
		
		Stanza stanza = null;
		
		try {
			
			JSONObject jStanza = new JSONObject(JSON);
			if (jStanza.has("stream")) {
				// stream stanza received
				stanza = new Stream();
				stanza = stanza.fromJSON(JSON);
			} else if (jStanza.has("message")) {
				stanza = new Message();
				stanza = stanza.fromJSON(JSON);
			} else if (jStanza.has("iq")) {
				stanza = new IQ();
				stanza = stanza.fromJSON(JSON);
			} else if (jStanza.has("presence")) {
				stanza = new Presence();
				stanza = stanza.fromJSON(JSON);
			} else {
				// ERROR
				System.out.println("ERROR in JSON decoding process");
				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return stanza;
		
	}
	
	

}
