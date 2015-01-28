package co.rumpy.client.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import co.rumpy.client.android.IQOperation.Failure;
import co.rumpy.client.android.utils.Constants;
import co.rumpy.client.android.utils.StringUtils;
import co.rumpy.stanza.iq.IQ;
import co.rumpy.stanza.iq.Roster;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AddContactActivity extends SherlockFragmentActivity {
	
	Button findContactButton;
	EditText contactEditText;
	SharedPreferences sharedPref;
	ProgressDialog progressDialog;
	
	FrameLayout fragmentContainer;
	
	private static final String TAG = AddContactActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle arg0) {
		
		super.onCreate(arg0);
		
		setContentView(R.layout.add_contact);
		findContactButton = (Button) findViewById(R.id.find_contact);
		findContactButton.setOnClickListener(findListener);
		contactEditText = (EditText) findViewById(R.id.add_contact_text);
		sharedPref = getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
		fragmentContainer = (FrameLayout) findViewById(R.id.addcontact_frame_container);
		
	}
	
	OnClickListener findListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			String targetSignum = contactEditText.getText().toString();
			
			progressDialog = ProgressDialog.show(AddContactActivity.this, "", "Search for user..");
			
			IQ getUserIQ = new IQ();
			getUserIQ.setTo("server.rumpy.co");
			getUserIQ.setFrom(sharedPref.getString("signum", null));
			getUserIQ.setId(StringUtils.randomStringGenerator(6));
			getUserIQ.setType("get");
			getUserIQ.setContent("user");
			getUserIQ.setQuery(targetSignum);
			
			Log.d(TAG, getUserIQ.toJSON());
			
			new IQOperation(getUserIQ, new IQOperation.IQHandler() {
				
				@Override
				public void onResultResponse(IQ iqResponse) {
					
					progressDialog.dismiss();
					Roster roster = (Roster) iqResponse.getQuery();
					Toast.makeText(AddContactActivity.this, "IQ result received", Toast.LENGTH_SHORT).show();
					Log.d(TAG, "IQ result received");
					
					Bundle rosterBundle = roster.toBundle();
					AddContactResultFragment frag = new AddContactResultFragment();
					frag.setArguments(rosterBundle);
					
					FragmentManager fm = getSupportFragmentManager();
					FragmentTransaction ft = fm.beginTransaction();
					
					if (fragmentContainer != null) {
						ft.replace(R.id.addcontact_frame_container, frag);
					} else {
						ft.add(R.id.addcontact_frame_container, frag);
					}
					
					ft.commit();
					
				}
				
				@Override
				public void onFailure(Failure reason) {
					progressDialog.dismiss();
					Toast.makeText(AddContactActivity.this, "IQ operation fail: " + reason.toString(), Toast.LENGTH_SHORT).show();
					Log.d(TAG, "fail: " + reason.toString());
				}
				
				@Override
				public void onErrorResponse() {
					progressDialog.dismiss();
					Toast.makeText(AddContactActivity.this, "IQ error received..", Toast.LENGTH_SHORT).show();
					Log.d(TAG, "IQ error received..");
				}
			}).send();
			
		}
	};
	
	

}
