package com.prtech.svarog_notifications;

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataField;
import com.prtech.svarog_common.DbDataField.DbFieldType;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbDataTable;
import com.prtech.svarog_common.IDbInit;

public class DbInit implements IDbInit {
	
	private static DbDataTable addSortOrder(DbDataTable dbtt) {
		Integer order = 100;
		if (dbtt.getDbTableFields() != null)
			for (DbDataField dbf : dbtt.getDbTableFields()) {
				if (dbf != null && dbf.getSort_order() == null) {
					dbf.setSort_order(order);
					order = order + 100;
				}
			}
		return dbtt;
	}

	// SUBJECT
	public static DbDataTable createSubject() {
		DbDataTable dbe = new DbDataTable();
		dbe.setDbTableName(SN.SUBJECT);
		dbe.setDbRepoName(SN.CONST_MASTER_REPO);
		dbe.setDbSchema(SN.CONST_DEFAULT_SCHEMA);
		dbe.setIsSystemTable(false);
		dbe.setIsRepoTable(false);
		dbe.setLabel_code("subject.general");
		dbe.setUse_cache(false);

		// Column 1
		DbDataField dbe1 = new DbDataField();
		dbe1.setDbFieldName(SN.PKID);
		dbe1.setIsPrimaryKey(true);
		dbe1.setDbFieldType(DbFieldType.NUMERIC);
		dbe1.setDbFieldSize(18);
		dbe1.setDbFieldScale(0);
		dbe1.setIsNull(false);
		dbe1.setLabel_code("subject.pkid");

		// Column 2
		DbDataField dbe2 = new DbDataField();
		dbe2.setDbFieldName(SN.MODULE_NAME);
		dbe2.setDbFieldType(DbFieldType.NVARCHAR);
		dbe2.setDbFieldScale(0);
		dbe2.setDbFieldSize(18);
		dbe2.setCode_list_user_code(SN.MODULE_NAME);
		dbe2.setIsNull(false);
		dbe2.setLabel_code("subject.module_name");
		dbe2.setGui_metadata(SN.SUBJECT_CONST);
		dbe2.setSort_order(10004);

		// Column 3
		DbDataField dbe3 = new DbDataField();
		dbe3.setDbFieldName(SN.TITLE);
		dbe3.setDbFieldType(DbFieldType.NVARCHAR);
		dbe3.setDbFieldScale(0);
		dbe3.setDbFieldSize(100);
		dbe3.setIsNull(false);
		dbe3.setLabel_code("subject.title");
		dbe3.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"subject.title\"}}");
		dbe3.setSort_order(10002);

		// Column 4
		DbDataField dbe4 = new DbDataField();
		dbe4.setDbFieldName(SN.CATEGORY);
		dbe4.setDbFieldType(DbFieldType.NVARCHAR);
		dbe4.setDbFieldScale(0);
		dbe4.setDbFieldSize(100);
		dbe4.setCode_list_user_code(SN.CATEGORY);
		dbe4.setIsNull(false);
		dbe4.setLabel_code("subject.category");
		dbe4.setGui_metadata(SN.SUBJECT_CONST);
	dbe4.setSort_order(10006);

		// Column 5
		DbDataField dbe5 = new DbDataField();
		dbe5.setDbFieldName(SN.PRIORITY);
		dbe5.setDbFieldType(DbFieldType.NVARCHAR);
		dbe5.setDbFieldScale(0);
		dbe5.setDbFieldSize(100);
		dbe5.setCode_list_user_code(SN.PRIORITY);
		dbe5.setIsNull(false);
		dbe5.setLabel_code("subject.priority");
		dbe5.setGui_metadata(SN.SUBJECT_CONST);
		dbe5.setSort_order(10008);

