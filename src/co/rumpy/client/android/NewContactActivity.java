package co.rumpy.client.android;

import java.util.ArrayList;

import co.rumpy.client.android.IQOperation.Failure;
import co.rumpy.client.android.IQOperation.IQHandler;
import co.rumpy.client.android.db.RosterProvider;
import co.rumpy.client.android.utils.Constants;
import co.rumpy.stanza.iq.IQ;
import co.rumpy.stanza.iq.IQs;
import co.rumpy.stanza.iq.Roster;
import co.rumpy.stanza.presence.Presence;
import co.rumpy.stanza.presence.Presences;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class NewContactActivity extends FragmentActivity {
	
	String signum;
	String fullname;
	String image;
	String presence;
	SharedPreferences pref;
	String mySignum;
	String myBareSignum;
	ContentResolver resolver;
	
	@Override
	protected void onCreate(Bundle arg0) {
		
		super.onCreate(arg0);
		Bundle bundle = getIntent().getExtras();
		signum = bundle.getString("signum");
		fullname = bundle.getString("fullname");
		image = bundle.getString("image");
		presence = bundle.getString("presence");
		
		pref = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
		mySignum = pref.getString("signum", "");
		myBareSignum = pref.getString(Constants.PREF_TAG_BARESIGNUM, null);
		
		resolver = getContentResolver();
		
		alert();
	}

	private void alert() {
		
		Log.d("TAG", "alert started");
		
		AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
		adBuilder.setTitle("New Contact");
		adBuilder.setCancelable(false);
		
		View view = getLayoutInflater().inflate(R.layout.newcontact_dialog_userdetail, null);
		TextView fullnameView = (TextView) view.findViewById(R.id.newcontact_dialog_textfullname);
		fullnameView.setText(fullname);
		TextView signumView = (TextView) view.findViewById(R.id.newcontact_dialog_textsignum);
		signumView.setText(signum);
		TextView presenceView = (TextView) view.findViewById(R.id.newcontact_dialog_textpresence);
		presenceView.setText(presence);
		adBuilder.setView(view);
		
		adBuilder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Toast.makeText(NewContactActivity.this, "Accept " + fullname, Toast.LENGTH_SHORT).show();
				Presence subscribedPresence = Presences.subscribedPresence(myBareSignum, signum);
				//RumpyService.sendStanza(subscribedPresence);
				RumpyService.sendStanza(subscribedPresence, NewContactActivity.this);
				
				IQ setRoster = IQs.setRoster(mySignum, "server.rumpy.co", signum);
				new IQOperation(setRoster, new IQHandler() {
					
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
				
				NewContactActivity.this.finish();
				
			}
		});
		
		adBuilder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Toast.makeText(NewContactActivity.this, "Decline " + fullname, Toast.LENGTH_SHORT).show();
				Presence unsubscribedPresence = Presences.unsubscribedPresence(myBareSignum, signum);
				//RumpyService.sendStanza(unsubscribedPresence);
				RumpyService.sendStanza(unsubscribedPresence, NewContactActivity.this);
				NewContactActivity.this.finish();
			}
		});
		
		AlertDialog ad = adBuilder.create();
		ad.show();
		
		Log.d("TAG", "alert fired!");
		
	}

}
