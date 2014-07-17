package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import odoo.controls.OList;
import odoo.controls.OList.OnRowClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.odoo.addons.message.models.MailMessage;
import com.odoo.addons.message.providers.message.MessageProvider;
import com.odoo.orm.ODataRow;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.support.listview.OListAdapter;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Message extends BaseFragment implements OnPullListener,
		OnRowClickListener {
	public static final String TAG = Message.class.getSimpleName();

	enum MType {
		inbox, tome, todo, archives, group, outbox
	}

	OList mListControl = null;
	List<ODataRow> mListRecords = new ArrayList<ODataRow>();

	Integer mRecentSwiped = -1;
	Integer mGroupId = null;
	Integer mSelectedItemPosition = -1;
	Integer selectedCounter = 0;
	String mCurrentType = "inbox";
	View mView = null;
	ListView mListView = null;
	OListAdapter mListViewAdapter = null;
	List<Object> mMessageObjects = new ArrayList<Object>();
	OETouchListener mTouchListener = null;
	MType mType = MType.inbox;

	MessagesLoader mMessageLoader = null;
	StarredOperation mStarredOperation = null;
	ReadUnreadOperation mReadUnreadOperation = null;

	HashMap<String, Integer> message_row_indexes = new HashMap<String, Integer>();
	HashMap<String, Integer> message_model_colors = new HashMap<String, Integer>();

	Integer tag_color_count = 0;
	Boolean isSynced = false;

	int[] background_resources = new int[] {
			R.drawable.message_listview_bg_toread_selector,
			R.drawable.message_listview_bg_tonotread_selector };

	int[] starred_drawables = new int[] { R.drawable.ic_action_starred,
			R.drawable.ic_action_unstarred };
	String tag_colors[] = new String[] { "#A4C400", "#00ABA9", "#1BA1E2",
			"#AA00FF", "#D80073", "#A20025", "#FA6800", "#6D8764", "#76608A",
			"#EBB035" };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		scope = new AppScope(this);
		mView = inflater.inflate(R.layout.message_layout, container, false);
		init();
		return mView;
	}

	public void init() {
		Log.d(TAG, "Message->init()");
		mListControl = (OList) mView.findViewById(R.id.lstMeesages);
		initData();
		mTouchListener = scope.main().getTouchAttacher();
		mTouchListener.setPullableView(mListControl, this);
		mListControl.setOnRowClickListener(this);

	}

	private void initData() {
		Log.d(TAG, "Message->initData()");
		if (mSelectedItemPosition > -1) {
			return;
		}
		Bundle bundle = getArguments();
		if (bundle != null) {
			if (bundle.containsKey("type")) {
				mCurrentType = bundle.getString("type");
				Log.e("inside ", mCurrentType);
				String title = "archives";
				if (mCurrentType.equals("inbox")) {
					Log.e("Loader", "inbox");
					mMessageLoader = new MessagesLoader(MType.inbox);
					mMessageLoader.execute((Void) null);
					title = "inbox";
				} else if (mCurrentType.equals("to-me")) {
					Log.e("Loader", "tome");
					title = "To-Me";
					mMessageLoader = new MessagesLoader(MType.tome);
					mMessageLoader.execute((Void) null);
				} else if (mCurrentType.equals("to-do")) {
					Log.e("Loader", "todo");
					title = "To-DO";
					mMessageLoader = new MessagesLoader(MType.todo);
					mMessageLoader.execute((Void) null);
				} else if (mCurrentType.equals("archives")) {
					Log.e("Loader", "archive");
					mMessageLoader = new MessagesLoader(MType.archives);
					mMessageLoader.execute((Void) null);
				}
				scope.main().setTitle(title);
			} else {
				if (bundle.containsKey("group_id")) {
					Log.e("Loader", "Group Id");
					mGroupId = bundle.getInt("group_id");
					mMessageLoader = new MessagesLoader(MType.group);
					mMessageLoader.execute((Void) null);
				} else {
					Log.e("Loader", "inbox else");
					scope.main().setTitle("inbox");
					mMessageLoader = new MessagesLoader(MType.inbox);
					mMessageLoader.execute((Void) null);
				}

			}
		}

	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Message", true));
		menu.add(new DrawerItem(TAG, "Inbox", count(context, MType.inbox),
				R.drawable.ic_action_inbox, getFragment(MType.inbox)));
		menu.add(new DrawerItem(TAG, "To:me", count(context, MType.tome),
				R.drawable.ic_action_user, getFragment(MType.tome)));
		menu.add(new DrawerItem(TAG, "To-do", count(context, MType.todo),
				R.drawable.ic_action_todo, getFragment(MType.todo)));
		menu.add(new DrawerItem(TAG, "Archives",
				count(context, MType.archives), 0, getFragment(MType.archives)));
		menu.add(new DrawerItem(TAG, "Outbox", count(context, MType.outbox),
				R.drawable.ic_action_archive, getFragment(MType.outbox)));
		return menu;

	}

	private Fragment getFragment(MType value) {
		Message msg = new Message();
		Bundle args = new Bundle();
		args.putString("type", value.toString());
		msg.setArguments(args);
		return msg;
	}

	private int count(Context context, MType key) {
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
			getActivity().startActivity(
					new Intent(getActivity(), MessageComposeActivity.class));
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

	private SyncFinishReceiver messageSyncFinish = new SyncFinishReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.e("Pullable", "complete");
			mTouchListener.setPullComplete();
			scope.main().refreshDrawer(TAG);
			mListViewAdapter.clear();
			mMessageObjects.clear();
			// mListViewAdapter.notifiyDataChange(mMessageObjects);
		}
	};

	public HashMap<String, Object> getWhere(MType type) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		String where = null;
		String[] whereArgs = null;
		switch (type) {
		case inbox:
			where = "to_read = ? AND is_favorite = ?";
			whereArgs = new String[] { "true", "false" };
			break;
		case tome:
			where = "res_id = ? AND to_read = ?";
			whereArgs = new String[] { "0", "true" };
			break;
		case todo:
			where = "to_read = ? AND is_favorite = ?";
			whereArgs = new String[] { "true", "true" };
			break;
		case outbox:
			where = "res_id = ? AND model = ?";
			whereArgs = new String[] { mGroupId + "", "mail.group" };
			break;
		default:
			where = null;
			whereArgs = null;
			break;
		}
		map.put("where", where);
		map.put("whereArgs", whereArgs);
		return map;
	}

	public class MessagesLoader extends AsyncTask<Void, Void, Boolean> {

		MType messageType = null;

		public MessagesLoader(MType type) {
			messageType = type;
			mView.findViewById(R.id.loadingProgress)
					.setVisibility(View.VISIBLE);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			scope.main().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (db().isEmptyTable()) {
						scope.main().requestSync(MessageProvider.AUTHORITY);
					}
					mListRecords.clear();
					mMessageObjects.clear();
					HashMap<String, Object> map = getWhere(messageType);
					String where = (String) map.get("where");
					String whereArgs[] = (String[]) map.get("whereArgs");
					mType = messageType;
					switch (mType) {
					case inbox:
						mListRecords.addAll(db().select(where, whereArgs));
						break;
					case todo:
						mListRecords.addAll(db().select(where, whereArgs));
						break;
					case tome:
						mListRecords.addAll(db().select(where, whereArgs));
						break;
					case archives:
						mListRecords.addAll(db().select(where, whereArgs));
						break;
					case outbox:
						mListRecords.addAll(db().select(where, whereArgs));
						break;
					default:
						break;
					}
				}
			});
			return true;

		}

		@Override
		protected void onPostExecute(Boolean success) {
			mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
			mMessageLoader = null;
			mListControl.initListControl(mListRecords);
		}

	}

	public class StarredOperation extends AsyncTask<Void, Void, Boolean> {
		boolean mStarred = false;

		public StarredOperation(Boolean starred) {
			mStarred = starred;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			return null;
		}

	}

	public class ReadUnreadOperation extends AsyncTask<Void, Void, Boolean> {
		boolean mToRead = false;

		public ReadUnreadOperation(Boolean to_read) {
			mToRead = to_read;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			return null;
		}

	}

	@Override
	public void onRowItemClick(int position, View view, ODataRow row) {
		MessageDetail mDetail = new MessageDetail();
		Bundle bundle = new Bundle();
		bundle.putString("key", mCurrentType.toString());
		bundle.putAll(row.getPrimaryBundleData());
		mDetail.setArguments(bundle);
		startFragment(mDetail, true);
	}
}
