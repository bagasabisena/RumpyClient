package co.rumpy.client.android.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;

public class RegisterTask extends AsyncTask<String, Void, String> {
	
	String url = "http://49.50.9.73:80/RumpyServlet/register";

	@Override
	protected String doInBackground(String... params) {
		
		HttpURLConnection connection;
		
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return url;
		
		
	}
	
	

}
