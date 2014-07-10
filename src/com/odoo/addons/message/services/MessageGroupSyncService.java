package com.odoo.addons.message.services;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.odoo.support.service.OService;

public class MessageGroupSyncService extends OService {
	public static final String TAG = "com.odoo.addons.messaging.services.MailGroupSyncService";
	private static SyncAdapterImpl sSyncAdapter = null;
	static int i = 0;
	Context mContext = null;

	public MessageGroupSyncService() {
		mContext = this;
	}

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {

	}

	public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context mContext;

		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle bundle, String str,
				ContentProviderClient providerClient, SyncResult syncResult) {
			Log.d(TAG, "Message Group sync service started");
			try {
				if (account != null) {
					new MessageGroupSyncService().performSync(mContext,
							account, bundle, str, providerClient, syncResult);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

}
