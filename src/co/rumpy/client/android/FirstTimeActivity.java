package co.rumpy.client.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import co.rumpy.client.android.db.Database;
import co.rumpy.client.android.structure.ContactDetail;
import co.rumpy.client.android.utils.Constants;
import co.rumpy.client.android.utils.XML;
import co.rumpy.stanza.iq.IQ;
import co.rumpy.stanza.iq.Roster;

import com.actionbarsherlock.app.SherlockActivity;

public class FirstTimeActivity extends SherlockActivity {
	
	EditText enterID;
	SharedPreferences sp;
	IntentFilter filter;
	ProgressDialog pd;
	Button submit;
	Button next;
	
	XML xml;
	public static final String NEW_ROSTER = "co.rumpy.rumpy.action.NEW_ROSTER";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.first_time_layout);
		
		enterID = (EditText) findViewById(R.id.userid);
		
		submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(submitListener);
		
		next = (Button) findViewById(R.id.next);
		next.setOnClickListener(nextListener);
		next.setEnabled(false);
		
		//filter = new IntentFilter(NEW_ROSTER);
		//registerReceiver(receiver, filter);
		
		sp = getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
		
		xml = new XML();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	OnClickListener submitListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			String signum = enterID.getText().toString();
			SharedPreferences.Editor editor = sp.edit();
			editor.putString("signum", signum);
    		editor.commit();
    		
    		String[] signums = new String[] {signum};
    		backgroundHTTP httpTask = new backgroundHTTP();
    		httpTask.execute(signums);
		}
	};
	
	OnClickListener nextListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			startActivity(new Intent(FirstTimeActivity.this, MainTabActivity.class));
			Intent serviceIntent = new Intent();
			serviceIntent.setClass(FirstTimeActivity.this, RumpyService.class);
			serviceIntent.setAction(RumpyService.SERVICE_START);
			startService(serviceIntent);
			finish();
			
		}
	};
	
	private class backgroundHTTP extends AsyncTask<String, Void, String> {
		
		private ProgressDialog pd;
		InputStream in = null;
		BufferedReader reader = null;
		StringBuilder sb = null;
		String JSON;
    	Map<String, String> xmlContent = new HashMap<String, String>();

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			pd = ProgressDialog.show(FirstTimeActivity.this, "", "Fetching Roster from Server...");
			
		}
		
		@Override
		protected String doInBackground(String... signums) {
			// TODO Auto-generated method stub
			
			for (String signum : signums) {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				String uri = "http://49.50.9.73:80/RumpyServlet/get?signum=" + signum; 
				HttpGet httpGet = new HttpGet(uri);
				//HttpPost httpPost = new HttpPost("http://49.50.9.73/post/");
				
				//List<NameValuePair> postElement = new ArrayList<NameValuePair>();
				//postElement.add(new BasicNameValuePair("signum", signum));
				
				try {
					HttpResponse httpResponse = httpClient.execute(httpGet);
					HttpEntity httpEntity = httpResponse.getEntity();
					in = httpEntity.getContent();
					
					reader = new BufferedReader(new InputStreamReader(in));
					sb = new StringBuilder();
					String line;
					
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				JSON = sb.toString();
				
				IQ iq = new IQ();
				iq.fromJSON(JSON);
				
				//Insert roster to android sqlite database
				Database db = Database.getInstance(FirstTimeActivity.this);
				
				if (iq.getTo().equals(signum)) {
					
					// correct roster
					@SuppressWarnings("unchecked")
					ArrayList<Roster> rosters = (ArrayList<Roster>) iq.getQuery();
					for (Roster roster : rosters) {
						
						String sig = roster.getSignum();
						String fullname = roster.getFullname();
						String image = roster.getImage();
						String presence = roster.getPresence();
						boolean isAvailable = db.isContactAvailable(sig);
						if (!isAvailable) {
							db.addContact(new ContactDetail(sig, fullname, image, presence));
						}
						
						Log.d(fullname, presence);
					}
				}
				
			}
			
			/*int itemCount = Integer.parseInt(xmlContent.get("itemCount"));
			
			for (int i=0; i < itemCount; i++) {
				String signum = xmlContent.get("item_signum" + i);
				String fullname = xmlContent.get("item_fullname" + i);
				String image = xmlContent.get("item_image" + i);
				String presence = xmlContent.get("item_presence" + i);
				boolean isAvailable = db.isContactAvailable(signum);
				if (!isAvailable) {
					db.addContact(new ContactDetail(signum, fullname, image, presence));
				}	
			}*/
			
			return JSON;
			
			
		}
		
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			pd.dismiss();
			next.setEnabled(true);
			submit.setEnabled(false);
		}
		
	}

}
