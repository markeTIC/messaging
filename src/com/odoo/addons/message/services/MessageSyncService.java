package com.odoo.addons.message.services;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.ODomain;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.odoo.MainActivity;
import com.odoo.addons.message.models.MailMessage;
import com.odoo.auth.OdooAccountManager;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.OUser;
import com.odoo.support.service.OService;
import com.odoo.util.ODate;
import com.odoo.util.OENotificationHelper;
import com.odoo.util.PreferenceManager;
import com.openerp.R;

public class MessageSyncService extends OService {
	public static final String TAG = MessageSyncService.class.getSimpleName();

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Intent intent = new Intent();
		intent.setAction(SyncFinishReceiver.SYNC_FINISH);
		OUser user = OdooAccountManager.getAccountDetail(context, account.name);
		try {
			MailMessage mdb = new MailMessage(context);
			mdb.setUser(user);
			OSyncHelper oe = mdb.getSyncHelper();
			if (oe == null) {
				return;
			}
			int user_id = user.getUser_id();
			JSONObject newContext = new JSONObject();
			newContext.put("default_model", "res.users");
			newContext.put("default_res_id", user_id);

			OArguments arguments = new OArguments();
			arguments.addNull();
			ODomain domain = new ODomain();

			PreferenceManager mPref = new PreferenceManager(context);
			int data_limit = mPref.getInt("sync_data_limit", 60);
			domain.add("create_date", ">=", ODate.getDateBefore(data_limit));

			if (!extras.containsKey("group_ids")) {
				JSONArray msgIds = new JSONArray(mdb.ids().toString());
				domain.add("id", "not in", msgIds);
				domain.add("|");
				domain.add("partner_ids.user_ids", "in",
						new JSONArray().put(user_id));
				domain.add("|");
				domain.add("notification_ids.partner_id.user_ids", "in",
						new JSONArray().put(user_id));

				domain.add("author_id.user_ids", "in",
						new JSONArray().put(user_id));
			} else {
				JSONArray group_ids = new JSONArray(
						extras.getString("group_ids"));

				domain.add("model", "=", "mail.group");
				domain.add("res_id", "in", group_ids);
			}
			arguments.add(domain.getArray());
			arguments.add(new JSONArray());
			arguments.add(true);
			arguments.add(oe.getContext(null));
			arguments.addNull();
			arguments.add(50);
			List<Integer> ids = mdb.ids();
			if (oe.syncWithMethod("message_read", arguments)) {
				int affected_rows = mdb.count("to_read = ?",
						new Object[] { true });
				List<Integer> affected_ids = oe.getAffectedIds();
				boolean notification = true;
				ActivityManager am = (ActivityManager) context
						.getSystemService(ACTIVITY_SERVICE);
				List<ActivityManager.RunningTaskInfo> taskInfo = am
						.getRunningTasks(1);
				ComponentName componentInfo = taskInfo.get(0).topActivity;
				if (componentInfo.getPackageName().equalsIgnoreCase(
						"com.openerp")) {
					notification = false;
				}
				if (notification && affected_rows > 0) {
					OENotificationHelper mNotification = new OENotificationHelper();
					Intent mainActiivty = new Intent(context,
							MainActivity.class);
					mNotification.setResultIntent(mainActiivty, context);
					mNotification.showNotification(context, affected_rows
							+ " new messages", affected_rows
							+ " new message received (OpneERP)", authority,
							R.drawable.ic_odoo_o);
				}
				intent.putIntegerArrayListExtra("new_ids",
						(ArrayList<Integer>) affected_ids);
			}
			List<Integer> updated_ids = updateOldMessages(mdb, oe, user, ids);
			intent.putIntegerArrayListExtra("updated_ids",
					(ArrayList<Integer>) updated_ids);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private List<Integer> updateOldMessages(MailMessage db, OSyncHelper oe,
			OUser user, List<Integer> ids) {
		Log.d(TAG, "MessageSyncServide->updateOldMessages()");
		List<Integer> updated_ids = new ArrayList<Integer>();
		try {
			JSONArray ids_array = new JSONArray();
			for (int id : ids)
				ids_array.put(id);
			JSONObject fields = new JSONObject();

			fields.accumulate("fields", "read");
			fields.accumulate("fields", "starred");
			fields.accumulate("fields", "partner_id");
			fields.accumulate("fields", "message_id");

			ODomain domain = new ODomain();
			domain.add("message_id", "in", ids_array);
			domain.add("partner_id", "=", user.getPartner_id());
			JSONObject result = oe.search_read("mail.notification", fields,
					domain.get());
			for (int j = 0; j < result.getJSONArray("records").length(); j++) {
				JSONObject objRes = result.getJSONArray("records")
						.getJSONObject(j);
				int message_id = objRes.getJSONArray("message_id").getInt(0);
				boolean read = objRes.getBoolean("read");
				boolean starred = objRes.getBoolean("starred");
				OValues values = new OValues();
				values.put("starred", starred);
				values.put("to_read", !read);
				db.update(values, message_id);
				updated_ids.add(message_id);
			}
			updateMessageVotes(db, oe, user, ids_array);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return updated_ids;
	}

	private void updateMessageVotes(MailMessage db, OSyncHelper oe, OUser user,
			JSONArray ids_array) {
		Log.d(TAG, "MessageSyncServide->updateMessageVotes()");
		try {
			JSONObject vote_fields = new JSONObject();
			vote_fields.accumulate("fields", "vote_user_ids");

			ODomain domain = new ODomain();
			domain.add("id", "in", ids_array);
			JSONObject vote_detail = oe.search_read("mail.message",
					vote_fields, domain.get());
			// .search_read("mail.message",vote_fields, domain.get(), 0, 0,
			// null, null);
			for (int j = 0; j < vote_detail.getJSONArray("records").length(); j++) {
				JSONObject obj_vote = vote_detail.getJSONArray("records")
						.getJSONObject(j);
				JSONArray voted_user_ids = obj_vote
						.getJSONArray("vote_user_ids");
				OValues values = new OValues();
				for (int i = 0; i < voted_user_ids.length(); i++) {
					if (voted_user_ids.getInt(i) == user.getUser_id()) {
						values.put("has_voted", true);
						break;
					} else {
						values.put("has_voted", false);
					}
				}
				int total_votes = voted_user_ids.length();
				int message_id = obj_vote.getInt("id");
				values.put("vote_nb", total_votes);
				db.update(values, message_id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
