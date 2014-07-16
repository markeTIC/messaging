package com.odoo.addons.message.services;

import odoo.ODomain;

import org.json.JSONArray;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

import com.odoo.addons.message.models.MailGroupDB;
import com.odoo.addons.message.providers.groups.MailGroupProvider;
import com.odoo.auth.OdooAccountManager;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.OUser;
import com.odoo.support.service.OService;

public class MailGroupSyncService extends OService {
	public static final String TAG = "com.odoo.addons.message.services.MailGroupSyncService";

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		try {
			Intent intent = new Intent();
			intent.setAction(SyncFinishReceiver.SYNC_FINISH);
			OUser user = OdooAccountManager.getAccountDetail(context,
					account.name);
			MailGroupDB db = new MailGroupDB(context);
			db.setUser(user);
			OSyncHelper oe = db.getSyncHelper();
			if (oe != null && oe.syncWithServer()) {
				MailFollowers followers = new MailFollowers(context);
				ODomain domain = new ODomain();
				domain.add("partner_id", "=", user.getPartner_id());
				domain.add("res_model", "=", db.getModelName());
				if (followers.getSyncHelper().syncWithServer(domain)) {
					// syncing group messages
					JSONArray group_ids = new JSONArray();
					for (ODataRow grp : followers.select(
							"res_model = ? AND partner_id = ?", new String[] {
									db.getModelName(),
									user.getPartner_id() + "" })) {
						group_ids.put(grp.getInt("res_id"));
					}
					Bundle messageBundle = new Bundle();
					messageBundle.putString("group_ids", group_ids.toString());
					messageBundle.putBoolean(
							ContentResolver.SYNC_EXTRAS_MANUAL, true);
					messageBundle.putBoolean(
							ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
					ContentResolver.requestSync(account,
							MailGroupProvider.AUTHORITY, messageBundle);
				}
			}
			if (OdooAccountManager.currentUser(context).getAndroidName()
					.equals(account.name))
				context.sendBroadcast(intent);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
