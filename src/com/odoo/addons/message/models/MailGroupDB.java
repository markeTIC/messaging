package com.odoo.addons.message.models;

import android.content.Context;

import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBlob;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

public class MailGroupDB extends OModel {
	OColumn name = new OColumn("name", OVarchar.class, 64);
	OColumn description = new OColumn("description", OText.class);
	OColumn image_medium = new OColumn("image_medium", OBlob.class);

	public MailGroupDB(Context context) {
		super(context, "mail.group");
	}

}