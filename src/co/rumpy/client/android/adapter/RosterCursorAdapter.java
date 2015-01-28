package co.rumpy.client.android.adapter;

import co.rumpy.client.android.R;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class RosterCursorAdapter extends CursorAdapter {
	
	private final Activity context;
	
	public RosterCursorAdapter(Activity context, Cursor c, int flags) {
		super(context, c, flags);
		this.context = context;
	}
	
	static class ViewHolder {
		
		public ImageView thumbImage;
		public TextView fullname;
		public TextView lastPresence;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		
		ViewHolder holder = (ViewHolder) view.getTag();
		
		String thumbImage = cursor.getString(3);
		if (thumbImage.equals("bagas.jpg")) {
			holder.thumbImage.setImageResource(R.drawable.bagas); // this is wrong, should get from internal storage
		} else if (thumbImage.equals("diska.jpg")){
			holder.thumbImage.setImageResource(R.drawable.diska);
		} else {
			holder.thumbImage.setImageResource(R.drawable.thumb);
		}
		
		// initiate contact fullname
		String fullname = cursor.getString(2);
		holder.fullname.setText(fullname);
		
		// initiate contact's last presence (status update)
		String presence = cursor.getString(4);
		holder.lastPresence.setText(presence);
		
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup group) {
		
		View rowView = LayoutInflater.from(context).inflate(R.layout.layout_row_contactlist, group, false);
		
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.thumbImage = (ImageView) rowView.findViewById(R.id.contactThumb);
		viewHolder.fullname = (TextView) rowView.findViewById(R.id.contactFullname);
		viewHolder.lastPresence = (TextView) rowView.findViewById(R.id.contactLastPresence);
		
		rowView.setTag(viewHolder);
		
		return rowView;
	}

}
