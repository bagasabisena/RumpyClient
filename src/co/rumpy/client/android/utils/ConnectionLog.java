package co.rumpy.client.android.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class ConnectionLog {
	
	private String mPath;
	private Writer mWriter;
	
	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(" [HH:mm:ss] ");
	
	public ConnectionLog() throws IOException {
		
		File sdcard = Environment.getExternalStorageDirectory();
		open(sdcard.getAbsolutePath() + "/heartbeat.log");
	}
	
	public ConnectionLog(String basePath) throws IOException {
		
		open(basePath);
	}

	protected void open(String basePath) throws IOException {
		
		File f = new File(basePath + "-" + getTodayString());
		mPath = f.getAbsolutePath();
		mWriter = new BufferedWriter(new FileWriter(mPath), 2048);
		
		println("opened log");
		
	}

	public void println(String message) throws IOException {
		
		mWriter.write(TIMESTAMP_FORMAT.format(new Date()));
		mWriter.write(message);
		mWriter.write("\n");
		mWriter.flush();
		
	}

	private String getTodayString() {
		
		SimpleDateFormat df = new SimpleDateFormat("ddMMyyyy-hhmmss");
		return df.format(new Date());
		
	}
	
	public String getPath() {
		
		return this.mPath;
		
	}
	
	public void close() throws IOException {
		
		mWriter.close();
	}

}
