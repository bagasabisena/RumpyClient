package co.rumpy.client.android;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainTabActivity extends SherlockFragmentActivity {
	
	TabHost tabHost;
	ViewPager viewPager;
	TabsAdapter tabsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_tab);
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayUseLogoEnabled(true);
		ab.setDisplayShowTitleEnabled(false);
		ab.setLogo(R.drawable.ic_logo);
		
		viewPager = (ViewPager) findViewById(R.id.pager);
		
		tabsAdapter = new TabsAdapter(this, tabHost, viewPager);
		
		TabSpec tab1 = tabHost.newTabSpec("chats");
		View tab1View = setTabView(R.drawable.gradient_red, "Chats");
		tab1.setIndicator(tab1View);
		
		TabSpec tab2 = tabHost.newTabSpec("contacts");
		View tab2View = setTabView(R.drawable.gradient_green, "Contacts");
		tab2.setIndicator(tab2View);
		
		TabSpec tab3 = tabHost.newTabSpec("presence");
		View tab3View = setTabView(R.drawable.gradient_yellow, "Presence");
		tab3.setIndicator(tab3View);
		
		TabSpec tab4 = tabHost.newTabSpec("channels");
		View tab4View = setTabView(R.drawable.gradient_orange, "Channels");
		tab4.setIndicator(tab4View);
		
		tabsAdapter.addTab(tab1, RecentChatFragment.ArrayListFragment.class, null);
		tabsAdapter.addTab(tab2, ContactListFragment.ArrayListFragment.class, null);
		//tabsAdapter.addTab(tab3, UnderConstructionFragment.class, null);
		//tabsAdapter.addTab(tab4, UnderConstructionFragment.class, null);
		
		
		
		//tabsAdapter.addTab(tabHost.newTabSpec("chats").setIndicator("Recent Chats"), TestFragment.ArrayListFragment.class, null);
		//tabsAdapter.addTab(tabHost.newTabSpec("contacts").setIndicator("Contacts"), TestFragment.ArrayListFragment.class, null);

		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int id = item.getItemId();
		switch (id) {
		case R.id.itemstart:
			Intent startServiceIntent = new Intent();
			startServiceIntent.setClass(MainTabActivity.this, RumpyService.class);
			startServiceIntent.setAction(RumpyService.SERVICE_START);
			startService(startServiceIntent);
			Toast.makeText(MainTabActivity.this, "Service started!!", Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.itemstop:
			Intent stopServiceIntent = new Intent();
			stopServiceIntent.setClass(MainTabActivity.this, RumpyService.class);
			stopServiceIntent.setAction(RumpyService.SERVICE_STOP);
			startService(stopServiceIntent);
			Toast.makeText(MainTabActivity.this, "Service stopped..", Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.item_add_contact:
			//Toast.makeText(MainTabActivity.this, "Add contact!", Toast.LENGTH_SHORT).show();
			Intent startActivityIntent = new Intent(MainTabActivity.this, AddContactActivity.class);
			startActivity(startActivityIntent);
			break;
		}
		
		return true;
	}
	
	public View setTabView(int drawableResId, String tabName) {
		
		View view = LayoutInflater.from(this).inflate(R.layout.layout_tab_main, null);
		view.setBackgroundDrawable(getResources().getDrawable(drawableResId));
		TextView tv = (TextView) view.findViewById(R.id.tab_text);
		tv.setText(tabName);
		
		return view;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		super.onSaveInstanceState(outState);
		outState.putString("tab", tabHost.getCurrentTabTag());
		
	}
	
	public static class TabsAdapter extends FragmentPagerAdapter implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

		private final Context mContext;
		private final TabHost mTabHost;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
		
		private static class TabInfo {
			
			private final String tag;
            private final Class<?> clss;
            private final Bundle args;
            
            TabInfo (String _tag, Class<?> _clss, Bundle _args) {
            	
            	tag = _tag;
            	clss = _clss;
            	args = _args;
            	
            }
		}
		
		static class DummyTabFactory implements TabHost.TabContentFactory {
			
			private final Context mContext;
			
			public DummyTabFactory(Context context) {
				mContext = context;
			}

			@Override
			public View createTabContent(String tag) {
				
				View v = new View(mContext);
				v.setMinimumHeight(0);
				v.setMinimumWidth(0);
				return v;
			}
			
		}
		
		public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
			
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mTabHost = tabHost;
			mViewPager = pager;
			mTabHost.setOnTabChangedListener(this);
			mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
			
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
			
			tabSpec.setContent(new DummyTabFactory(mContext));
			String tag = tabSpec.getTag();
			
			TabInfo info = new TabInfo(tag, clss, args);
			mTabs.add(info);
			mTabHost.addTab(tabSpec);
			notifyDataSetChanged();
		}
		@Override
		public void onPageScrollStateChanged(int arg0) {
			
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			
		}

		@Override
		public void onPageSelected(int position) {
			
			// Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
			
		}

		@Override
		public void onTabChanged(String tabId) {
			
			int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
			
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(), info.args);
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}
		
	}

}
