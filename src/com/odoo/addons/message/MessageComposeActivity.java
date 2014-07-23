package com.odoo.addons.message;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.odoo.base.ir.Attachment;
import com.odoo.orm.ODataRow;
import com.openerp.R;

public class MessageComposeActivity extends Activity {
	Context mContext = null;

	Attachment mAttachment = null;
	List<Object> mAttachments = new ArrayList<Object>();

	enum AttachmentType {
		IMAGE, FILE
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		// Intent i = getIntent();
		// if (i != null) {
		// String name = i.getExtras().getString("name");
		// Toast.makeText(mContext, name, Toast.LENGTH_LONG).show();
		// }
		setContentView(R.layout.activity_message_compose);
		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.message_compose, menu);
		return true;
	}

	public void init() {
		mAttachment = new Attachment(mContext);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			ODataRow attachment = mAttachment.handleResult(requestCode, data);
			mAttachments.add(attachment);
		}
		super.onActivityResult(requestCode, resultCode, data);

	}

	@Override
	public void onBackPressed() {
		getFragmentManager().popBackStack();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.menu_message_compose:
			break;
		case R.id.menu_message_compose_add_attachment_images:
			// mAttachment
			// .requestAttachment(Attachment.Types.IMAGE_OR_CAPTURE_IMAGE);
			return true;

		case R.id.menu_message_compose_add_attachment_files:
			// mAttachment.requestAttachment(Attachment.Types.FILE);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
