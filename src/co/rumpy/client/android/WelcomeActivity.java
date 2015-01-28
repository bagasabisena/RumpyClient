package co.rumpy.client.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WelcomeActivity extends Activity {
	
	Button signup;
	Button login;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_layout);
		
		signup = (Button) findViewById(R.id.welcome_textview_signup);
		signup.setOnClickListener(signupListener);
		
		login = (Button) findViewById(R.id.welcome_textview_login);
		login.setOnClickListener(loginListener);
		
	}
	
	OnClickListener signupListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent signupIntent = new Intent(WelcomeActivity.this, SignupActivity.class);
			startActivity(signupIntent);
			finish();
			
		}
	};
	
	OnClickListener loginListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent loginIntent = new Intent(WelcomeActivity.this, FirstTimeActivity.class);
			startActivity(loginIntent);
			finish();
			
		}
	};

}
