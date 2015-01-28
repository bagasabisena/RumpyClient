package co.rumpy.client.android.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class XML {
	
	public static final int STREAM = 0;
	public static final int MESSAGE = 1;
	public static final int IQ = 2;
	
	public static final String MESSAGE_CHAT = "chat";
	public static final String MESSAGE_GROUP = "group";
	
	HashMap<String, String> xmlContent;
	
	public XML() {
		xmlContent = new HashMap<String, String>();
	}
	
	public byte[] regSend(String server, String me, String ip) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		serializer.startTag(null, "stream");
		serializer.attribute(null, "to", server); //next time should get from shared preference
		serializer.attribute(null, "from", me); //next time should get from shared preference
		serializer.attribute(null, "ip", ip); //next time should get from shared preference
		serializer.endTag(null, "stream");
		serializer.endDocument();
		serializer.flush();
		
		byte[] xml = outXML.toByteArray();
		return xml;
	}
	
	public HashMap<String, String> regRecv(InputStream in) throws UnknownHostException, IOException, XmlPullParserException {
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		//HashMap<String, String> xmlContent = new HashMap<String, String>();
		//parser.setInput(socket.getInputStream(), "UTF-8");
		parser.setInput(in, "UTF-8");
		int eventType = parser.getEventType();
		String tagName = null;
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				
				if (tagName.equalsIgnoreCase("stream")) {
					xmlContent.put("header", "stream");
					for (int i=0; i<parser.getAttributeCount();i++) {
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("to")) {
							String toAddress = parser.getAttributeValue(i);
							xmlContent.put("to", toAddress);
						} else if (attribute.equalsIgnoreCase("from")) {
							String fromAddress = parser.getAttributeValue(i);
							xmlContent.put("from", fromAddress);
						} else {
							String id = parser.getAttributeValue(i);
							xmlContent.put("id", id);
						}
					}
				}
				
				if (tagName.equalsIgnoreCase("message")) {
					xmlContent.put("header", "message");
					for (int i=0; i<parser.getAttributeCount();i++) {
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("to")) {
							String toAddress = parser.getAttributeValue(i);
							xmlContent.put("to", toAddress);
						} else if (attribute.equalsIgnoreCase("from")) {
							String fromAddress = parser.getAttributeValue(i);
							xmlContent.put("from", fromAddress);
						} else {
							String type = parser.getAttributeValue(i);
							xmlContent.put("type", type);
						}
					}
					
				}
				
				break;
				
			case XmlPullParser.TEXT:
				String body = parser.getText();
				Log.d("BODY", body);
				xmlContent.put("body", body);
				
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				break;
					
			}
			eventType = parser.next();
		}
		
		return xmlContent;
	}
	
	public byte[] messageSend (String from, String to, String message, String randomID, String messageType) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		//serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		serializer.startTag(null, "message");
		serializer.attribute(null, "id", randomID);
		serializer.attribute(null, "type", messageType); //next time should get from shared preference
		serializer.attribute(null, "from", from); //next time should get from shared preference
		serializer.attribute(null, "to", to); //next time should get from shared preference
		
			serializer.startTag(null, "body");
			serializer.text(message);
			serializer.endTag(null, "body");
			
			if (messageType.equalsIgnoreCase(MESSAGE_CHAT)) {
				serializer.startTag(null, "request");
				serializer.endTag(null, "request");
			}
		
		serializer.endTag(null, "message");
		serializer.endDocument();
		serializer.flush();
		
		byte[] xml = outXML.toByteArray();
		return xml;
		
		
	}
	
	public byte[] createHeartbeat(String from, String to, String id) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		serializer.startTag(null, "iq");
		serializer.attribute(null, "from", from);
		serializer.attribute(null, "to", to);
		serializer.attribute(null, "type", "get");
		serializer.attribute(null, "id", id);
			
			serializer.startTag(null, "query");
			serializer.attribute(null, "type", "heartbeat");
			serializer.endTag(null, "query");
			
		serializer.endTag(null, "iq");
		serializer.endDocument();
		serializer.flush();
			
		byte[] xml = outXML.toByteArray();
		return xml;
		
	}
	
	public String randomStringGenerator (int length) {
		
		Random random = new Random((new Date()).getTime());
		
		char[] values = {'a','b','c','d','e','f','g','h','i','j',
	               'k','l','m','n','o','p','q','r','s','t',
	               'u','v','w','x','y','z','0','1','2','3',
	               '4','5','6','7','8','9'};
		String out = "";
		
		for (int i=0; i<length; i++) {
			int idx = random.nextInt(values.length);
			out += values[idx];
		}
		
		return out;
	}
	
	public int getHeader (InputStream inXML) throws XmlPullParserException, IOException {
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		parser.setInput(inXML, "UTF-8");
		int eventType = parser.getEventType();
		String tagName = null;
		int headerType = 0;
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase("stream")) {
					headerType = STREAM;
				} else if (tagName.equalsIgnoreCase("message")) {
					headerType = MESSAGE;
				} else if (tagName.equalsIgnoreCase("iq")){
					headerType = IQ;
				}
				
				
				break;

			default:
				break;
			}
			
			eventType = parser.next();
		}
		
		return headerType;
		
	}
	
	public HashMap<String, String> parseIQ(InputStream inXML) throws XmlPullParserException, IOException {
		
		HashMap<String, String> xmlContent = new HashMap<String, String>();
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		parser.setInput(inXML, "UTF-8");
		int eventType = parser.getEventType();
		String tagName = null;
		int itemIndex = 0;
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase("iq")) {
					xmlContent.put("header", "iq");
					for (int i=0; i<parser.getAttributeCount();i++) {
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("to")) {
							String toAddress = parser.getAttributeValue(i);
							xmlContent.put("to", toAddress);
						} else if (attribute.equalsIgnoreCase("from")) {
							String fromAddress = parser.getAttributeValue(i);
							xmlContent.put("from", fromAddress);
						} else if (attribute.equalsIgnoreCase("id")) {
							String iqID = parser.getAttributeValue(i);
							xmlContent.put("id", iqID);
						} else {
							String type = parser.getAttributeValue(i);
							Log.d("ATTRIBUTE", type);
							xmlContent.put("type", type);
						}
					}
				} else if (tagName.equalsIgnoreCase("query")){
					for (int i=0; i<parser.getAttributeCount();i++) {
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("type")) {
							String queryType = parser.getAttributeValue(i);
							xmlContent.put("query_type", queryType);
						}
					}
				} else if (tagName.equalsIgnoreCase("item")){
					for (int i=0; i<parser.getAttributeCount();i++) {
						String attribute = parser.getAttributeName(i);
						if (attribute.equals("signum")) {
							String signum = parser.getAttributeValue(i);
							xmlContent.put("item_signum" + itemIndex, signum);
						} else if (attribute.equals("fullname")) {
							String fullname = parser.getAttributeValue(i);
							xmlContent.put("item_fullname" + itemIndex, fullname);
						} else if (attribute.equals("image")) {
							String image = parser.getAttributeValue(i);
							xmlContent.put("item_image" + itemIndex, image);
						} else if (attribute.equals("presence")) {
							String presence = parser.getAttributeValue(i);
							xmlContent.put("item_presence" + itemIndex, presence);
						}
					}
					
					itemIndex++;
				}
				
				
				break;
				
			case XmlPullParser.TEXT:
				if (tagName.equalsIgnoreCase("heartbeat")) {
					xmlContent.put("heartbeat_id", parser.getText());
				}

			default:
				break;
			}
			
			eventType = parser.next();
		}
		
		if (itemIndex != 0) {
			String itemIndexToString = Integer.toString(itemIndex);
			xmlContent.put("itemCount", itemIndexToString);
		}
		
		return xmlContent;
		
	}
	
	public HashMap<String, String> parseMessage(InputStream inXML) throws XmlPullParserException, IOException {
		
		HashMap<String, String> xmlContent = new HashMap<String, String>();
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		parser.setInput(inXML, "UTF-8");
		int eventType = parser.getEventType();
		String tagName = null;
		int readIndex = 0;
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase("message")) {
					xmlContent.put("header", "message");
					for (int i=0; i<parser.getAttributeCount();i++) {
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("to")) {
							String toAddress = parser.getAttributeValue(i);
							xmlContent.put("to", toAddress);
						} else if (attribute.equalsIgnoreCase("from")) {
							String fromAddress = parser.getAttributeValue(i);
							xmlContent.put("from", fromAddress);
						} else if (attribute.equalsIgnoreCase("type")){
							String type = parser.getAttributeValue(i);
							xmlContent.put("type", type);
						} else {
							String message_id = parser.getAttributeValue(i);
							xmlContent.put("id", message_id);
						}
					}
				} else if (tagName.equalsIgnoreCase("request")) {
					xmlContent.put("request", "true");
				} else if (tagName.equalsIgnoreCase("received")) {
					xmlContent.put("received", "true");
					String attribute = parser.getAttributeValue(0);
					xmlContent.put("received_id", attribute);
				} else if (tagName.equalsIgnoreCase("read")) {
					xmlContent.put("read", "true");
					String attribute = parser.getAttributeValue(0);
					xmlContent.put("read_id" + readIndex, attribute);
					readIndex++;
				}
				
				break;
				
			case XmlPullParser.TEXT:
				if (tagName.equalsIgnoreCase("body")) {
					xmlContent.put("body", parser.getText());
				}
				break;

			default:
				break;
			}
			
			eventType = parser.next();
		}
		
		if (readIndex != 0) {
			String readIndexToString = Integer.toString(readIndex);
			xmlContent.put("readIndex", readIndexToString);
			
		}
		
		
		
		return xmlContent;
	
	}
	
	public byte[] createIQ_signIn(String iq_id, String mesh_id, String fullname, String key) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		// <iq type="get" id="123ab">
		serializer.startTag(null, "iq");
		serializer.attribute(null, "type", "get");
		serializer.attribute(null, "id", iq_id);
			
			// <query type="signin">
			serializer.startTag(null, "query");
			serializer.attribute(null, "type", "signin");
				// <signum>bagasabisena@mesh.com</signum>
				serializer.startTag(null, "signum");
				serializer.text(mesh_id);
				serializer.endTag(null, "signum");
				// <fullname>Bagas Abisena</fullname>
				serializer.startTag(null, "fullname");
				serializer.text(fullname);
				serializer.endTag(null, "fullname");
				// <key>123abc</key>
				serializer.startTag(null, "key");
				serializer.text(key);
				serializer.endTag(null, "key");
			serializer.endTag(null, "query");
			
		serializer.endTag(null, "iq");
		serializer.endDocument();
		serializer.flush();
			
		byte[] xml = outXML.toByteArray();
		return xml;
	}
	
	public byte[] messageReceived (String from, String to, String id) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		serializer.startTag(null, "message");
		serializer.attribute(null, "to", to);
		serializer.attribute(null, "from", from);
		serializer.attribute(null, "id", StringUtils.randomStringGenerator(6));
		
			serializer.startTag(null, "received");
			serializer.attribute(null, "id", id);
			serializer.endTag(null, "received");
			
		serializer.endTag(null, "message");
		serializer.endDocument();
		serializer.flush();
		
		byte[] xml = outXML.toByteArray();
		return xml;
	}
	
	
	public byte[] messageRead (String from, String to, String id) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		serializer.startTag(null, "message");
		serializer.attribute(null, "to", to);
		serializer.attribute(null, "from", from);
		serializer.attribute(null, "id", StringUtils.randomStringGenerator(6));
		
			serializer.startTag(null, "read");
			serializer.attribute(null, "id", id);
			serializer.endTag(null, "read");
			
		serializer.endTag(null, "message");
		serializer.endDocument();
		serializer.flush();
		
		byte[] xml = outXML.toByteArray();
		return xml;
	}
	
	public byte[] messageReadMulti (String from, String to, List<String> ids) throws IllegalArgumentException, IllegalStateException, IOException {
		
		ByteArrayOutputStream outXML = new ByteArrayOutputStream();
		
		XmlSerializer serializer =Xml.newSerializer();
		serializer.setOutput(outXML, "UTF-8");
		
		serializer.startDocument(null, true);
		serializer.startTag(null, "message");
		serializer.attribute(null, "to", to);
		serializer.attribute(null, "from", from);
		serializer.attribute(null, "id", StringUtils.randomStringGenerator(6));
		
			for (String id : ids) {
				
				serializer.startTag(null, "read");
				serializer.attribute(null, "id", id);
				Log.d("READ ID", id);
				serializer.endTag(null, "read");
				
			}
			
		serializer.endTag(null, "message");
		serializer.endDocument();
		serializer.flush();
		
		byte[] xml = outXML.toByteArray();
		return xml;
	}

}
