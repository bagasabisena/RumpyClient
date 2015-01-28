package co.rumpy.client.android.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import co.rumpy.client.android.R;

import android.content.Context;

public class CustomHTTPClient extends DefaultHttpClient {
	
	final Context context;
	private static final String KEYPASS = "password";
	
	public CustomHTTPClient(Context context) {
		this.context = context;
	}
	
	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		
		SchemeRegistry registry = new SchemeRegistry();
		
		try {
			//String defaultType = KeyStore.getDefaultType();
			KeyStore trustedStore = KeyStore.getInstance("BKS");
			InputStream certificateStream = context.getResources().openRawResource(R.raw.keystore);
			trustedStore.load(certificateStream, KEYPASS.toCharArray());
			certificateStream.close();
			
			SSLSocketFactory sslSocketFactory = new SSLSocketFactory(trustedStore);
			
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 8080));
			registry.register(new Scheme("https", sslSocketFactory, 8443));
			
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new SingleClientConnManager(getParams(), registry);
	}

}
