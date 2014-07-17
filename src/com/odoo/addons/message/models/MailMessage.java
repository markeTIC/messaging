package com.odoo.addons.message.models;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

public class MailMessage extends OModel {
	OColumn partner_ids = new OColumn("partner_ids", ResPartner.class,
			RelationType.ManyToMany);

	OColumn subject = new OColumn("subject", OText.class);
	OColumn type = new OColumn("type", OVarchar.class, 30);
	OColumn body = new OColumn("body", OText.class);
	OColumn email_from = new OColumn("email_from", OText.class);
	OColumn parent_id = new OColumn("parent_id", MailMessage.class,
			RelationType.ManyToOne);
	OColumn record_name = new OColumn("record_name", OText.class);
	OColumn to_read = new OColumn("to_read", OBoolean.class);
	OColumn author_id = new OColumn("author_id", ResPartner.class,
			RelationType.ManyToOne);
	OColumn model = new OColumn("model", OVarchar.class, 50);
	OColumn res_id = new OColumn("res_id", OText.class);
	OColumn date = new OColumn("date", ODateTime.class);
	OColumn has_voted = new OColumn("has_voted", OBoolean.class);
	OColumn vote_nb = new OColumn("vote_nb", OInteger.class);
	OColumn is_favorite = new OColumn("Is Favorite", OBoolean.class);

	OColumn attachment_ids = new OColumn("attachment_ids", IrAttachment.class,
			RelationType.ManyToMany);

	public MailMessage(Context context) {
		super(context, "mail.message");
	}

	@Override
	public JSONObject beforeCreateRow(OColumn column, JSONObject original_record) {
		try {
			// check for parent id
			if (column.equals(parent_id)) {
				JSONArray parent_id = new JSONArray();
				parent_id.put(original_record.getInt(column.getName()));
				parent_id.put("Parent");
				original_record.put(column.getName(), parent_id);
			}
			// Check for author and email_from
			if (column.equals(author_id)) {
				JSONArray author_id = original_record.getJSONArray(column
						.getName());
				if (author_id.getInt(0) == 0) {
					original_record.put("email_from", author_id.get(1));
					original_record.put("author_id", false);
				} else {
					original_record.put("email_from", false);
				}
			}
			// Check for partner_ids
			if (column.equals(partner_ids)) {
				JSONArray partner_ids = original_record
						.getJSONArray("partner_ids");
				JSONArray partner_ids_list = new JSONArray();
				for (int i = 0; i < partner_ids.length(); i++) {
					partner_ids_list.put(partner_ids.getJSONArray(i).get(0));
				}
				original_record.put("partner_ids", partner_ids_list);
			}
			// Check for attachment ids
			if (column.equals(attachment_ids)) {
				JSONArray attachment_ids_list = new JSONArray();
				JSONArray attachment_ids = original_record
						.getJSONArray("attachment_ids");
				for (int i = 0; i < attachment_ids.length(); i++)
					attachment_ids_list.put(attachment_ids.getJSONObject(i)
							.getInt("id"));
				original_record.put("attachment_ids", attachment_ids_list);
			}
			return original_record;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.beforeCreateRow(column, original_record);
	}
}
