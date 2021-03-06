package co.rumpy.client.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartAtBootServiceReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent();
			i.setClass(context, RumpyService.class);
			i.setAction(RumpyService.SERVICE_START);
			context.startService(i);
		}
		
	}

}
