package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OForm;

import org.json.JSONArray;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.odoo.addons.message.Message.MType;
import com.odoo.addons.message.models.MailMessage;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.drawer.DrawerListener;
import com.openerp.R;

public class MessageDetail extends BaseFragment {
	public static final String TAG = "com.odoo.addons.message.MessageDetail";

	private View mView = null;
	private MType mType = null;
	private Integer mId = null;
	private Boolean mLocalRecord = false;
	private OForm mForm = null;
	private ODataRow mRecord = null;
	private Menu mMenu = null;
	private OModel mModel = null;
	Integer mMessageId = null;
	ODataRow mMessageData = null;
	ReadUnreadOperation mReadUnreadOperation = null;
	List<Object> mMessageObjects = new ArrayList<Object>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		initArgs();
		setHasOptionsMenu(true);

		mView = inflater.inflate(
				R.layout.fragment_message_detail_listview_items, container,
				false);
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		init();
	}

	private void init() {
		Bundle bundle_data = getArguments();
		mMessageId = bundle_data.getInt("id");
		Log.e("mMessageId=", mMessageId + "");
		// mMessageData = db().select(mMessageId);

		switch (mType) {
		case inbox:
			OControls.setVisible(mView, R.id.odooFormMessagesDetail);
			mForm = (OForm) mView.findViewById(R.id.odooFormMessagesDetail);
			mModel = new MailMessage(getActivity());
			if (mId != null) {
				mRecord = mModel.select(mId, mLocalRecord);
				int count = mModel.count("parent_id = ?", new String[] { mId
						+ "" });

				Log.e("Record = ", mRecord + "" + count + " " + mId);
				mForm.initForm(mRecord);
			} else {
				mForm.setModel(mModel);

			}
			break;
		case todo:

			break;
		case tome:

			break;
		case archives:

			break;
		case outbox:

			break;

		default:
			break;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_message_detail, menu);
		mMenu = menu;

	}

	@Override
	public Object databaseHelper(Context context) {
		return null;
	}

	private void initArgs() {
		Bundle args = getArguments();
		Log.e("Args", args + "");
		mType = Message.MType.valueOf(args.getString("key"));
		if (args.containsKey("id")) {
			Log.e("inside", "id");
			mLocalRecord = args.getBoolean("local_record");
			if (mLocalRecord) {
				mId = args.getInt("local_id");
			} else {
				mId = args.getInt("id");
			}
		} else {
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_message_read:
			Toast.makeText(getActivity(), "Read", Toast.LENGTH_SHORT).show();

			break;
		case R.id.menu_message_unread:
			Toast.makeText(getActivity(), "Un Read", Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public class ReadUnreadOperation extends AsyncTask<Void, Void, Boolean> {

		ProgressDialog mProgressDialog = null;
		boolean mToRead = false;
		boolean isConnection = true;
		OSyncHelper mOE = null;

		public ReadUnreadOperation(boolean toRead) {
			mOE = db().getSyncHelper();
			if (mOE == null)
				isConnection = false;
			mToRead = toRead;
			mProgressDialog = new ProgressDialog(getActivity());
			mProgressDialog.setMessage("Working...");
			if (isConnection) {
				mProgressDialog.show();
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (!isConnection)
				return false;

			boolean flag = false;

			String default_model = "false";
			JSONArray ids = new JSONArray();
			int parent_id = 0, res_id = 0;

			parent_id = mMessageData.getInt("id");
			res_id = mMessageData.getInt("res_id");
			default_model = mMessageData.getString("model");

			ids.put(parent_id);
			for (ODataRow child : db().select("parent_id = ? ",
					new String[] { parent_id + "" })) {
				ids.put(child.getInt("id"));
			}
			// if (toggleReadUnread(mOE, ids, default_model, res_id, parent_id,
			// mToRead)) {
			// flag = true;
			// }

			return flag;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				DrawerListener drawer = (DrawerListener) getActivity();
				drawer.refreshDrawer(Message.TAG);
			} else {
				Toast.makeText(getActivity(), "No connection",
						Toast.LENGTH_LONG).show();
			}
			mProgressDialog.dismiss();
		}

	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

}
