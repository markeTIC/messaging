package com.odoo.addons.message.providers.groups;

import com.odoo.support.provider.OContentProvider;

public class MailGroupProvider extends OContentProvider {
	public static String CONTENTURI = "com.odoo.addons.message.providers.groups.MailGroupProvider";
	public static String AUTHORITY = "com.odoo.addons.message.providers.groups";

	@Override
	public String authority() {
		return AUTHORITY;
	}

	@Override
	public String contentUri() {
		return CONTENTURI;
	}

}
