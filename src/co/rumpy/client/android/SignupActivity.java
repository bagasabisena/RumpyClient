package co.rumpy.client.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import co.rumpy.client.android.http.CustomHTTPClient;
import co.rumpy.client.android.utils.Constants;
import co.rumpy.stanza.Signum;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignupActivity extends Activity {
	
	EditText editTextSignum;
	EditText editTextFullname;
	EditText editTextPassword;
	EditText editTextConfirmPassword;
	String confirmPassword;
	TextView signupStatus;
	Button buttonRegister;
	ProgressDialog pd;
	SharedPreferences sp;
	
	String resource = "android";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.signup_layout);
		
		editTextSignum = (EditText) findViewById(R.id.signup_edittext_signum);
		editTextFullname = (EditText) findViewById(R.id.signup_edittext_fullname);
		editTextPassword = (EditText) findViewById(R.id.signup_edittext_password);
		editTextConfirmPassword = (EditText) findViewById(R.id.signup_edittext_confirmpassword);
		editTextConfirmPassword.setOnFocusChangeListener(focusListener);
		//editTextConfirmPassword.setOnClickListener(onConfirmPasswordClickListener);
		signupStatus = (TextView) findViewById(R.id.signup_textview_signupstatus);
		
		buttonRegister = (Button) findViewById(R.id.signup_button_register);
		buttonRegister.setOnClickListener(registerListener);
		buttonRegister.setEnabled(false);
		
	}
	
	private OnClickListener onConfirmPasswordClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			buttonRegister.setEnabled(true);
		}
	};
	
	private OnFocusChangeListener focusListener = new OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			buttonRegister.setEnabled(true);			
		}
	};
	
	private OnClickListener registerListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			String signum = editTextSignum.getText().toString();
			String fullname = editTextFullname.getText().toString();
			String password = editTextPassword.getText().toString();
			String confirmPassword = editTextConfirmPassword.getText().toString();
			
			if(!password.equals(confirmPassword)) {
				Toast.makeText(SignupActivity.this, "Password not match..", Toast.LENGTH_SHORT).show();
				editTextPassword.setText("");
				editTextConfirmPassword.setText("");
				return;
			}
			
			SignupTask signupTask = new SignupTask();
			signupTask.execute(signum, password, fullname);
			
		}
	};
	
	private class CheckSignumAvailabilityTask extends AsyncTask<String, Void, StatusLine> {

		@Override
		protected StatusLine doInBackground(String... params) {
			
			HttpClient client = new CustomHTTPClient(SignupActivity.this);
			String url = "http://49.50.9.73:8080/RumpyServlet/check?signum=" + params[0];
			HttpGet get = new HttpGet(url);
			HttpResponse response = null;
			
			try {
				response = client.execute(get);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			StatusLine sl = response.getStatusLine();
			return sl;
		}
		
	}
	
	private class SignupTask extends AsyncTask<String, Void, StatusLine> {
		
		String signum;
		String password;
		String fullname;
		
		@Override
		protected void onPreExecute() {
			
			super.onPreExecute();
			//pd = ProgressDialog.show(SignupActivity.this, "", "Registering..");
			signupStatus.setText("Registering...");
		}

		@Override
		protected StatusLine doInBackground(String... params) {
			
			HttpClient client = new CustomHTTPClient(SignupActivity.this);
			String url = "http://192.168.2.3:8080/RumpyServlet/register";
			HttpPost post = new HttpPost(url);
			HttpResponse response = null;
			
			signum = params[0];
			password = params[1];
			fullname = params[2];
			
			List<NameValuePair> nvp = new ArrayList<NameValuePair>();
			nvp.add(new BasicNameValuePair("signum", signum));
			nvp.add(new BasicNameValuePair("password", password));
			nvp.add(new BasicNameValuePair("fullname", fullname));
			
			try {
				post.setEntity(new UrlEncodedFormEntity(nvp));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			try {
				response = client.execute(post);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return response.getStatusLine();
		}
		
		@Override
		protected void onPostExecute(StatusLine result) {
			
			int statusCode = result.getStatusCode();
			switch (statusCode) {
			case HttpStatus.SC_OK:
				signupStatus.setText("Registered!");
				
				Signum s = new Signum(signum);
				String bare = s.getBareSignum();
				sp = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = sp.edit();
				editor.putString(Constants.PREF_TAG_SIGNUM, signum);
				editor.putString(Constants.PREF_TAG_PASSWORD, password);
				editor.putString(Constants.PREF_TAG_FULLNAME, fullname);
				editor.putString(Constants.PREF_TAG_BARESIGNUM, bare);
				editor.putString(Constants.PREF_TAG_FULLSIGNUM, signum);
				editor.commit();
				
				//start activity
				startActivity(new Intent(SignupActivity.this, MainTabActivity.class));
				Intent serviceIntent = new Intent();
				serviceIntent.setClass(SignupActivity.this, RumpyService.class);
				serviceIntent.setAction(RumpyService.SERVICE_START);
				startService(serviceIntent);
				finish();
				break;
				
			case HttpStatus.SC_NOT_FOUND:
				String reason = result.getReasonPhrase();
				signupStatus.setText(reason);
				break;

			default:
				break;
			}
			
		}
		
		
	}
	

}
