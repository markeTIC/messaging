package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OForm.OnViewClickListener;
import odoo.controls.OList;
import odoo.controls.OList.OnListRowViewClickListener;

import org.json.JSONArray;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.addons.message.Message.MType;
import com.odoo.addons.message.models.MailMessage;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.drawer.DrawerListener;
import com.openerp.R;

public class MessageDetail extends BaseFragment implements OnViewClickListener,
		OnListRowViewClickListener {
	public static final String TAG = "com.odoo.addons.message.MessageDetail";

	private View mView = null;
	private MType mType = null;
	private Integer mId = null;
	private Boolean mLocalRecord = false;
	private OList mListMessages = null;
	private List<ODataRow> mRecord = null;
	// /private Menu mMenu = null;
	MailMessage mail = null;
	Integer mMessageId = null;
	ODataRow mMessageData = null;
	ReadUnreadOperation mReadUnreadOperation = null;
	List<Object> mMessageObjects = new ArrayList<Object>();
	ImageView btnStar;
	boolean isFavorite = false;

	Integer[] mStarredDrawables = new Integer[] { R.drawable.ic_action_starred,
			R.drawable.ic_action_unstarred };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.message_detail_layout, container,
				false);
		initArgs();
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		init();
	}

	private void init() {
		mListMessages = (OList) mView.findViewById(R.id.lstMessageDetail);
		mail = new MailMessage(getActivity());
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnStar, this);
		switch (mType) {
		case inbox:
			if (mId != null) {
				// mRecord = mail.select("id = ? OR parent_id = ? ", new
				// Object[] { mId, mId });
				mRecord = mail.select("id = ? OR parent_id = ?", new Object[] {
						mId, mId }, null, null, "local_write_date");
				// for (ODataRow row : mRecord) {
				// OLog.log(row.getString("is_favorite"));
				// }
				// Check for isFavorite and Set color of The ImageView
				// (imgBtnStar) Here
				// if (isFavorite.equals("true"))
				// btnStar.setColorFilter(Color.YELLOW);
				// else
				// btnStar.setColorFilter(Color.GRAY);

				int count = mail.count("parent_id = ?",
						new String[] { mId + "" });
				mListMessages.initListControl(mRecord);
			} else {
				// oList.setModel(mModel);
			}
			break;
		case tome:
			if (mId != null) {
				mRecord = mail.select(
						"res_id = ? AND to_read = ? OR parent_id = ?",
						new Object[] { 0, true, mId });
				mListMessages.initListControl(mRecord);
			}

			break;
		case todo:
			if (mId != null) {
				mRecord = mail.select(
						"to_read = ? AND is_favorite = ? OR parent_id = ?",
						new Object[] { true, true, mId });
				mListMessages.initListControl(mRecord);
			}

			break;
		case archives:
			if (mId != null) {
				mRecord = mail.select("id = ? AND parent_id = ?", new Object[] {
						mId, mId });
				mListMessages.initListControl(mRecord);
			} else {
				// oList.setModel(mModel);
			}
			break;
		case outbox:

			break;

		default:
			break;
		}
		// mListMessages.setOnListRowViewClickListener(R.id.imgBtnStar,
		// (OnListRowViewClickListener) this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_message_detail, menu);
		// mMenu = menu;

	}

	@Override
	public Object databaseHelper(Context context) {
		return null;
	}

	private void initArgs() {
		// btnStar = (ImageView) mView.findViewById(R.id.imgBtnStar);
		Bundle args = getArguments();
		mType = Message.MType.valueOf(args.getString("key"));
		if (args.containsKey("id")) {
			mLocalRecord = args.getBoolean("local_record");
			// isFavorite = args.getBoolean("is_favorite");
			// if (isFavorite)
			// btnStar.setColorFilter(Color.YELLOW);
			// else
			// btnStar.setColorFilter(Color.GRAY);

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
		OValues values = new OValues();
		switch (item.getItemId()) {
		case R.id.menu_message_read:
			// values.put("to_read", "true/false");
			// new MailMessage(getActivity()).update(values, mId, mLocalRecord);
			Toast.makeText(getActivity(), "Read", Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_message_unread:
			// values.put("to_read", "true/false");
			// new MailMessage(getActivity()).update(values, mId, mLocalRecord);
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

	@Override
	public void onFormViewClick(View view, ODataRow row) {
		ImageView imgStart = (ImageView) view;

	}

	@Override
	public void onRowViewClick(ViewGroup view_group, View view, int position,
			ODataRow row) {
		String is_favorite = "";
		if (view.getId() == R.id.imgBtnStar) {
			ImageView imgstar = (ImageView) view;
			is_favorite = row.getString("is_favorite");
			if (is_favorite.equals("true"))
				imgstar.setColorFilter(Color.YELLOW);
			else
				imgstar.setColorFilter(Color.GRAY);

		}

	}

}