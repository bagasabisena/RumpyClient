package co.rumpy.client.android;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import co.rumpy.client.android.adapter.ContactListArrayAdapter;
import co.rumpy.client.android.adapter.RosterCursorAdapter;
import co.rumpy.client.android.db.Database;
import co.rumpy.client.android.db.RosterProvider;
import co.rumpy.client.android.structure.ContactDetail;
import co.rumpy.client.android.utils.Constants;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ContactListFragment extends SherlockFragmentActivity {
	
	
	
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
	
	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem itemAdd = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "add contact");
		itemAdd.setIcon(R.drawable.add_person);
		itemAdd.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case Menu.FIRST:
			Toast.makeText(this, "Heeaayy", Toast.LENGTH_SHORT).show();
			break;

		default:
			Toast.makeText(this, "Hooey", Toast.LENGTH_SHORT).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}*/
	
	
	public static class ArrayListFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {
		
		private static final String TAG = ArrayListFragment.class.getSimpleName();
		ArrayList<ContactDetail> contacts;
		ContactListArrayAdapter adapter;
		Database db;
		ContentResolver resolver;
		RosterCursorAdapter cursorAdapter;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			
			super.onActivityCreated(savedInstanceState);
			
			getSherlockActivity().getSupportLoaderManager().initLoader(Constants.LOADER_ROSTER, null, this);
			resolver = getSherlockActivity().getContentResolver();
			cursorAdapter = new RosterCursorAdapter(getSherlockActivity(), null, 0);
			setListAdapter(cursorAdapter);
			
			//-------------------------------------------
			contacts = new ArrayList<ContactDetail>();
			
			setHasOptionsMenu(true);
			
			// Should get from database
			// right now still static
			//contacts.add(new ContactDetail("adiskaf@rumpy.co", "Adiska Fardani", "diska.jpg", "nolimitid.com - Social Monitoring"));
			//contacts.add(new ContactDetail("bagas@rumpy.co", "Bagas Abisena", "bagas.jpg", "Service Engineer IMS Integrator"));
			
			//db = Database.getInstance(getSherlockActivity());
			//ContactDetail contact = db.getContact("adiskaf@rumpy.co");
			//contacts.add(contact);
			//contacts = db.getAllContacts();
			
			//adapter = new ContactListArrayAdapter(getSherlockActivity(), contacts);
			//setListAdapter(adapter);
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			
			RosterCursorAdapter _adapter = (RosterCursorAdapter) l.getAdapter();
			Cursor c = _adapter.getCursor();
			c.moveToPosition(position);
			
			
			
			//ContactDetail contact = (ContactDetail) getListAdapter().getItem(position);
			//String signum = contact.getSignum();
			String signum = c.getString(1);
			//String fullname = contact.getFullname();
			String fullname = c.getString(2);
			//String image = contact.getImageThumbName();
			String image = c.getString(3);
			//String presence = contact.getLastPresence();
			String presence = c.getString(4);
			
			Intent i = new Intent(getActivity(), ConversationActivity.class);
			i.putExtra("signum", signum);
			i.putExtra("fullname", fullname);
			i.putExtra("presence", presence);
			i.putExtra("image", image);
			startActivity(i);
			
		}

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			CursorLoader loader = new CursorLoader(getSherlockActivity(), RosterProvider.CONTENT_URI, 
					null, null, null, null);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
			cursorAdapter.swapCursor(arg1);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			cursorAdapter.swapCursor(null);
		}
		
		/*@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			
			super.onCreateOptionsMenu(menu, inflater);
			
			MenuItem itemAdd = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "add contact");
			itemAdd.setIcon(R.drawable.add_person);
			itemAdd.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			
			Intent startActivityIntent = new Intent(getSherlockActivity(), AddContactActivity.class);
			//itemAdd.setIntent(startActivityIntent);
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			
			switch (item.getItemId()) {
			case Menu.FIRST:
				Toast.makeText(getSherlockActivity(), "Heeaayy", Toast.LENGTH_SHORT).show();
				break;

			default:
				Toast.makeText(getSherlockActivity(), "Hooey", Toast.LENGTH_SHORT).show();
				break;
			}
			return super.onOptionsItemSelected(item);
		}*/
		
		
	}

}
