package co.rumpy.client.android;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import co.rumpy.client.android.ConversationActivity;
import co.rumpy.client.android.ContactListFragment.ArrayListFragment;
import co.rumpy.client.android.adapter.ChatListArrayAdapter;
import co.rumpy.client.android.adapter.ContactListArrayAdapter;
import co.rumpy.client.android.adapter.RecentChatCursorAdapter;
import co.rumpy.client.android.db.Database;
import co.rumpy.client.android.db.MessageProvider;
import co.rumpy.client.android.structure.ContactDetail;
import co.rumpy.client.android.structure.RecentChatDetail;
import co.rumpy.client.android.utils.Constants;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;


public class RecentChatFragment extends SherlockFragmentActivity {
	
	@Override
	protected void onCreate(Bundle arg0) {
		
		super.onCreate(arg0);
		
		FragmentManager fm = getSupportFragmentManager();
		
		// Check if the view is fragment or not
		// If not then target non-fragment view to become view
		// This is what we wanted
		if (fm.findFragmentById(android.R.id.content) == null) {
			ArrayListFragment list = new ArrayListFragment();
			fm.beginTransaction().add(android.R.id.content, list).commit();
		}
	}
	
	public static class ArrayListFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {
		
		ArrayList<RecentChatDetail> chats;
		ChatListArrayAdapter adapter;
		Database db;
		RecentChatCursorAdapter cAdapter;
		ContentResolver resolver;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			
			super.onActivityCreated(savedInstanceState);
			
			//chats = new ArrayList<RecentChatDetail>();
			
			setHasOptionsMenu(true);
			
			// Should get from database
			// right now still static
			//contacts.add(new ContactDetail("adiskaf@rumpy.co", "Adiska Fardani", "diska.jpg", "nolimitid.com - Social Monitoring"));
			//contacts.add(new ContactDetail("bagas@rumpy.co", "Bagas Abisena", "bagas.jpg", "Service Engineer IMS Integrator"));
			
			//db = Database.getInstance(getSherlockActivity());
			//ContactDetail contact = db.getContact("adiskaf@rumpy.co");
			//contacts.add(contact);
			//chats = db.getRecentChats();
			
			//adapter = new ChatListArrayAdapter(getSherlockActivity(), chats);
			//setListAdapter(adapter);
			
			getSherlockActivity().getSupportLoaderManager().initLoader(Constants.LOADER_RECENTCHAT, null, this);
			resolver = getSherlockActivity().getContentResolver();
			cAdapter = new RecentChatCursorAdapter(getSherlockActivity(), null, 0);
			setListAdapter(cAdapter);
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			
			/*RecentChatDetail rcd = (RecentChatDetail) getListAdapter().getItem(position);
			String signum = rcd.getSignum();
			String fullname = rcd.getFullname();
			String image = rcd.getImageThumbName();
			String lastChat = rcd.getLastChat();
			long timestamp = rcd.getTimestamp();
			String presence = rcd.getPresence();*/
			
			RecentChatCursorAdapter _adapter = (RecentChatCursorAdapter) l.getAdapter();
			Cursor c = _adapter.getCursor();
			c.moveToPosition(position);
			
			String signum = c.getString(0);
			String fullname = c.getString(1);
			String image = c.getString(2);
			String presence = c.getString(3);
			
			Intent i = new Intent(getSherlockActivity(), ConversationActivity.class);
			i.putExtra("signum", signum);
			i.putExtra("fullname", fullname);
			i.putExtra("presence", presence);
			i.putExtra("image", image);
			startActivity(i);
			
			TextView fullnameTextView = (TextView) v.findViewById(R.id.recent_textview_fullname);
			TextView messageTextView = (TextView) v.findViewById(R.id.recent_textview_lastchat);
			TextView timeTextView = (TextView) v.findViewById(R.id.recent_textview_time);
			
			fullnameTextView.setTypeface(null, Typeface.NORMAL);
			messageTextView.setTypeface(null, Typeface.NORMAL);
			timeTextView.setTypeface(null, Typeface.NORMAL);
			
			/*ContactDetail contact = (ContactDetail) getListAdapter().getItem(position);
			String signum = contact.getSignum();
			String fullname = contact.getFullname();
			String presence = contact.getLastPresence();
			String image = contact.getImageThumbName();
			
			Intent i = new Intent(getActivity(), ConversationActivity.class);
			i.putExtra("signum", signum);
			i.putExtra("fullname", fullname);
			i.putExtra("presence", presence);
			i.putExtra("image", image);
			startActivity(i);*/
			
		}

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			Uri uri = Uri.withAppendedPath(MessageProvider.CONTENT_URI, "recent");
			CursorLoader loader = new CursorLoader(getSherlockActivity(), uri, null, null, null, null);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			cAdapter.swapCursor(cursor);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			cAdapter.swapCursor(null);			
		}
	}

}
