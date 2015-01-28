package co.rumpy.client.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import co.rumpy.client.android.utils.Constants;
import co.rumpy.stanza.iq.Roster;
import co.rumpy.stanza.presence.Presence;

import com.actionbarsherlock.app.SherlockFragment;

public class AddContactResultFragment extends SherlockFragment {
	
	ImageView thumbImage;
	TextView fullnameTextView;
	TextView signumTextView;
	TextView presenceTextView;
	Roster roster;
	Button addContactButton;
	SharedPreferences sp;
	
	String signum;
	String mySignum;
	String myBareSignum;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.addcontact_fragment_contactresult, container, false);
		thumbImage = (ImageView) view.findViewById(R.id.addcontact_fragment_imagethumb);
		fullnameTextView = (TextView) view.findViewById(R.id.addcontact_fragment_textfullname);
		signumTextView = (TextView) view.findViewById(R.id.addcontact_fragment_textsignum);
		presenceTextView = (TextView) view.findViewById(R.id.addcontact_fragment_textpresence);
		
		addContactButton = (Button) view.findViewById(R.id.addcontact_fragment_button_add);
		addContactButton.setOnClickListener(listener);
		
		signum = roster.getSignum();
		String fullname = roster.getFullname();
		String imageThumb = roster.getImage();
		String presence = roster.getPresence();
		
		if (imageThumb.equals("bagas.jpg")) {
			thumbImage.setImageResource(R.drawable.bagas); // this is wrong, should get from internal storage
		} else if (imageThumb.equals("diska.jpg")){
			thumbImage.setImageResource(R.drawable.diska);
		} else {
			thumbImage.setImageResource(R.drawable.ic_launcher);
		}
		
		fullnameTextView.setText(fullname);
		signumTextView.setText(signum);
		presenceTextView.setText(presence);
		
		sp = getSherlockActivity().getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
		mySignum = sp.getString("signum", null);
		myBareSignum = sp.getString(Constants.PREF_TAG_BARESIGNUM, null);
		
		return view;
		
	}
	
	@Override
	public void setArguments(Bundle args) {
		
		String signum = args.getString("signum");
		String fullname = args.getString("fullname");
		String image = args.getString("image");
		String presence = args.getString("presence");
		
		Roster roster = new Roster(signum, fullname, image, presence);
		this.roster = roster;
	}
	
	private OnClickListener listener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Presence subscribe = new Presence();
			subscribe.setTo(signum);
			subscribe.setFrom(myBareSignum);
			subscribe.setType(Presence.TYPE_SUBSCRIBE);
			
			//RumpyService.sendStanza(subscribe);
			RumpyService.sendStanza(subscribe, getSherlockActivity());
		}
	};
	

}
