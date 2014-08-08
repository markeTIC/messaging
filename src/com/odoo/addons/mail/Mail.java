package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnListBottomReachedListener;
import odoo.controls.OList.OnListRowViewClickListener;
import odoo.controls.OList.OnRowClickListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.models.MailNotification;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Mail extends BaseFragment implements OnPullListener,
		BeforeListRowCreateListener, OnListRowViewClickListener,
		OnRowClickListener, OnListBottomReachedListener {

	public static final String TAG = Mail.class.getSimpleName();
	public static final String KEY = "fragment_mail";
	public static final String KEY_MESSAGE_ID = "mail_id";
	public static final Integer REQUEST_COMPOSE_MAIL = 234;
	private View mView = null;
	private OList mListControl = null;
	private List<ODataRow> mListRecords = new ArrayList<ODataRow>();
	private MessagesLoader mMessageLoader = null;
	private Boolean mSynced = false;
	private OETouchListener mTouchListener = null;
	private Integer mLastSelectPosition = -1;
	private Integer mLimit = 20;
	private MailMessage db = null;

	public enum Type {
		Inbox, ToMe, ToDo, Archives, Outbox, Group
	}

	private Type mType = Type.Inbox;
	private int[] background_resources = new int[] {
			R.drawable.message_listview_bg_toread_selector,
			R.drawable.message_listview_bg_tonotread_selector };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		scope = new AppScope(this);
		mTouchListener = scope.main().getTouchAttacher();
		initType();
		return inflater.inflate(R.layout.mail, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init();
	}

	private void initType() {
		Bundle bundle = getArguments();
		if (bundle.containsKey(KEY)) {
			mType = Type.valueOf(bundle.getString(KEY));
		}
	}

	private void init() {
		mListControl = (OList) mView.findViewById(R.id.lstMeesages);
		mTouchListener.setPullableView(mListControl, this);
		mListControl
				.setOnListRowViewClickListener(R.id.img_starred_mlist, this);
		mListControl.setOnListBottomReachedListener(this);
		mListControl.setBeforeListRowCreateListener(this);
		mListControl.setOnRowClickListener(this);
		mListControl.setRecordLimit(mListRecords.size());
		if (mLastSelectPosition == -1) {
			mMessageLoader = new MessagesLoader(mType, 0);
			mMessageLoader.execute();
		} else {
			showData(false);
		}

	}

	private void showData(Boolean mSyncing) {
		if (!mSyncing)
			mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
		mMessageLoader = null;
		mListControl.initListControl(mListRecords);
	}

	private HashMap<String, Object> getWhere(Context context, Type type) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		String where = null;
		String[] whereArgs = null;
		switch (type) {
		case Inbox:
			where = "to_read = ? AND starred = ? AND id != ?";
			whereArgs = new String[] { "true", "false", "0" };
			if (mListControl != null) {
				mListControl.setEmptyListIcon(R.drawable.ic_action_inbox);
				mListControl.setEmptyListMessage(context.getResources()
						.getString(R.string.message_inbox_all_read));
			}
			break;
		case ToMe:
			where = "to_read = ? AND starred = ? ";
			whereArgs = new String[] { "true", "false" };
			if (mListControl != null) {
				mListControl.setEmptyListIcon(R.drawable.ic_action_user);
				mListControl.setEmptyListMessage(context.getResources()
						.getString(R.string.message_tome_all_read));
			}
			break;
		case ToDo:
			where = "to_read = ? AND starred = ?";
			whereArgs = new String[] { "true", "true" };
			if (mListControl != null) {
				mListControl.setEmptyListIcon(R.drawable.ic_action_clipboard);
				mListControl.setEmptyListMessage(context.getResources()
						.getString(R.string.message_todo_all_read));
			}
			break;
		case Outbox:
			where = "id = ?";
			whereArgs = new String[] { "0" };
			if (mListControl != null) {
				mListControl.setEmptyListIcon(R.drawable.ic_action_unsent_mail);
				mListControl.setEmptyListMessage(context.getResources()
						.getString(R.string.message_no_outbox_message));
			}
			break;
		case Group:
			Integer group_id = getArguments().getInt(Groups.KEY);
			where = "res_id = ? and model = ?";
			whereArgs = new String[] { group_id + "", "mail.group" };
			if (mListControl != null) {
				mListControl
						.setEmptyListIcon(R.drawable.ic_action_social_group);
				mListControl.setEmptyListMessage(context.getResources()
						.getString(R.string.message_no_group_message));
			}
			break;
		case Archives:
			where = "id != ?";
			whereArgs = new String[] { "0" };
			break;
		default:
			break;
		}
		map.put("where", where);
		map.put("whereArgs", whereArgs);
		return map;
	}

	public class MessagesLoader extends AsyncTask<Void, Void, Boolean> {

		Type messageType = null;
		Boolean mSyncing = false;
		Integer mOffset = 0;

		public MessagesLoader(Type type, Integer offset) {
			messageType = type;
			mOffset = offset;
			if (mOffset == 0)
				mView.findViewById(R.id.loadingProgress).setVisibility(
						View.VISIBLE);
			else
				mView.findViewById(R.id.loadingProgress).setVisibility(
						View.GONE);
			if (db().isEmptyTable() && !mSynced) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				mSyncing = true;
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				Thread.sleep(500);
			} catch (Exception e) {

			}
			scope.main().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (mOffset == 0)
						mListRecords.clear();
					LinkedHashMap<String, ODataRow> mParentList = new LinkedHashMap<String, ODataRow>();
					HashMap<String, Object> map = getWhere(getActivity(),
							messageType);
					String where = (String) map.get("where");
					String whereArgs[] = (String[]) map.get("whereArgs");
					OModel model = db();
					model.setLimit(mLimit).setOffset(mOffset);
					for (ODataRow row : model.select(where, whereArgs, null,
							null, "date DESC")) {
						ODataRow parent = row.getM2ORecord("parent_id").browse(
								model);
						if (parent != null) {
							// Child
							if (!mParentList.containsKey("key_"
									+ parent.getString(OColumn.ROW_ID))) {
								parent.put("body", row.getString("body"));
								parent.put("date", row.getString("date"));
								parent.put("to_read", row.getBoolean("to_read"));
								mParentList.put(
										"key_"
												+ parent.getString(OColumn.ROW_ID),
										parent);

							}
						} else { // parent
							if (!mParentList.containsKey("key_"
									+ row.getString(OColumn.ROW_ID))) {
								mParentList.put(
										"key_" + row.getString(OColumn.ROW_ID),
										row);
							}
						}
					}
					for (String k : mParentList.keySet()) {
						mListRecords.add(mParentList.get(k));
					}
					mListControl.setRecordOffset(model.getNextOffset());
				}
			});
			return true;

		}

		@Override
		protected void onPostExecute(Boolean success) {
			showData(mSyncing);
		}

	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Messaging", true));
		menu.add(new DrawerItem(TAG, "Inbox", count_total(context, Type.Inbox),
				R.drawable.ic_action_inbox, object(Type.Inbox)));
		menu.add(new DrawerItem(TAG, "To: me", count_total(context, Type.ToMe),
				R.drawable.ic_action_user, object(Type.ToMe)));
		menu.add(new DrawerItem(TAG, "To-do", count_total(context, Type.ToDo),
				R.drawable.ic_action_clipboard, object(Type.ToDo)));
		menu.add(new DrawerItem(TAG, "Archives", 0,
				R.drawable.ic_action_briefcase, object(Type.Archives)));
		menu.add(new DrawerItem(TAG, "Outbox",
				count_total(context, Type.Outbox),
				R.drawable.ic_action_unsent_mail, object(Type.Outbox)));
		return menu;
	}

	private int count_total(Context context, Type key) {
		if (db == null)
			db = new MailMessage(context);
		int total_count = 0;
		HashMap<String, Object> map = getWhere(context, key);
		String where = (String) map.get("where");
		String whereArgs[] = (String[]) map.get("whereArgs");
		total_count = db.count(where, whereArgs);
		return total_count;
	}

	private Fragment object(Type type) {
		Mail mail = new Mail();
		Bundle bundle = new Bundle();
		bundle.putString(KEY, type.toString());
		mail.setArguments(bundle);
		return mail;
	}

	@Override
	public void onResume() {
		super.onResume();
		scope.main().registerReceiver(mSyncFinishReceiver,
				new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
	}

	@Override
	public void onPause() {
		super.onPause();
		scope.main().unregisterReceiver(mSyncFinishReceiver);
	}

	SyncFinishReceiver mSyncFinishReceiver = new SyncFinishReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			mTouchListener.setPullComplete();
			scope.main().refreshDrawer(TAG);
			mListRecords.clear();
			mListControl.setRecordLimit(mListRecords.size());
			if (mMessageLoader != null)
				mMessageLoader.cancel(true);
			mMessageLoader = new MessagesLoader(mType, 0);
			mMessageLoader.execute();
		}
	};

	@Override
	public void onPullStarted(View arg) {
		if (inNetwork()) {
			scope.main().requestSync(MailProvider.AUTHORITY);
		} else {
			mTouchListener.setPullComplete();
		}
	}

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		ImageView imgStarred = (ImageView) view
				.findViewById(R.id.img_starred_mlist);
		boolean is_fav = row.getBoolean("starred");
		imgStarred.setColorFilter((is_fav) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));

		// Check for to_read selector
		boolean to_read = row.getBoolean("to_read");
		view.setBackgroundResource(background_resources[(to_read) ? 1 : 0]);
	}

	@Override
	public void onRowViewClick(ViewGroup view_group, View view, int position,
			ODataRow row) {
		if (view.getId() == R.id.img_starred_mlist) {
			if (inNetwork()) {
				boolean starred = new MailNotification(getActivity())
						.getStarred(row.getInt(OColumn.ROW_ID));
				ImageView imgStarred = (ImageView) view;
				imgStarred.setColorFilter((!starred) ? Color
						.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
				new MarkAsTodo(getActivity(), row, !starred).execute();
			} else {
				Toast.makeText(getActivity(), "No Internet Connection",
						Toast.LENGTH_SHORT).show();
			}

		}
	}

	public static class MarkAsTodo extends AsyncTask<Void, Void, Boolean> {

		private ODataRow mRecord = null;
		private Boolean mTodoState = false;
		private Context mContext = null;

		public MarkAsTodo(Context context, ODataRow record, Boolean todo_state) {
			mContext = context;
			mRecord = record;
			mTodoState = todo_state;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			MailMessage mail = new MailMessage(mContext);
			return mail.markAsTodo(mRecord, mTodoState);
		}

	}

	@Override
	public void onRowItemClick(int position, View view, ODataRow row) {
		mLastSelectPosition = position;
		MailDetail mDetail = new MailDetail();
		Bundle bundle = new Bundle();
		bundle.putAll(row.getPrimaryBundleData());
		mDetail.setArguments(bundle);
		startFragment(mDetail, true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_mail, menu);
		SearchView mSearchView = (SearchView) menu.findItem(
				R.id.menu_mail_search).getActionView();
		if (mListControl != null)
			mSearchView.setOnQueryTextListener(mListControl.getQueryListener());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_mail_create:
			Intent i = new Intent(getActivity(), ComposeMail.class);
			startActivityForResult(i, REQUEST_COMPOSE_MAIL);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_COMPOSE_MAIL
				&& resultCode == Activity.RESULT_OK) {
			if (inNetwork()) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				Toast.makeText(getActivity(), "Message Sent", Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(getActivity(), "Message can't send",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onBottomReached(Integer record_limit, Integer record_offset) {
		if (mMessageLoader != null)
			mMessageLoader.cancel(true);
		mMessageLoader = new MessagesLoader(mType, record_offset);
		mMessageLoader.execute();
	}

	@Override
	public Boolean showLoader() {
		return true;
	}
}
