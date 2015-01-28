package co.rumpy.client.android.adapter;

import java.util.Calendar;
import java.util.Date;

import co.rumpy.client.android.R;
import co.rumpy.client.android.structure.ChatDetail;
import co.rumpy.client.android.structure.MessageDetail;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.CursorAdapter;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class RecentChatCursorAdapter extends CursorAdapter {
	
	private final Context context;
	
	public RecentChatCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		this.context = context;
	}
	
	static class ViewHolder {
		
		public ImageView thumbImageView;
		public TextView fullnameTextView;
		public TextView messageTextView;
		public TextView timestampTextView;
		
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		
		ViewHolder holder = (ViewHolder) view.getTag();
		
		String thumbImage = cursor.getString(2);
		if (thumbImage.equals("bagas.jpg")) {
			holder.thumbImageView.setImageResource(R.drawable.bagas); // this is wrong, should get from internal storage
		} else if (thumbImage.equals("diska.jpg")){
			holder.thumbImageView.setImageResource(R.drawable.diska);
		} else {
			holder.thumbImageView.setImageResource(R.drawable.thumb);
		}
		
		String fullname = cursor.getString(1);
		holder.fullnameTextView.setText(fullname);
		
		String message = cursor.getString(4);
		holder.messageTextView.setText(message);
		
		int messageState = cursor.getInt(6);
		if (messageState == ChatDetail.STATE_UNREAD) {
			holder.fullnameTextView.setTypeface(null, Typeface.BOLD);
			holder.messageTextView.setTypeface(null, Typeface.BOLD);
			holder.timestampTextView.setTypeface(null, Typeface.BOLD);
		}
		
		long timestamp = cursor.getLong(5);
		String timeString = toDateString(timestamp);
		holder.timestampTextView.setText(timeString);
		
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup group) {
		
		View rowView = LayoutInflater.from(context).inflate(R.layout.layout_row_recentchat, group, false);
		
		ViewHolder holder = new ViewHolder();
		holder.thumbImageView = (ImageView) rowView.findViewById(R.id.recent_imageview_contactimage);
		holder.fullnameTextView = (TextView) rowView.findViewById(R.id.recent_textview_fullname);
		holder.messageTextView = (TextView) rowView.findViewById(R.id.recent_textview_lastchat);
		holder.timestampTextView = (TextView) rowView.findViewById(R.id.recent_textview_time);
		
		rowView.setTag(holder);
		return rowView;
		
	}
	
	private String toDateString(long timestamp) {
		
		long timestampNow = System.currentTimeMillis();
		Time messageTime = new Time();
		messageTime.set(timestamp);
		Time timeNow = new Time();
		timeNow.set(timestampNow);
		
		int interval = timeNow.yearDay - messageTime.yearDay;
		
		if (interval == 0) {
			return messageTime.format("%R");
		} else if (interval == 1) {
			return "Yesterday";
		} else if ((interval > 1) && (interval < 7)) {
			return messageTime.format("%A");
		} else {
			return messageTime.format("%d/%m");
		}
		
	}

}

