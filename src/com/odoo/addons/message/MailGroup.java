package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.Odoo;
import odoo.controls.OList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.addons.message.Message.MType;
import com.odoo.addons.message.models.MailGroupDB;
import com.odoo.addons.message.models.MailMessage;
import com.odoo.addons.message.providers.groups.MailGroupProvider;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.support.OUser;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.drawer.DrawerListener;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class MailGroup extends BaseFragment implements OnPullListener {

	private App mApp = null;
	private Odoo odoo = null;
	public static final String TAG = "com.openerp.addons.message.MailGroup";
	GroupsLoader mGroupsLoader = null;
	JoinUnfollowGroup mJoinUnFollowGroup = null;

	private OETouchListener mTouchListener;

	View mView = null;
	Boolean hasSynced = false;
	OList mGroupGridView = null;
	List<Object> mGroupGridListItems = new ArrayList<Object>();

	public static HashMap<String, Object> mMenuGroups = new HashMap<String, Object>();
	String mTagColors[] = new String[] { "#218559", "#192823", "#FF8800",
			"#CC0000", "#59A2BE", "#808080", "#9933CC", "#0099CC", "#669900",
			"#EBB035" };
	MailFollowers mMailFollowerDB = null;

	@Override
	public Object databaseHelper(Context context) {
		return new MailGroupDB(context);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.fragment_message_groups_list,
				container, false);
		mApp = (App) getActivity().getApplicationContext();
		if (mApp.inNetwork())
			odoo = mApp.getOdoo();
		init();
		return mView;
	}

	private void init() {
		scope = new AppScope(getActivity());
		mMailFollowerDB = new MailFollowers(getActivity());
		initControls();
		mGroupsLoader = new GroupsLoader();
		mGroupsLoader.execute();
	}

	private void initControls() {
		mGroupGridView = (OList) mView.findViewById(R.id.listGroups);
		mTouchListener = scope.main().getTouchAttacher();
		mTouchListener.setPullableView(mGroupGridView, this);
	}

	class GroupsLoader extends AsyncTask<Void, Void, Void> {

		public GroupsLoader() {
			mView.findViewById(R.id.loadingProgress)
					.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			mGroupGridListItems.clear();
			mGroupGridListItems.addAll(db().select());
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
			mGroupsLoader = null;
			checkStatus();
		}
	}

	private void checkStatus() {
		if (!db().isEmptyTable()) {
			mView.findViewById(R.id.groupSyncWaiter).setVisibility(View.GONE);
		} else {
			mView.findViewById(R.id.groupSyncWaiter)
					.setVisibility(View.VISIBLE);
			TextView txvSyncDetail = (TextView) mView
					.findViewById(R.id.txvMessageHeaderSubtitle);
			txvSyncDetail.setText("Your groups will appear shortly");
			if (!hasSynced) {
				hasSynced = true;
				scope.main().requestSync(MailGroupProvider.AUTHORITY);
			}
		}
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		MailGroupDB db = new MailGroupDB(context);
		mMailFollowerDB = new MailFollowers(context);

		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "My Groups", true));
		menu.add(new DrawerItem(TAG, "Groups", count(context, MType.group), 0,
				getFragment(MType.group)));
		MailGroup group = new MailGroup();
		Bundle bundle = new Bundle();

		// if (((Object) db).isInstalledOnServer()) {
		// menu.add(new DrawerItem(TAG, "My Groups", true));

		// Join Group
		group.setArguments(bundle);
		// menu.add(new DrawerItem(TAG, "Join Group", 0,
		// R.drawable.ic_action_social_group, group));

		// Dynamic Groups
		List<ODataRow> groups = mMailFollowerDB.select(
				"res_model = ? AND partner_id = ?",
				new String[] { db.getModelName(),
						OUser.current(context).getPartner_id() + "" });
		int index = 0;
		MailMessage messageDB = new MailMessage(context);
		for (ODataRow row : groups) {
			if (mTagColors.length - 1 < index)
				index = 0;
			ODataRow grp = db.select(row.getInt("res_id"));
			if (grp != null) {
				Message message = new Message();
				bundle = new Bundle();
				bundle.putInt("group_id", grp.getInt("id"));
				message.setArguments(bundle);

				int count = messageDB.count(
						"to_read = ? AND model = ? AND res_id = ?",
						new String[] { "true", db().getModelName(),
								row.getString("id") });
				menu.add(new DrawerItem(TAG, grp.getString("name"), count,
						mTagColors[index], message));
				grp.put("tag_color", Color.parseColor(mTagColors[index]));
				mMenuGroups.put("group_" + grp.getInt("id"), grp);
				index++;
			}
		}
		// }
		return menu;
	}

	private Fragment getFragment(MType value) {
		MailGroup mg = new MailGroup();
		Bundle args = new Bundle();
		args.putString("type", value.toString());
		mg.setArguments(args);
		return mg;
	}

	private int count(Context context, MType key) {
		int count = 0;
		return count;
	}

	public class JoinUnfollowGroup extends AsyncTask<Void, Void, Boolean> {
		int mGroupId = 0;
		boolean mJoin = false;
		String mToast = "";
		JSONObject result = new JSONObject();

		public JoinUnfollowGroup(int group_id, boolean join) {
			mGroupId = group_id;
			mJoin = join;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				if (mMailFollowerDB == null)
					mMailFollowerDB = new MailFollowers(getActivity());
				int partner_id = OUser.current(getActivity()).getPartner_id();
				OSyncHelper oe = db().getSyncHelper();
				if (oe == null) {
					mToast = "No Connection";
					return false;
				}

				JSONArray arguments = new JSONArray();
				arguments.put(new JSONArray().put(mGroupId));
				arguments.put(odoo.updateContext(new JSONObject()));

				if (mJoin) {
					odoo.call_kw("mail.group", "action_follow", arguments);
					// odoo.call_kw("action_follow", arguments, null);
					mToast = "Group joined";
					oe.syncWithServer();
				} else {
					odoo.call_kw("mail.group", "action_unfollow", arguments);
					mToast = "Unfollowed from group";
					mMailFollowerDB.delete(
							"res_id = ? AND partner_id = ? AND res_model = ? ",
							new String[] { mGroupId + "", partner_id + "",
									db().getModelName() });

				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Toast.makeText(getActivity(), mToast, Toast.LENGTH_LONG).show();
			DrawerListener drawer = (DrawerListener) getActivity();
			drawer.refreshDrawer(TAG);
			drawer.refreshDrawer(Message.TAG);
		}
	}

	@Override
	public void onPullStarted(View arg0) {
		scope.main().requestSync(MailGroupProvider.AUTHORITY);

	}

}
