package co.rumpy.client.android.adapter;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import co.rumpy.client.android.R;
import co.rumpy.client.android.structure.ChatDetail;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ConversationArrayAdapter extends ArrayAdapter<ChatDetail> {
	
	private final Activity context;
	private ArrayList<ChatDetail> chatDetailList;
	
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
	
	public ConversationArrayAdapter (Activity context, ArrayList<ChatDetail> chatDetailList) {
		super(context, R.layout.layout_row_conversation, chatDetailList);
		this.context = context;
		this.chatDetailList = chatDetailList;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View rowView = convertView;
		
		if (rowView == null) {
			LayoutInflater layoutInflater = context.getLayoutInflater();
			rowView = layoutInflater.inflate(R.layout.layout_row_conversation, null);
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
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
		ChatDetail chatDetail = chatDetailList.get(position);
		
		// initialize both layout to visible state, why?
		// because if not, the layout state (either left or right) can be in gone state, and
		// the view won't be visible even though the view is set with value
		holder.leftLayout.setVisibility(View.VISIBLE);
		holder.rightLayout.setVisibility(View.VISIBLE);
		
		boolean isFromMe = chatDetail.isFromMe();
		
		if (isFromMe) {
			
			// Set the textview on the left with the message
			String message = chatDetail.getMessage();
			holder.rightMessage.setText(message);
			
			long millis = chatDetail.getTimestamp();
			String time = toDateString(millis);
			holder.rightTime.setText(time);
			
			holder.rightLayout.setBackgroundResource(R.drawable.baloon_self);
			
			// Set the state image based on the message state (sent, delivered, read)
			int state = chatDetail.getState();
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
			
			String message = chatDetail.getMessage();
			holder.leftMessage.setText(message);
			
			long millis = chatDetail.getTimestamp();
			String time = toDateString(millis);
			holder.leftTime.setText(time);
			
			holder.leftLayout.setBackgroundResource(R.drawable.baloon_remote);
			
			holder.rightLayout.setVisibility(View.GONE);
		}
		
		return rowView;
	}
	
	private String toDateString (long millis) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		Date date = new Date(millis);
		return sdf.format(date);
	}

}
