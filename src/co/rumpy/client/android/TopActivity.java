package co.rumpy.client.android;

import co.rumpy.client.android.utils.Constants;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class TopActivity extends SherlockActivity {
	
SharedPreferences sp;
String signum = null;
private final int SPLASH_LENGTH = 1*1000; //1 seconds
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		sp = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        this.signum = sp.getString("signum", null);
        setContentView(R.layout.top_layout);
		//String signum = "bagas@rumpy.co";
        
	}
	
	@Override
		protected void onResume() {
			
			super.onResume();
			
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					
					if (signum == null) {
			        	//Log.d("SIGNUM", signum);
			        	Intent intentFirstTime = new Intent(TopActivity.this, WelcomeActivity.class);
			        	startActivity(intentFirstTime);
			        	finish();
			        } else {
			    		startActivity(new Intent(TopActivity.this, MainTabActivity.class));
			    		boolean isStarted = sp.getBoolean(RumpyService.PREF_IS_SERVICE_STARTED, false);
			    		if (!isStarted) {
			    			Intent i = new Intent();
			    			i.setClass(TopActivity.this, RumpyService.class);
			    			i.setAction(RumpyService.SERVICE_START);
			    			startService(i);
			    		}
			    		
			    		finish();
			        }
					
				}
			}, SPLASH_LENGTH);
			
			
		}

}
