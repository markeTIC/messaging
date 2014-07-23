package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnListRowViewClickListener;
import odoo.controls.OList.OnRowClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import com.odoo.addons.message.models.MailMessage;
import com.odoo.addons.message.providers.message.MessageProvider;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Message extends BaseFragment implements OnPullListener,
		OnRowClickListener, OnListRowViewClickListener,
		BeforeListRowCreateListener {
	public static final String TAG = Message.class.getSimpleName();

	enum MType {
		inbox, tome, todo, archives, group, outbox
	}

	OList mListControl = null;
	List<ODataRow> mListRecords = new ArrayList<ODataRow>();

	Integer mGroupId = null;
	Integer selectedCounter = 0;
	String mCurrentType = "inbox";
	SearchView mSerachView = null;
	View mView = null;
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
		mListControl
				.setOnListRowViewClickListener(R.id.img_starred_mlist, this);
		initData();
		mTouchListener = scope.main().getTouchAttacher();
		mTouchListener.setPullableView(mListControl, this);
		mListControl.setOnRowClickListener(this);
		mListControl.setBeforeListRowCreateListener(this);

	}

	private void initData() {
		Log.d(TAG, "Message->initData()");
		Bundle bundle = getArguments();
		if (bundle != null) {
			if (bundle.containsKey("type")) {
				mCurrentType = bundle.getString("type");
				String title = "Archives";
				if (mCurrentType.equals(MType.inbox.toString())) {
					mMessageLoader = new MessagesLoader(MType.inbox);
					mMessageLoader.execute((Void) null);
					title = "Inbox";
				} else if (mCurrentType.equals(MType.tome.toString())) {
					title = "To-me";
					mMessageLoader = new MessagesLoader(MType.tome);
					mMessageLoader.execute((Void) null);
				} else if (mCurrentType.equals(MType.todo.toString())) {
					title = "To-do";
					mMessageLoader = new MessagesLoader(MType.todo);
					mMessageLoader.execute((Void) null);
				} else if (mCurrentType.equals(MType.archives.toString())) {
					mMessageLoader = new MessagesLoader(MType.archives);
					mMessageLoader.execute((Void) null);
				}
				scope.main().setTitle(title);
			} else {
				if (bundle.containsKey("group_id")) {
					mGroupId = bundle.getInt("group_id");
					mMessageLoader = new MessagesLoader(MType.group);
					mMessageLoader.execute((Void) null);
				} else {
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
		menu.add(new DrawerItem(TAG, "To-me", count(context, MType.tome),
				R.drawable.ic_action_user, getFragment(MType.tome)));
		menu.add(new DrawerItem(TAG, "To-do", count(context, MType.todo),
				R.drawable.ic_action_todo, getFragment(MType.todo)));
		menu.add(new DrawerItem(TAG, "Archives",
				count(context, MType.archives), R.drawable.ic_action_archive,
				getFragment(MType.archives)));
		menu.add(new DrawerItem(TAG, "Outbox", count(context, MType.outbox),
				R.drawable.ic_action_outbox, getFragment(MType.outbox)));
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
		switch (key) {
		case inbox:
			count = new MailMessage(context).count(
					"to_read = ? AND is_favorite = ?", new Object[] { "true",
							"false" });
			break;
		case tome:
			count = new MailMessage(context).count("to_read = ?",
					new Object[] { "true" });
			break;
		case todo:
			count = new MailMessage(context).count(
					"to_read = ? AND is_favorite = ?", new Object[] { "true",
							"true" });
			break;
		case outbox:
			count = new MailMessage(context).count("id = ?",
					new Object[] { "false" });
			break;

		default:
			break;
		}
		return count;
	}

	public void onCreateOptionsMenu(android.view.Menu menu,
			android.view.MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_message, menu);
		// mSerachView = (SearchView) menu.findItem(R.id.menu_message_search);
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
			mListRecords.clear();
			if (mMessageLoader != null)
				mMessageLoader.cancel(true);
			mMessageLoader = new MessagesLoader(mType);
			mMessageLoader.execute();
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
			where = "to_read = ?";
			whereArgs = new String[] { "true" };
			break;
		case todo:
			where = "to_read = ? AND is_favorite = ?";
			whereArgs = new String[] { "true", "true" };
			break;
		case group:
			where = "res_id = ? AND model = ?";
			whereArgs = new String[] { mGroupId + "", "mail.group" };
			break;
		case outbox:
			where = "id = ?";
			whereArgs = new String[] { "false" };
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
			mType = type;
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
					LinkedHashMap<String, ODataRow> mParentList = new LinkedHashMap<String, ODataRow>();
					HashMap<String, Object> map = getWhere(messageType);
					String where = (String) map.get("where");
					String whereArgs[] = (String[]) map.get("whereArgs");
					for (ODataRow row : db().select(where, whereArgs, null,
							null, "date DESC")) {
						ODataRow parent = row.getM2ORecord("parent_id")
								.browse();

						if (parent != null) {
							// Child
							if (!mParentList.containsKey("key_"
									+ parent.getString("id"))) {
								parent.put("body", row.getString("body"));
								parent.put("date", row.getString("date"));
								parent.put("to_read", row.getBoolean("to_read"));
								mParentList.put(
										"key_" + parent.getString("id"), parent);

							}
						} else { // parent
							if (!mParentList.containsKey("key_"
									+ row.getString("id"))) {
								mParentList.put("key_" + row.getString("id"),
										row);
							}
						}
					}
					for (String k : mParentList.keySet()) {
						mListRecords.add(mParentList.get(k));
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
		String title = "false";
		MessageDetail mDetail = new MessageDetail();
		Bundle bundle = new Bundle();
		bundle.putString("key", mCurrentType.toString());
		bundle.putAll(row.getPrimaryBundleData());
		if (!row.getString("record_name").equals("false"))
			title = row.getString("record_name");
		if (title.equals("false") && !row.getString("subject").equals("false"))
			title = row.getString("subject");
		if (title.equals("false"))
			title = "comment";
		OLog.log("title = " + title);
		bundle.putString("subject", title);
		mDetail.setArguments(bundle);
		startFragment(mDetail, true);
	}

	@Override
	public void onRowViewClick(ViewGroup view_group, View view, int position,
			ODataRow row) {
		MailMessage mail = new MailMessage(getActivity());
		if (view.getId() == R.id.img_starred_mlist) {
			ImageView imgStarred = (ImageView) view;
			boolean is_fav = row.getBoolean("is_favorite");
			imgStarred.setColorFilter((!is_fav) ? Color.parseColor("#FF8800")
					: Color.parseColor("#aaaaaa"));
			OValues values = new OValues();
			values.put("is_favorite", !is_fav);
			mail.update(values, row.getInt("id"));
			row.put("is_favorite", !is_fav);
			mListRecords.remove(position);
			mListRecords.add(position, row);
		}

	}

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		ImageView imgStarred = (ImageView) view
				.findViewById(R.id.img_starred_mlist);
		boolean is_fav = row.getBoolean("is_favorite");
		imgStarred.setColorFilter((is_fav) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));

		// Check for to_read selector
		boolean to_read = row.getBoolean("to_read");
		view.setBackgroundResource(background_resources[(to_read) ? 1 : 0]);
	}
}
