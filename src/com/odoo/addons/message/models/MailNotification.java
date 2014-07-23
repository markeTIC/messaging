package com.odoo.addons.message.models;

import android.content.Context;

import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.OVarchar;

public class MailNotification extends OModel {
	OColumn is_read = new OColumn("Is_Read", OVarchar.class);
	OColumn is_favorite = new OColumn("Is_favorite", OBoolean.class);
	OColumn partner_id = new OColumn("Partner_id", ResPartner.class,
			RelationType.ManyToOne);
	OColumn message_id = new OColumn("Message_id", MailMessage.class,
			RelationType.ManyToOne);

	public MailNotification(Context context) {
		super(context, "mail.notification");
	}
}
