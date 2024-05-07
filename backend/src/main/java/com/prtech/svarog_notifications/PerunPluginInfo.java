package com.prtech.svarog_notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonObject;
import com.prtech.svarog_interfaces.IPerunPlugin;
import com.prtech.svarog_interfaces.ISvCore;

public class PerunPluginInfo implements IPerunPlugin {

	static final String confPath = "configuration";
	static final String context = "svarog-notifications";

	@Override
	public int getVersion() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public String getContextName() {
		// TODO Auto-generated method stub
		return context;
	}

	@Override
	public String getJsPluginUrl() {
		return "svarog-notifications.js";
	}

	@Override
	public String getIconPath() {
		return "/perun-assets/img/access_cards/svarog-notifications.svg";
	}

	@Override
	public String getLabelCode() {
		// TODO Auto-generated method stub
		return "perun.plugin.svarog-notifications";
	}

	@Override
	public String getPermissionCode() {
		// TODO Auto-generated method stub
		return "card.svarog-notifications";
	}

	@Override
	public int getSortOrder() {
		// TODO Auto-generated method stub
		return 25;
	}

	@Override
	public boolean replaceContextMenuOnNew() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public JsonObject getMenu(JsonObject existingMenu, ISvCore core) {
		JsonObject result = new JsonObject();
		return result;
	}

	@Override
	public JsonObject getContextMenu(HashMap<String, String> contextMap, JsonObject existingMenu, ISvCore core) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean replaceMenuOnNew() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public List<String> dependencies() {
		List<String> deps = new ArrayList<String>();
		return deps;
	}

}
