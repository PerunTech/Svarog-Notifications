package com.prtech.svarog_notifications;

import com.google.gson.JsonObject;
import com.prtech.svarog_interfaces.ISvCore;
import com.prtech.svarog_interfaces.MenuGenerator;

public class BundleMenu extends MenuGenerator {

	public BundleMenu(JsonObject initialJsonObject, String moduleTitle, String menuTitle, ISvCore svr) {
		super(initialJsonObject, moduleTitle, menuTitle, svr);
	}

}