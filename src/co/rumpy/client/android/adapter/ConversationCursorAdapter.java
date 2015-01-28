package co.rumpy.client.android.adapter;

import java.sql.Date;
import java.text.SimpleDateFormat;

import co.rumpy.client.android.R;
import co.rumpy.client.android.adapter.ConversationArrayAdapter.ViewHolder;
import co.rumpy.client.android.structure.ChatDetail;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ConversationCursorAdapter extends CursorAdapter {
	
	private final Activity context;

	public ConversationCursorAdapter(Activity context, Cursor c, int flags) {
		super(context, c, flags);
		this.context = context;
	}
	
	static class ViewHolder {
		public TextView leftMessage;
		public ImageView leftMessageStateImage;
		public TextView leftTime;
		public TextView rightMessage;
		public ImageView rightMessageStateImage;
		public TextView rightTime;
		public RelativeLayout leftLayout;
		public RelativeLayout rightLayout;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		
		ViewHolder holder = (ViewHolder) view.getTag();
		
		// initialize both layout to visible state, why?
		// because if not, the layout state (either left or right) can be in gone state, and
		// the view won't be visible even though the view is set with value
		holder.leftLayout.setVisibility(View.VISIBLE);
		holder.rightLayout.setVisibility(View.VISIBLE);
		
		boolean isFromMe = false;
		
		switch (cursor.getInt(6)) {
		case 0:
			isFromMe = false;
			break;
			
		case 1:
			isFromMe = true;
			break;
		}
		
		if (isFromMe) {
			
			// Set the textview on the left with the message
			//String message = chatDetail.getMessage();
			String message = cursor.getString(2);
			holder.rightMessage.setText(message);
			
			//long millis = chatDetail.getTimestamp();
			long millis = cursor.getLong(5);
			String time = toDateString(millis);
			holder.rightTime.setText(time);
			
			holder.rightLayout.setBackgroundResource(R.drawable.baloon_self);
			
			// Set the state image based on the message state (sent, delivered, read)
			//int state = chatDetail.getState();
			int state = cursor.getInt(3);
			switch (state) {
			case ChatDetail.STATE_SENT:
				holder.rightMessageStateImage.setImageResource(R.drawable.sent);
				break;
				
			case ChatDetail.STATE_DELIVERED:
				holder.rightMessageStateImage.setImageResource(R.drawable.delivered);
				break;
				
			case ChatDetail.STATE_READ:
				holder.rightMessageStateImage.setImageResource(R.drawable.read);
				break;

			default:
				break;
			}
			
			// Set the opposite (right) layout (the layout for incoming message) to gone			
			holder.leftLayout.setVisibility(View.GONE);
			
		} else {
			
			//String message = chatDetail.getMessage();
			String message = cursor.getString(2);
			holder.leftMessage.setText(message);
			
			//long millis = chatDetail.getTimestamp();
			long millis = cursor.getLong(5);
			String time = toDateString(millis);
			holder.leftTime.setText(time);
			
			holder.leftLayout.setBackgroundResource(R.drawable.baloon_remote);
			
			holder.rightLayout.setVisibility(View.GONE);
		}
		
		
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup group) {
		
		View rowView = LayoutInflater.from(context).inflate(R.layout.layout_row_conversation, group, false);
		
		ViewHolder viewHolder = new ViewHolder();
		
		viewHolder.leftLayout = (RelativeLayout) rowView.findViewById(R.id.left_hand);
		viewHolder.leftMessage = (TextView) rowView.findViewById(R.id.message_left);
		viewHolder.leftMessageStateImage = (ImageView) rowView.findViewById(R.id.image_state_left);
		viewHolder.leftTime = (TextView) rowView.findViewById(R.id.time_left);
		
		viewHolder.rightLayout = (RelativeLayout) rowView.findViewById(R.id.right_hand);
		viewHolder.rightMessage = (TextView) rowView.findViewById(R.id.message_right);
		viewHolder.rightMessageStateImage = (ImageView) rowView.findViewById(R.id.image_state_right);
		viewHolder.rightTime = (TextView) rowView.findViewById(R.id.time_right);
		
		rowView.setTag(viewHolder);
		
		return rowView;
	}
	
	private String toDateString (long millis) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		Date date = new Date(millis);
		return sdf.format(date);
	}

}