		DbDataField[] dbTableFields = new DbDataField[5];
		dbTableFields[0] = dbe1;
		dbTableFields[1] = dbe2;
		dbTableFields[2] = dbe3;
		dbTableFields[3] = dbe4;
		dbTableFields[4] = dbe5;
		dbe.setDbTableFields(dbTableFields);
		return dbe;

	}

	// MESSAGE
	public static DbDataTable createMessage() {
		DbDataTable dbe = new DbDataTable();
		dbe.setDbTableName(SN.MESSAGE);
		dbe.setDbRepoName(SN.CONST_MASTER_REPO);
		dbe.setDbSchema(SN.CONST_DEFAULT_SCHEMA);
		dbe.setIsSystemTable(false);
		dbe.setIsRepoTable(false);
		dbe.setParentName(SN.SUBJECT);
		dbe.setLabel_code("message.general");
		dbe.setUse_cache(false);

		// Column 1
		DbDataField dbe1 = new DbDataField();
		dbe1.setDbFieldName(SN.PKID);
		dbe1.setIsPrimaryKey(true);
		dbe1.setDbFieldType(DbFieldType.NUMERIC);
		dbe1.setDbFieldSize(18);
		dbe1.setDbFieldScale(0);
		dbe1.setIsNull(false);
		dbe1.setLabel_code("message.pkid");

		// Column 2
		DbDataField dbe2 = new DbDataField();
		dbe2.setDbFieldName(SN.TEXT);
		dbe2.setDbFieldType(DbFieldType.NVARCHAR);
		dbe2.setDbFieldScale(0);
		dbe2.setDbFieldSize(2000);
		dbe2.setIsNull(false);
		dbe2.setLabel_code("message.text");
		dbe2.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"message.basic_info\",\"uischema\":{\"ui:widget\":\"textarea\",\"ui:placeholder\":\"Type your message here\"}}}");
		dbe2.setSort_order(10002);

		// Column 3
		DbDataField dbe3 = new DbDataField();
		dbe3.setDbFieldName(SN.CREATED_BY);
		dbe3.setDbFieldType(DbFieldType.NUMERIC);
		dbe3.setDbFieldScale(0);
		dbe3.setDbFieldSize(18);
		dbe3.setLabel_code("message.created_by");
		dbe3.setGui_metadata(
				"{\"react\":{\"filterable\":false,\"visible\":false,\"resizable\":false,\"uischema\":{\"ui:widget\":\"hidden\"},\"grouppath\":\"message.basic_info\"}}");
		dbe3.setSort_order(10008);

		// Column 4
		DbDataField dbe4 = new DbDataField();
		dbe4.setDbFieldName(SN.PRIORITY);
		dbe4.setDbFieldType(DbFieldType.NVARCHAR);
		dbe4.setDbFieldScale(0);
		dbe4.setDbFieldSize(18);
		dbe4.setCode_list_user_code(SN.PRIORITY);
		dbe4.setLabel_code("message.priority");
		dbe4.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"message.basic_info\",\"uischema\":{\"ui:widget\":\"hidden\"}}}");
		dbe4.setSort_order(10012);

		// Column 5
		DbDataField dbe5 = new DbDataField();
		dbe5.setDbFieldName(SN.CREATED_BY_USERNAME);
		dbe5.setDbFieldType(DbFieldType.NVARCHAR);
		dbe5.setDbFieldScale(0);
		dbe5.setDbFieldSize(100);
		dbe5.setLabel_code("message.created_by_username");
		dbe5.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"message.basic_info\",\"uischema\":{\"ui:widget\":\"hidden\"}}}");
		dbe5.setSort_order(10011);

		// Column 6
		DbDataField dbe6 = new DbDataField();
		dbe6.setDbFieldName(SN.HAS_ATTACHMENT);
		dbe6.setDbFieldType(DbFieldType.BOOLEAN);
		dbe6.setLabel_code("message.has_attachment");
		dbe6.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":false,\"resizable\":false,\"uischema\":{\"ui:widget\":\"hidden\"}}}");
		dbe6.setSort_order(10012);

		DbDataField[] dbTableFields = new DbDataField[6];
		dbTableFields[0] = dbe1;
		dbTableFields[1] = dbe2;
		dbTableFields[2] = dbe3;
		dbTableFields[3] = dbe4;
		dbTableFields[4] = dbe5;
		dbTableFields[5] = dbe6;
		dbe.setDbTableFields(dbTableFields);
		return dbe;

	}

	// MESSAGE ATTACHMENT
	public static DbDataTable createMessageAttachement() {
		DbDataTable dbe = new DbDataTable();
		dbe.setDbTableName(SN.MSG_ATTACHEMENT);
		dbe.setDbRepoName(SN.CONST_MASTER_REPO);
		dbe.setDbSchema(SN.CONST_DEFAULT_SCHEMA);
		dbe.setIsSystemTable(false);
		dbe.setIsRepoTable(false);
		dbe.setLabel_code("msg_attachement.general");
		dbe.setUse_cache(false);

		// Column 1
		DbDataField dbe1 = new DbDataField();
		dbe1.setDbFieldName(SN.PKID);
		dbe1.setIsPrimaryKey(true);
		dbe1.setDbFieldType(DbFieldType.NUMERIC);
		dbe1.setDbFieldSize(18);
		dbe1.setDbFieldScale(0);
		dbe1.setIsNull(false);
		dbe1.setLabel_code("msg_attachement.pkid");

		// Column 2
		DbDataField dbe2 = new DbDataField();
		dbe2.setDbFieldName(SN.MSG_ID);
		dbe2.setDbFieldType(DbFieldType.NUMERIC);
		dbe2.setDbFieldScale(0);
		dbe2.setDbFieldSize(18);
		dbe2.setLabel_code("msg_attachement.msg_id");
		dbe2.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"msg_attachement.basic_info\"}}");
		dbe2.setSort_order(10000);

		// Column 3
		DbDataField dbe3 = new DbDataField();
		dbe3.setDbFieldName(SN.ATCH_OBJ_ID);
		dbe3.setDbFieldType(DbFieldType.NUMERIC);
		dbe3.setDbFieldScale(0);
		dbe3.setDbFieldSize(18);
		dbe3.setLabel_code("msg_attachement.atch_obj_id");
		dbe3.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"msg_attachement.attch_info\"}}");
		dbe3.setSort_order(10002);

		// Column 4
		DbDataField dbe4 = new DbDataField();
		dbe4.setDbFieldName(SN.ATCH_OBJ_TYPE);
		dbe4.setDbFieldType(DbFieldType.NUMERIC);
		dbe4.setDbFieldScale(0);
		dbe4.setDbFieldSize(18);
		dbe4.setLabel_code("msg_attachement.atch_obj_type");
		dbe4.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"msg_attachement.attch_info\"}}");
		dbe4.setSort_order(10004);

		// Column 5
		DbDataField dbe5 = new DbDataField();
		dbe5.setDbFieldName(SN.NAME);
		dbe5.setDbFieldType(DbFieldType.NVARCHAR);
		dbe5.setDbFieldScale(0);
		dbe5.setDbFieldSize(200);
		dbe5.setLabel_code("msg_attachement.name");
		dbe5.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"grouppath\":\"msg_attachement.basic_info\"}}");
		dbe5.setSort_order(10006);

		// Column 6
		DbDataField dbe6 = new DbDataField();
		dbe6.setDbFieldName(SN.NOTE);
		dbe6.setDbFieldType(DbFieldType.NVARCHAR);
		dbe6.setDbFieldScale(0);
		dbe6.setDbFieldSize(100);
		dbe6.setLabel_code("msg_attachement.note");
		dbe6.setGui_metadata(
				"{\"react\":{\"filterable\":true,\"visible\":true,\"resizable\":true,\"uischema\":{\"ui:widget\": \"textarea\"},\"grouppath\":\"msg_attachement.note\"}}");
		dbe6.setSort_order(10010);

		DbDataField[] dbTableFields = new DbDataField[6];
		dbTableFields[0] = dbe1;
		dbTableFields[1] = dbe2;
		dbTableFields[2] = dbe3;
		dbTableFields[3] = dbe4;
		dbTableFields[4] = dbe5;
		dbTableFields[5] = dbe6;
		dbe.setDbTableFields(dbTableFields);
		return dbe;

	}

	@Override
	public ArrayList<DbDataTable> getCustomObjectTypes() {
		DbDataTable dbtt = null;
		ArrayList<DbDataTable> dbtList = new ArrayList<DbDataTable>();
		dbtt = DbInit.createSubject();
		dbtList.add(addSortOrder(dbtt));
		dbtt = DbInit.createMessage();
		dbtList.add(addSortOrder(dbtt));
		dbtt = DbInit.createMessageAttachement();
		dbtList.add(addSortOrder(dbtt));
		return dbtList;
	}

	// LINK MESSAGE and SVAROG_USERS (TO)
	public static DbDataObject createLinkMessageToUsers() {
		DbDataObject dbLink = new DbDataObject();
		dbLink.setObjectType(svCONST.OBJECT_TYPE_LINK_TYPE);
		dbLink.setVal(SN.LINK_TYPE, SN.MSG_TO);
		dbLink.setVal(SN.LINK_TYPE_DESCRIPTION, "Link that assign message to user");
		dbLink.setVal(SN.LINK_OBJ_TYPE_1, (SN.MESSAGE));
		dbLink.setVal(SN.LINK_OBJ_TYPE_2, (svCONST.OBJECT_TYPE_USER));
		return dbLink;
	}

	// LINK MESSAGE and SVAROG_USERS (CC)
	public static DbDataObject createLinkMessageCcUsers() {
		DbDataObject dbLink = new DbDataObject();
		dbLink.setObjectType(svCONST.OBJECT_TYPE_LINK_TYPE);
		dbLink.setVal(SN.LINK_TYPE, SN.MSG_CC);
		dbLink.setVal(SN.LINK_TYPE_DESCRIPTION, "Link for carbon copy - cc");
		dbLink.setVal(SN.LINK_OBJ_TYPE_1, (SN.MESSAGE));
		dbLink.setVal(SN.LINK_OBJ_TYPE_2, (svCONST.OBJECT_TYPE_USER));
		return dbLink;
	}

	// LINK MESSAGE and SVAROG_USERS (BCC)
	public static DbDataObject createLinkMessageBccUsers() {
		DbDataObject dbLink = new DbDataObject();
		dbLink.setObjectType(svCONST.OBJECT_TYPE_LINK_TYPE);
		dbLink.setVal(SN.LINK_TYPE, SN.MSG_BCC);
		dbLink.setVal(SN.LINK_TYPE_DESCRIPTION, "Link for Blind carbon copy - bcc");
		dbLink.setVal(SN.LINK_OBJ_TYPE_1, (SN.MESSAGE));
		dbLink.setVal(SN.LINK_OBJ_TYPE_2, (svCONST.OBJECT_TYPE_USER));
		return dbLink;
	}

	@Override
	public ArrayList<DbDataObject> getCustomObjectInstances() {
		DbDataObject dbtt = null;
		ArrayList<DbDataObject> dbtList = new ArrayList<DbDataObject>();
		dbtt = DbInit.createLinkMessageToUsers();
		dbtList.add((dbtt));
		dbtt = DbInit.createLinkMessageCcUsers();
		dbtList.add((dbtt));
		dbtt = DbInit.createLinkMessageBccUsers();
		dbtList.add((dbtt));
		return dbtList;
	}

	// setting basic configuration on field R.P*
	public static JsonObject getDefaultUiMeta(Boolean isReadonly, Boolean isHidden, Boolean isEditable,
			/* Boolean isNumber */ Boolean isEditrules) {
		JsonObject obj = new JsonObject();
		if (isEditable)
			obj.addProperty("editable", true);
		if (isHidden)
			obj.addProperty("hidden", true);
		if (isReadonly) {
			JsonObject subObj = new JsonObject();
			subObj.addProperty("readonly", true);
			obj.add("editoptions", subObj);
		}
		if (isEditrules) {
			JsonObject subObj = new JsonObject();
			subObj.addProperty("edithidden", true);
			subObj.addProperty("required", true);
			obj.add("editrules", subObj);
		}
		return obj;
	}

	/* za Formatoptions f.r */
	public static JsonObject getUiForm(JsonObject obj, Integer uiRow, Integer uiCol) {
		JsonObject subObj = new JsonObject();
		subObj.addProperty("rowpos", uiRow);
		subObj.addProperty("colpos", uiCol);
		obj.add("formoptions", subObj);
		return obj;
	}

}
