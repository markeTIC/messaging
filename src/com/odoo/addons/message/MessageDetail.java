package com.odoo.addons.message;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.addons.message.models.MailMessage;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.R;

public class MessageDetail extends BaseFragment {
	View mView = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.message_detail_layout, container,
				false);
		return mView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_message_detail, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_message_detail_send:
			startFragment(new Message(), false);
			break;
		case R.id.menu_message_detail_attachmet:

			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

}
