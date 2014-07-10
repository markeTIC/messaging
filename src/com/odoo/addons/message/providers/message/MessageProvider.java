package com.odoo.addons.message.providers.message;

import com.odoo.support.provider.OContentProvider;

public class MessageProvider extends OContentProvider {

	public static String CONTENTURI = "com.odoo.addons.message.providers.message.MessageProvider";
	public static String AUTHORITY = "com.odoo.addons.message.providers.message";

	@Override
	public String authority() {
		return AUTHORITY;
	}

	@Override
	public String contentUri() {
		return CONTENTURI;
	}

}
