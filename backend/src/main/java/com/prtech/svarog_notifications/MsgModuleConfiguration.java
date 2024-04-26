package com.prtech.svarog_notifications;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import com.prtech.svarog_interfaces.ISvConfigurationMulti;
import com.prtech.svarog_interfaces.ISvCore;

public class MsgModuleConfiguration implements ISvConfigurationMulti {

	@Override
	public int executionOrder(UpdateType updateType) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String beforeSchemaUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String beforeLabelsUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String beforeCodesUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String beforeTypesUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String beforeLinkTypesUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String beforeAclUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String beforeSidAclUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String afterUpdate(Connection conn, ISvCore core, String schema) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getVersion(int currentVersion) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<UpdateType> getUpdateTypes() {
		return Arrays.asList(UpdateType.ACL);
	}
}
