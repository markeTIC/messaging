package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.odoo.addons.message.models.MailMessage;
import com.odoo.addons.message.providers.message.MessageProvider;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.support.listview.OListAdapter;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Message extends BaseFragment implements OnPullListener {
	public static final String TAG = Message.class.getSimpleName();

	enum Keys {
		INBOX, TOME, TODO, ARCHIVE, GROUP, OUTBOX
	}

	View mView = null;
	ListView mListView = null;
	OListAdapter mListViewAdapter = null;
	List<Object> mMessageObjects = new ArrayList<Object>();
	OETouchListener mTouchAttacher = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.message_layout, container, false);
		scope = new AppScope(this);
		init();
		return mView;
	}

	public void init() {
		mListView = (ListView) mView.findViewById(R.id.lstMeesages);

		mListViewAdapter = new OListAdapter(getActivity(),
				R.layout.fragment_message_listview_items, mMessageObjects) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View mView = convertView;
				if (mView == null)
					mView = getActivity().getLayoutInflater().inflate(
							getResource(), parent, false);
				// mView = handleRowView(mView, position);
				return mView;
			}
		};
		mMessageObjects.add("Nilesh");
		mMessageObjects.add("Shailesh");
		mMessageObjects.add("Dimple");
		mListView.setAdapter(mListViewAdapter);
		mTouchAttacher = scope.main().getTouchAttacher();
		mTouchAttacher.setPullableView(mListView, this);

	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Message", true));
		menu.add(new DrawerItem(TAG, "Inbox", count(context, Keys.INBOX), 0,
				object(Keys.INBOX)));
		menu.add(new DrawerItem(TAG, "To:me", count(context, Keys.TOME), 0,
				object(Keys.TOME)));
		menu.add(new DrawerItem(TAG, "To-do", count(context, Keys.TODO), 0,
				object(Keys.TODO)));
		menu.add(new DrawerItem(TAG, "Archives", count(context, Keys.ARCHIVE),
				0, object(Keys.ARCHIVE)));
		menu.add(new DrawerItem(TAG, "Groups", count(context, Keys.GROUP), 0,
				object(Keys.GROUP)));
		menu.add(new DrawerItem(TAG, "Outbox", count(context, Keys.OUTBOX), 0,
				object(Keys.OUTBOX)));
		return menu;

	}

	private Fragment object(Keys value) {
		Message msg = new Message();
		Bundle args = new Bundle();
		args.putString("message", value.toString());
		msg.setArguments(args);
		return msg;
	}

	private int count(Context context, Keys key) {
		int count = 0;
		return count;
	}

	public void onCreateOptionsMenu(android.view.Menu menu,
			android.view.MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_message, menu);
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_message_create:
			startFragment(new MessageDetail(), true);
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPullStarted(View arg0) {
		scope.main().requestSync(MessageProvider.AUTHORITY);
	}

	@Override
	public void onResume() {
		super.onResume();
		scope.context().registerReceiver(messageSyncFinish,
				new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
	}

	@Override
	public void onPause() {
		super.onPause();
		scope.context().unregisterReceiver(messageSyncFinish);
	}

	SyncFinishReceiver messageSyncFinish = new SyncFinishReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mTouchAttacher.setPullComplete();
			scope.main().refreshDrawer(TAG);
			mListViewAdapter.clear();
			mMessageObjects.clear();
			mListViewAdapter.notifiyDataChange(mMessageObjects);
		}
	};
}
