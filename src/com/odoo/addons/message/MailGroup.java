package com.odoo.addons.message;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.addons.message.models.MailGroupDB;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;

public class MailGroup extends BaseFragment {

	@Override
	public Object databaseHelper(Context context) {
		return new MailGroupDB(context);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

}
