package com.prtech.svarog_notifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.prtech.svarog.CodeList;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvParameter;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.ResponseHandler;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.ResponseHandler.MessageType;

@Path("/svarog_notifications/services")
public class SvarogNotificationsServices {
	/**
	 * Log4j instance used for logging
	 */
	static final Logger log4j = LogManager.getLogger(SvarogNotificationsServices.class.getName());

	private Response handleException(Exception e, ResponseHandler jrh, String message) {
		if (e instanceof SvException) {
			SvException sve = (SvException) e;
			log4j.error(sve.getFormattedMessage(), sve);
			if (sve.getLabelCode().equals(SN.ERROR_INVALID_SESSION)) {
				jrh.create(MessageType.ERROR, I18n.getText(SN.ERROR_INVALID_SESSION),
						I18n.getText(SN.ERROR_INVALID_SESSION), new JsonObject());
				return Response.status(401).entity(jrh.getAll().toString()).build();
			} else if (sve.getLabelCode().equals(SN.ERROR_USER_NOT_AUTHORIZED)) {
				jrh.create(MessageType.ERROR, I18n.getText(SN.ERROR_USER_NOT_AUTHORIZED),
						I18n.getText(SN.ERROR_USER_NOT_AUTHORIZED), new JsonObject());
				return Response.status(403).entity(jrh.getAll().toString()).build();
			}
		} else {
			log4j.error(e.getMessage(), e);
			jrh.create(MessageType.ERROR, I18n.getText(message), I18n.getText(message), new JsonObject());
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	public static void debugException(Exception e) {
		if (log4j.isDebugEnabled())
			log4j.debug(e.getMessage(), e);
	}

	private static void debugSvException(SvException e) {
		if (log4j.isDebugEnabled())
			log4j.debug(e.getFormattedMessage(), e);
	}

	/**
	 * procedure to to release SvReader, SvWriter, SvParameter and probably some
	 * more
	 * 
	 * @param svc connected SvCore
	 */
	public static void releaseAll(SvCore svc) {
		if (svc != null)
			svc.release();
	}

	/**
	 * procedure to find the type of the object, so we can use table_name or
	 * object_type when calling any Web Service
	 * 
	 * @param objectName String that could be number (ID) of the table or the name
	 *                   of the table
	 * 
	 * @return Long with ObjectType ID, 0 if we could not find the table
	 */
	public static Long findTableType(String objectName) {
		Long pobjectType = 0L;
		try {
			if (objectName != null && objectName.matches("\\d*")) {
				pobjectType = Long.parseLong(objectName);
			} else if (objectName != null) {
				pobjectType = SvCore.getTypeIdByName(objectName, null);
			}
		} catch (Exception e) {
			debugException(e);
			log4j.error("object type (table) not found: " + objectName, e);
		}
		return pobjectType;
	}

	/**
	 * procedure to check the name of the field, we are not able to return or
	 * process fields : PKID, since this is the connection to SVAROG table,
	 * GUI_METADATA it has lots of JSON in it and configurations, CENTROID and GEOM
	 * are complex data-type and can't be displayed as any other similar format
	 * 
	 * @param fieldName String field name that we like to process
	 * 
	 * @return true if the field is "normal" and can be processed, false if its
	 *         complex or funny field
	 */
	public static Boolean processField(String fieldName) {
		Boolean retVal = false;
		if (!SN.PKID.equalsIgnoreCase(fieldName) && !SN.GUI_METADATA.equalsIgnoreCase(fieldName)
				&& !"CENTROID".equalsIgnoreCase(fieldName) && !"GEOM".equalsIgnoreCase(fieldName))
			retVal = true;
		return retVal;
	}

	/**
	 * Method for finding locale id per user, If not set returns default
	 * 
	 * @param svr SvReader instance
	 */
	private static String getLocaleId(SvReader svr) {
		String locale = SvConf.getDefaultLocale();
		try {
			if (svr.getUserLocale(SvReader.getUserBySession(svr.getSessionId())) != null) {
				DbDataObject localeObj = svr.getUserLocale(SvReader.getUserBySession(svr.getSessionId()));
				if (localeObj.getVal("LOCALE_ID").toString() != null)
					locale = localeObj.getVal("LOCALE_ID").toString();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return locale;
	}

	public JsonObject getReactGuiDataByField(DbDataObject field) {
		JsonObject guiMetadata = null;
		if (field.getVal(SN.GUI_METADATA) != null)
			guiMetadata = (new Gson()).fromJson(field.getVal(SN.GUI_METADATA).toString(), JsonObject.class);
		JsonObject jsonreactGUI = null;
		if (guiMetadata != null && guiMetadata.has(SN.REACT))
			jsonreactGUI = (JsonObject) guiMetadata.get(SN.REACT);
		return jsonreactGUI;
	}

	/**
	 * procedure to add JSONSchema values into the Json schema object, this is
	 * working for tables and forms
	 * 
	 * @param fieldType DbDataObject one field that we like to add to the JSON
	 *                  Schema
	 * @param jLeaf     JsonObject Object that already has some of the fields that
	 *                  are in same table/form
	 * @param isTable   Boolean is true if we process table, false if we process
	 *                  form, since fields are not the same
	 * 
	 * @return JsonObject with new type of field added
	 */

	private JsonObject addFieldTypeToJsonObject(DbDataObject fieldType, JsonObject jLeaf, Boolean isTable) {
		// if numeric field is part of table, we have to check the scale, so we
		// know if its integer or float, and if its form, we set to float all
		// the time
		Gson gson = new Gson();
		JsonObject jsonreactGUI = null;
		JsonObject guiMetadata = null;
		switch (fieldType.getVal(SN.FIELD_TYPE).toString()) {
		case SN.NVARCHAR:
		case "TEXT":
			jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.STRING);
			break;
		case SN.NUMERIC:
			if (!isTable) {
				Long fieldScale = getFieldScaleFromGuiMetaData(fieldType);
				if (fieldScale != null && fieldScale.compareTo(0L) > 0) {
					jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.NUMBER_LCASE);
				} else {
					jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.INTEGER_LCASE); // will always be whole number
				}
			} else {
				Long tmpL = (Long) fieldType.getVal(SN.FIELD_SCALE);
				if (tmpL != null && tmpL > 0) {
					jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.NUMBER_LCASE);
				} else {
					jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.INTEGER_LCASE);
				}
			}
			break;
		case SN.DATE:
			jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.STRING);
			jLeaf.addProperty("format", "date");
			jLeaf.addProperty(SN.DATETYPE_LCASE, "shortdate");
			break;
		case SN.TIMESTAMP:
		case SN.DATETIME:
			jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, SN.STRING);
			jLeaf.addProperty("format", "date-time");
			jLeaf.addProperty(SN.DATETYPE_LCASE, "longdate");
			break;
		case SN.BOOLEAN:
			jLeaf.addProperty(SN.TYPE_WITH_LOWERCASE, "boolean");
			break;
		default:
		}
		try {
			if (fieldType.getVal(SN.GUI_METADATA) != null)
				guiMetadata = gson.fromJson(fieldType.getVal(SN.GUI_METADATA).toString(), JsonObject.class);
		} catch (Exception e) {
			debugException(e);
		}
		if (guiMetadata != null && guiMetadata.has(SN.REACT)) {
			jsonreactGUI = (JsonObject) guiMetadata.get(SN.REACT);
		}
		if (jsonreactGUI != null && jsonreactGUI.has(SN.DEFAULT_LCASE))
			switch (fieldType.getVal(SN.FIELD_TYPE).toString()) {
			case SN.NUMERIC:
				if (!isTable) {
					jLeaf.addProperty(SN.DEFAULT_LCASE, jsonreactGUI.get(SN.DEFAULT_LCASE).getAsNumber());
				} else {
					Long tmpL = (Long) fieldType.getVal(SN.FIELD_SCALE);
					if (tmpL != null && tmpL > 0) {
						jLeaf.addProperty(SN.DEFAULT_LCASE, jsonreactGUI.get(SN.DEFAULT_LCASE).getAsNumber());
					} else {
						jLeaf.addProperty(SN.DEFAULT_LCASE, jsonreactGUI.get(SN.DEFAULT_LCASE).getAsInt());
					}
				}
				break;
			case SN.NVARCHAR:
			case SN.DATE:
			case SN.TIMESTAMP:
			case SN.DATETIME:
				jLeaf.addProperty(SN.DEFAULT_LCASE, jsonreactGUI.get(SN.DEFAULT_LCASE).getAsString());
				break;
			case SN.BOOLEAN:
				jLeaf.addProperty(SN.DEFAULT_LCASE, jsonreactGUI.get(SN.DEFAULT_LCASE).getAsBoolean());
				break;
			default:
				jLeaf.addProperty(SN.DEFAULT_LCASE, jsonreactGUI.get(SN.DEFAULT_LCASE).getAsString());
			}
		return jLeaf;
	}

	private Long getFieldScaleFromGuiMetaData(DbDataObject field) {
		JsonObject guiMetadata = null;
		if (field.getVal(SN.GUI_METADATA) != null)
			guiMetadata = (new Gson()).fromJson(field.getVal(SN.GUI_METADATA).toString(), JsonObject.class);
		Long jsonGUI = null;
		if (guiMetadata != null && guiMetadata.has(SN.FIELD_SCALE.toLowerCase()))
			jsonGUI = guiMetadata.get(SN.FIELD_SCALE.toLowerCase()).getAsLong();
		return jsonGUI;
	}

	/**
	 * procedure to produce string of list of codes from given table with one
	 * filter. it looks like it has similar data as prepareJsonCodeList, but the
	 * point is that order of IDs and values must be preserved
	 * 
	 * @param guiMetadata String metadata for the field that may contain some
	 *                    codelist
	 * @param jsonObj     JsonObject object has all previous data for the field that
	 *                    we are processing
	 * @param svr         connected SvReader
	 * 
	 * @return JsonObject
	 * @throws SvException 
	 */
	private JsonObject prepareFormJsonCodeListByMetadata(String guiMetadata, JsonObject jsonObj, SvReader svr) throws SvException {
		JsonObject jsonGUI = null;
		JsonObject jsonreactGUI = null;
		DbSearchExpression expr = new DbSearchExpression();
		ArrayList<Object> listIDs = new ArrayList<>();
		ArrayList<String> listNames = new ArrayList<>();
		Gson gson = new Gson();
		JsonObject jsonObjRet = jsonObj;
		String idSetFiels = SN.EMPTY_STRING;
		String enumName = SN.EMPTY_STRING;
		try {
			jsonGUI = gson.fromJson(guiMetadata, JsonObject.class);
		} catch (Exception e) {
			debugException(e);
		}
		if (jsonGUI != null && jsonGUI.has(SN.REACT))
			jsonreactGUI = (JsonObject) jsonGUI.get(SN.REACT);
		if ((jsonreactGUI != null)) {
			if ((jsonreactGUI.has(SN.IDTABLE)) && (jsonreactGUI.has(SN.IDGETFIELD))){
				DbDataObject tableObject = SvCore.getDbtByName(jsonreactGUI.get(SN.IDTABLE).getAsString());
				// TODO replace try catch with joson.has
				try {
					DbSearchCriterion critU = new DbSearchCriterion(jsonreactGUI.get(SN.IDFIELD_LCASE).getAsString(),
							DbCompareOperand.EQUAL, jsonreactGUI.get(SN.IDVALUE_LCASE).getAsString());
					expr.addDbSearchItem(critU);
				} catch (Exception e) {
					debugException(e);
					expr = null;
				}
				DbDataArray vData = null;
				try {
					vData = svr.getObjects(expr, tableObject.getObjectId(), null, 0, 0);
				} catch (SvException e) {
					debugSvException(e);
				}
				if (jsonreactGUI.has(SN.IDSETFIELD)) {
					idSetFiels = jsonreactGUI.get(SN.IDSETFIELD).getAsString();
				}
				if (vData != null && !vData.getItems().isEmpty()) {
					for (DbDataObject item : vData.getItems()) {
						if (idSetFiels.equals(SN.EMPTY_STRING)) {
							listIDs.add(item.getObjectId());
							listNames.add(I18n.getText(getLocaleId(svr),
									item.getVal(jsonreactGUI.get(SN.IDGETFIELD).getAsString()).toString()));
						} else {
							if (jsonreactGUI.get(SN.IDGETFIELD).getAsString().equalsIgnoreCase(SN.LABEL_CODE)) {
								enumName = I18n.getText(getLocaleId(svr),
										item.getVal(jsonreactGUI.get(SN.IDGETFIELD).getAsString()).toString());
							} else {
								enumName = item.getVal(jsonreactGUI.get(SN.IDGETFIELD).getAsString()).toString();
							}
							if (!listNames.contains(enumName)) {
								listNames.add(enumName);
								switch (jsonObj.get(SN.TYPE_WITH_LOWERCASE).getAsString()) {
								case SN.INTEGER_LCASE:
								case SN.NUMBER_LCASE:
									listIDs.add(Long.valueOf(item.getVal(idSetFiels).toString()));
									break;
								default:
									listIDs.add(item.getVal(idSetFiels).toString());
								}
							}
						}
					}
					JsonArray arrLongJson = new JsonArray();
					JsonElement listIDsJson = gson.toJsonTree(listIDs);
					arrLongJson.add(listIDsJson);
					JsonElement listNamesJson = gson.toJsonTree(listNames);
					String tmpLeaf = jsonObj.toString();
					tmpLeaf = tmpLeaf.substring(0, tmpLeaf.length() - 1) + ",\"enum\":" + listIDsJson.toString()
							+ ",\"enumNames\":" + listNamesJson.toString() + '}';
					jsonObjRet = gson.fromJson(tmpLeaf, JsonObject.class);
				}
			} else if (jsonObj.has("title")) {
				String title = jsonObj.get("title").getAsString();
				switch (title) {
				case "custom_field.custom_recipients":
				case "custom_field.custom_cc":
				case "custom_field.custom_bcc":
				case "Recipients":
				case "Cc":
				case "Bcc":
					DbDataArray users = svr.getObjectsByTypeId(svCONST.OBJECT_TYPE_USER, null, 0, 0);
					if (!Objects.isNull(users)) {
						ArrayList<Long> customListIDs = new ArrayList<>();
						ArrayList<String> customListNames = new ArrayList<>();
						for (DbDataObject user : users.getItems()) {
							customListIDs.add(user.getObjectId());
							customListNames.add(user.getVal("E_MAIL").toString());
						}
						JsonElement customEnums = gson.toJsonTree(customListIDs);
						JsonElement customEnumNames = gson.toJsonTree(customListNames);
						String tmpLeaf = jsonObj.toString();
						tmpLeaf = tmpLeaf.substring(0, tmpLeaf.length() - 1) + ",\"enum\":" + customEnums.toString()
								+ ",\"enumNames\":" + customEnumNames.toString() + '}';
						jsonObjRet = gson.fromJson(tmpLeaf, JsonObject.class);
					}
					break;
				default:
					break;
				}
			}
		}
		return jsonObjRet;
	}

	/**
	 * procedure to produce string of list of codes from SVAROG_CODES with given ID.
	 * it looks like it has similar data as prepareJsonCodeList, but the point is
	 * that order of IDs and values must be preserved, we take that ID of the list
	 * is valid number, we check if field is number, string or Boolean and generate
	 * the list of IDs
	 * 
	 * @param tmpField DbDataObject field from SVAROG_FIELDS that we like to
	 *                 process, generate list of IDs/names
	 * @param jsonObj  JsonObject Json Object that contain the generated object
	 * @param svr      connected SvReader
	 * 
	 * @return JsonObject with added 2 extra lists, od same object if there was an
	 *         error
	 */
	private JsonObject prepareFormJsonCodeListByID(DbDataObject tmpField, JsonObject jsonObj, SvReader svr) {
		Long plistCodeId = (Long) tmpField.getVal(SN.CODE_LIST_ID);
		String fieldType = tmpField.getVal(SN.FIELD_TYPE).toString();
		ArrayList<Long> listIDNumber = new ArrayList<>();
		ArrayList<String> listIDString = new ArrayList<>();
		ArrayList<String> listNames = new ArrayList<>();
		ArrayList<Boolean> listBoolean = new ArrayList<>();
		ArrayList<String> sortList = new ArrayList<>();
		JsonArray arrNames = new JsonArray();
		Gson gson = new Gson();
		JsonObject jsonObjRet = jsonObj;
		HashMap<String, String> listMap = null;
		try (CodeList cl = new CodeList(svr)) {
			listMap = cl.getCodeList(getLocaleId(svr), plistCodeId, true);
			Iterator<Entry<String, String>> it = listMap.entrySet().iterator();
			while (it.hasNext()) {
				HashMap.Entry pair = it.next();
				sortList.add(pair.getValue().toString());
			}
		} catch (SvException e) {
			debugSvException(e);
		}
		if (listMap != null && listMap.size() > 0) {
			Iterator<Entry<String, String>> it = listMap.entrySet().iterator();
			for (String temp : sortList) {
				it = listMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, String> pair = it.next();
					if (temp.equalsIgnoreCase(pair.getValue())) {
						it.remove();
						if ("true".equalsIgnoreCase(pair.getKey())) {
							listBoolean.add(true);
						} else if ("false".equalsIgnoreCase(pair.getKey())) {
							listBoolean.add(false);
						} else
							try {
								if (fieldType.equals(SN.NUMERIC))
									listIDNumber.add(Long.parseLong(pair.getKey()));
								if (fieldType.equals(SN.NVARCHAR))
									listIDString.add(pair.getKey());
							} catch (Exception ex) {
								debugException(ex);

								listIDString.add(pair.getKey());
							}
						listNames.add(pair.getValue());
					}
				}
			}

			JsonElement listValueJson = null;
			if (!listNames.isEmpty()) {
				arrNames = new JsonArray();
				JsonElement listNamesJson = gson.toJsonTree(listNames);
				arrNames.add(listNamesJson);
				if (!listBoolean.isEmpty()) {
					listValueJson = gson.toJsonTree(listBoolean);
				}
				if (!listIDNumber.isEmpty()) {
					listValueJson = gson.toJsonTree(listIDNumber);
				}
				if (!listIDString.isEmpty()) {
					listValueJson = gson.toJsonTree(listIDString);
				}
				if (listValueJson != null) {
					String tmpLeaf = jsonObj.toString();
					tmpLeaf = tmpLeaf.substring(0, tmpLeaf.length() - 1) + ",\"enum\":" + listValueJson.toString()
							+ ",\"enumNames\":" + listNamesJson.toString() + '}';
					jsonObjRet = gson.fromJson(tmpLeaf, JsonObject.class);
				}
			}
		}

		return jsonObjRet;
	}

	/**
	 * procedure get the code-list for the field. depending on what we have for
	 * field we call it by LIST_ID or GUI_METADATA
	 * 
	 * @param tmpFiled DbDataObject field from SVAROG_FIELDS for which we generate
	 *                 code-list (drop-down)
	 * @param jsonObj  JsonObject object has all previous data for the field that we
	 *                 are processing, we just add some more to it and return it
	 * @param svr      connected SvReader
	 * 
	 * @return JsonObject with new added list
	 * @throws SvException 
	 */
	private JsonObject prepareFormJsonCodeList1(DbDataObject tmpFiled, JsonObject jsonObj, SvReader svr) throws SvException {
		// prepare the list from LIST_ID on the field, or from GUI_METADATA
		if (tmpFiled.getVal(SN.CODE_LIST_ID) != null && (long) tmpFiled.getVal(SN.CODE_LIST_ID) > 0)
			return prepareFormJsonCodeListByID(tmpFiled, jsonObj, svr);
		else if (tmpFiled.getVal(SN.GUI_METADATA) != null)
			return prepareFormJsonCodeListByMetadata(tmpFiled.getVal(SN.GUI_METADATA).toString(), jsonObj, svr);
		return jsonObj;
	}

	/**
	 * procedure to add grouping of fields when table is displayed in form/document
	 * for editing
	 * 
	 * 
	 * @param jFields JsonObject with fields for the entire table, we need this in
	 *                case group exist, so we add new values to it
	 * @param jLeaf   JsonObject for field that we are processing atm, we add tigs
	 *                to existing group and add some extra grouping stuff to it
	 * 
	 * @return JsonObject with added group for display in document/form
	 */
	private JsonObject prepareFormJsonGroup(DbDataObject tmpObject, String customFieldName, JsonObject jFields, JsonObject jLeaf) {
		Gson gson = new Gson();
		String tmpField = tmpObject.getVal(SN.FIELD_NAME).toString();
		if (customFieldName.equals(SN.EMPTY_STRING)) {
			tmpField = tmpObject.getVal(SN.FIELD_NAME).toString();
		} else {
			tmpField = customFieldName;
		}
		Boolean grouppathfound = false;
		JsonObject jsonreactGUI = null;
		String groupPath = null;
		JsonObject groupValues = new JsonObject();
		JsonObject groupProperties;
		JsonObject guiMetadata = null;
		try {
			if (tmpObject.getVal(SN.GUI_METADATA) != null)
				guiMetadata = gson.fromJson(tmpObject.getVal(SN.GUI_METADATA).toString(), JsonObject.class);
		} catch (Exception e) {
			debugException(e);
		}
		// try to load already existing group /part
		if (guiMetadata != null && guiMetadata.has(SN.REACT)) {
			jsonreactGUI = (JsonObject) guiMetadata.get(SN.REACT);
			if (jsonreactGUI != null && jsonreactGUI.has(SN.GROUPPATH)) {
				groupPath = jsonreactGUI.get(SN.GROUPPATH).getAsString();
			}
			if (groupPath != null) {
				if (jFields != null && jFields.has(groupPath)) {
					groupValues = (JsonObject) jFields.get(groupPath);
				}
				if ((groupValues != null) && (groupValues.has(SN.PROPERTIES))) {
					groupProperties = (JsonObject) groupValues.get(SN.PROPERTIES);
				} else {
					groupValues = new JsonObject();
					groupProperties = new JsonObject();
				}
				grouppathfound = true;
				groupValues.addProperty(SN.TYPE_WITH_LOWERCASE, SN.OBJECT);
				groupValues.addProperty(SN.TITLE_WITH_LOWERCASE, I18n.getText(SvConf.getDefaultLocale(), groupPath));
				groupProperties.add(tmpField, jLeaf);
				groupValues.add(SN.PROPERTIES, groupProperties);
			}
		}
		if (jFields != null) {
			if (grouppathfound) {
				jFields.add(groupPath, groupValues);
			} else {
				jFields.add(tmpField, jLeaf);
			}
		}
		return jFields;
	}

	private JsonObject prepareFormJsonDependentDropDown(DbDataObject tmpFiled, JsonObject jDependencies,
			JsonObject beforeFields, SvReader svr) {

		JsonObject jsonreactGUI = null;
		DbSearchExpression expr = new DbSearchExpression();
		ArrayList<Object> listIDs = new ArrayList<>();
		ArrayList<String> listNames = new ArrayList<>();
		Gson gson = new Gson();
		String idSetFiels = SN.EMPTY_STRING;

		jsonreactGUI = getReactGuiDataByField(tmpFiled);
		if (jsonreactGUI != null && jsonreactGUI.has(SN.IDTABLE) && jsonreactGUI.has(SN.IDGETFIELD)
				&& jsonreactGUI.has(SN.IDDEPENDENTFIELD) && jsonreactGUI.has(SN.IDGROUPFIELD)) {
			DbDataObject tableObject = SvCore.getDbtByName(jsonreactGUI.get(SN.IDTABLE).getAsString());
			// TODO replace try catch with joson.has
			try {
				DbSearchCriterion critU = new DbSearchCriterion(jsonreactGUI.get(SN.IDFIELD_LCASE).getAsString(),
						DbCompareOperand.EQUAL, jsonreactGUI.get(SN.IDVALUE_LCASE).getAsString());
				expr.addDbSearchItem(critU);
			} catch (Exception e) {
				debugException(e);
				expr = null;
			}
			DbDataArray vData = null;
			try {
				vData = svr.getObjects(expr, tableObject.getObjectId(), null, 0, 0);
			} catch (SvException e) {
				debugSvException(e);
			}
			if (jsonreactGUI.has(SN.IDSETFIELD)) {
				idSetFiels = jsonreactGUI.get(SN.IDSETFIELD).getAsString();
			}
			JsonArray oneof = new JsonArray();
			JsonObject anyof = new JsonObject();
			JsonObject anyObj = null;
			JsonObject root = null;
			JsonObject rootKey = null;
			JsonObject dependentObj = null;
			String enumName = SN.EMPTY_STRING;
			if (vData != null && !vData.isEmpty()) {
				String[] groupColumn = new String[1];
				groupColumn[0] = jsonreactGUI.get(SN.IDGROUPFIELD).getAsString();
				HashMap<String, DbDataArray> groupItem = vData.groupItemsByColumn(groupColumn);
				for (Entry<String, DbDataArray> entry : groupItem.entrySet()) {
					listIDs = new ArrayList<>();
					listNames = new ArrayList<>();
					anyObj = new JsonObject();
					root = new JsonObject();
					rootKey = new JsonObject();

					if (beforeFields.has(jsonreactGUI.get(SN.IDDEPENDENTFIELD).getAsString())) {
						JsonObject denendentField = beforeFields
								.get(jsonreactGUI.get(SN.IDDEPENDENTFIELD).getAsString()).getAsJsonObject();
						switch (denendentField.get(SN.TYPE_WITH_LOWERCASE).getAsString()) {
						case SN.INTEGER_LCASE:
						case SN.NUMBER_LCASE:
							rootKey.addProperty(SN.CONST_LCASE, Long.valueOf(entry.getKey()));
							break;
						default:
							rootKey.addProperty(SN.CONST_LCASE, entry.getKey());
						}
					} else {
						rootKey.addProperty(SN.CONST_LCASE, entry.getKey());
					}
					for (DbDataObject item : entry.getValue().getItems()) {
						dependentObj = new JsonObject();
						dependentObj = addFieldTypeToJsonObject(tmpFiled, dependentObj, true);
						dependentObj.addProperty(SN.TITLE_WITH_LOWERCASE,
								I18n.getText(getLocaleId(svr), tmpFiled.getVal(SN.LABEL_CODE).toString()));
						if (idSetFiels.equals(SN.EMPTY_STRING)) {
							listIDs.add(item.getObjectId());
							listNames.add(I18n.getText(getLocaleId(svr),
									item.getVal(jsonreactGUI.get(SN.IDGETFIELD).getAsString()).toString()));
						} else {
							if (jsonreactGUI.get(SN.IDGETFIELD).getAsString().equalsIgnoreCase(SN.LABEL_CODE)) {
								enumName = I18n.getText(getLocaleId(svr),
										item.getVal(jsonreactGUI.get(SN.IDGETFIELD).getAsString()).toString());
							} else {
								enumName = item.getVal(jsonreactGUI.get(SN.IDGETFIELD).getAsString()).toString();
							}
							if (!listNames.contains(enumName)) {
								listNames.add(enumName);
								switch (dependentObj.get(SN.TYPE_WITH_LOWERCASE).getAsString()) {
								case SN.INTEGER_LCASE:
								case SN.NUMBER_LCASE:
									listIDs.add(Long.valueOf(item.getVal(idSetFiels).toString()));
									break;
								default:
									listIDs.add(item.getVal(idSetFiels).toString());
								}
							}
						}
					}
					anyObj.add(jsonreactGUI.get(SN.IDDEPENDENTFIELD).getAsString(), rootKey);
					JsonElement listIDsJson = gson.toJsonTree(listIDs);
					JsonElement listNamesJson = gson.toJsonTree(listNames);
					String tmpLeaf = dependentObj.toString();
					tmpLeaf = tmpLeaf.substring(0, tmpLeaf.length() - 1) + ",\"enum\":" + listIDsJson.toString()
							+ ",\"enumNames\":" + listNamesJson.toString() + '}';
					dependentObj = gson.fromJson(tmpLeaf, JsonObject.class);
					anyObj.add(tmpFiled.getVal(SN.FIELD_NAME).toString(), dependentObj);
					root.add("properties", anyObj);
					oneof.add(root);
				}
				anyof.add("oneOf", oneof);
				jDependencies.add(jsonreactGUI.get(SN.IDDEPENDENTFIELD).getAsString(), anyof);
			}
		}
		return jDependencies;
	}
	
	/**
	 * Web service that create new message (both initial and reply)
	 * 
	 * @param sessionId
	 * @param formVals
	 * @return
	 */
	@Path("/createNewMessage/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("application/json")
	public Response createNewMessage(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = new Reader();
		Writer wr = new Writer();
		try (SvReader svr = new SvReader(sessionId); SvWriter svw = new SvWriter(svr)) {
			Long subjectObjId = 0L;
			String subjectModuleName = SN.EMPTY_STRING;
			String subjectTitle = SN.EMPTY_STRING;
			String subjectPriority = SN.EMPTY_STRING;
			String subjectCategory = SN.EMPTY_STRING;
			String msgText = SN.EMPTY_STRING;
			String msgPriority = SN.EMPTY_STRING;
			String msgTo = SN.EMPTY_STRING;
			String msgCc = SN.EMPTY_STRING;
			String msgBcc = SN.EMPTY_STRING;
			String msgAttachment = SN.EMPTY_STRING;
			
			if (formVals.containsKey(SN.SUBJECT_OBJ_ID)) {
				subjectObjId = Long.valueOf(formVals.get(SN.SUBJECT_OBJ_ID).get(0));
			}
			if (formVals.containsKey(SN.MODULE_NAME)) {
				subjectModuleName = formVals.get(SN.MODULE_NAME).get(0);
			}
			if (formVals.containsKey(SN.TITLE)) {
				subjectTitle = formVals.get(SN.TITLE).get(0);
			}
			if (formVals.containsKey(SN.PRIORITY)) {
				subjectPriority = formVals.get(SN.PRIORITY).get(0);
				msgPriority = subjectPriority;
			}
			if (formVals.containsKey(SN.CATEGORY)) {
				subjectCategory = formVals.get(SN.CATEGORY).get(0);
			}
			if (formVals.containsKey(SN.TEXT)) {
				msgText = formVals.get(SN.TEXT).get(0);
			}
			if (formVals.containsKey(SN.CUSTOM_RECIPIENTS)) {
				msgTo = formVals.get(SN.CUSTOM_RECIPIENTS).get(0);
			}
			if (formVals.containsKey(SN.CUSTOM_CC)) {
				msgCc = formVals.get(SN.CUSTOM_CC).get(0);
			}
			if (formVals.containsKey(SN.CUSTOM_BCC)) {
				msgBcc = formVals.get(SN.CUSTOM_BCC).get(0);
			}
			if (formVals.containsKey(SN.MSG_ATTACHMENT)) {
				msgAttachment = formVals.get(SN.MSG_ATTACHMENT).get(0);
			}
			
			if (!subjectModuleName.equals(SN.EMPTY_STRING) && !subjectModuleName.trim().equals(SN.EMPTY_STRING)
					&& !subjectTitle.equals(SN.EMPTY_STRING) && !subjectTitle.trim().equals(SN.EMPTY_STRING)
					&& !subjectPriority.equals(SN.EMPTY_STRING) && !subjectPriority.trim().equals(SN.EMPTY_STRING)
					&& !subjectCategory.equals(SN.EMPTY_STRING) && !subjectCategory.trim().equals(SN.EMPTY_STRING)
					&& !msgText.equals(SN.EMPTY_STRING) && !msgText.trim().equals(SN.EMPTY_STRING)
					&& !msgTo.equals(SN.EMPTY_STRING) && !msgTo.trim().equals(SN.EMPTY_STRING)) {
				
				Subject subject = new Subject(subjectModuleName, subjectTitle, subjectCategory, subjectPriority);
				// subject section
				DbDataObject dboSubject = null;
				if (subjectObjId == 0L) {
					dboSubject = subject.createSubject();
					svw.saveObject(dboSubject, false);
					if (dboSubject != null) {
						svw.dbCommit();
					}
				} else {
					dboSubject = svr.getObjectById(subjectObjId, SvReader.getTypeIdByName(SN.SUBJECT), null);
				}
				// message section
				Message message = new Message(msgText, SvReader.getUserBySession(svw.getSessionId()).getObjectId(),
						SvReader.getUserBySession(svw.getSessionId()).getVal(SN.USER_NAME).toString(), msgPriority);
				DbDataObject dboMessage = message.createMessage(dboSubject);
				svw.saveObject(dboMessage, false);
				if (dboMessage != null) {
					svw.dbCommit();
					wr.createLinkBetweenMessageAndUser(dboMessage, SN.MSG_TO, rdr.convertStringIntoLongList(msgTo), svw,
							svr);
					wr.createLinkBetweenMessageAndUser(dboMessage, SN.MSG_CC, rdr.convertStringIntoLongList(msgCc), svw,
							svr);
					wr.createLinkBetweenMessageAndUser(dboMessage, SN.MSG_BCC, rdr.convertStringIntoLongList(msgBcc),
							svw, svr);
				}
				// attachment
				DbDataObject dboAttch = wr.processMessageAttachmentInfo(msgAttachment, dboMessage, svw, svr);
				if (dboAttch != null) {
					svw.dbCommit();
				}
				jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_CREATED_MESSAGE),
						I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_CREATED_MESSAGE), new JsonObject());
			} else {
				throw new SvException(SN.svarog_notifications_ERROR_MANDATORY_FIELDS_ARE_MISSING, svw.getInstanceUser());
			}
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_CREATE_NEW_MESSAGE);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	/**
	 * Web service that searches subjects by multiple criteria
	 * (title/category/priority/message text). Subject can be searched by
	 * multiple criteria at once
	 * 
	 * @param sessionId
	 * @param formVals
	 * @return
	 */
	@Path("/searchSubjects/{sessionId}")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchSubjects(@PathParam("sessionId") String sessionId, MultivaluedMap<String, String> formVals) {
		ResponseHandler jrh = new ResponseHandler();
		DbDataArray dbArrSubjects = null;
		Reader rdr = null;
		Writer wr = null;
		DbDataArray dbArrFinal = null;
		JsonArray jArr = null;
		JsonObject jObj = null;
		try (SvReader svr = new SvReader(sessionId)) {
			dbArrSubjects = new DbDataArray();
			rdr = new Reader();
			wr = new Writer();
			dbArrFinal = new DbDataArray();
			jArr = new JsonArray();
			jObj = new JsonObject();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);

			if (dboUser != null) {
				String subjectTitleVal = formVals.get(SN.SUBJECT_TITLE).get(0);
				String subjectCategoryVal = formVals.get(SN.SUBJECT_CATEGORY).get(0);
				String subjectPriorityVal = formVals.get(SN.SUBJECT_PRIORITY).get(0);
				String messageText = formVals.get(SN.MSG_TEXT).get(0);

				dbArrSubjects = rdr.getSubjectsBySubjectAndMessageCriteria(dboUser, subjectTitleVal, subjectCategoryVal,
						subjectPriorityVal, messageText, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jArr = rdr.sortAndPrepareCustomJsonArray(dbArrFinal, jObj, SN.SUBJECT, wr);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECTS),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECTS));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_NO_SUBJECTS_HAVE_BEEN_FOUND);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}

	/**
	 * Web service that return sent subjects
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getSentSubjects/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSentSubjects(@PathParam("sessionId") String sessionId) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		DbDataArray dbArrSubjects = null;
		DbDataArray dbArrFinal = null;
		JsonArray jArr = null;
		JsonObject jObj = null;
		Writer wr = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			dbArrSubjects = new DbDataArray();
			dbArrFinal = new DbDataArray();
			jArr = new JsonArray();
			jObj = new JsonObject();
			wr = new Writer();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrSubjects = rdr.getSentOrArchiveSubjects(dboUser, SN.VALID, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jArr = rdr.sortAndPrepareCustomJsonArray(dbArrFinal, jObj, SN.SUBJECT, wr);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECTS),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECTS));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_SENT_SUBJECTS);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}

	/**
	 * Web service that return archived subjects
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getArchivedSubjects/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getArchivedSubjects(@PathParam("sessionId") String sessionId) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		DbDataArray dbArrSubjects = null;
		DbDataArray dbArrFinal = null;
		JsonObject jObj = null;
		JsonArray jArr = null;
		Writer wr = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			dbArrSubjects = new DbDataArray();
			dbArrFinal = new DbDataArray();
			jObj = new JsonObject();
			jArr = new JsonArray();
			wr = new Writer();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrSubjects = rdr.getSentOrArchiveSubjects(dboUser, SN.CLOSED, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jArr = rdr.sortAndPrepareCustomJsonArray(dbArrFinal, jObj, SN.SUBJECT, wr);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_ARCHIVED_SUBJECTS),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_ARCHIVED_SUBJECTS));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_ARCHIVED_SUBJECTS);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}

	/**
	 * Web service that return subjects in inbox
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getInboxSubjects/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInboxSubjects(@PathParam("sessionId") String sessionId) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = new Reader();
		DbDataArray dbArrMessages = null;
		DbDataArray dbArrSubjects = null;
		DbDataArray dbArrFinal = null;
		JsonObject jObj = null;
		JsonArray jArr = null;
		Writer wr = new Writer();
		try (SvReader svr = new SvReader(sessionId)) {
			jObj = new JsonObject();
			jArr = new JsonArray();
			dbArrMessages = new DbDataArray();
			dbArrSubjects = new DbDataArray();
			dbArrFinal = new DbDataArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrMessages = rdr.getMessagesByCriteria(dboUser, svr);
				dbArrSubjects = rdr.getSubjectsInArray(dbArrMessages, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jArr = rdr.sortAndPrepareCustomJsonArray(dbArrFinal, jObj, SN.SUBJECT, wr);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSULLY_GET_INBOX_SUBJECTS),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSULLY_GET_INBOX_SUBJECTS));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_INBOX_SUBJECTS);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}

	/**
	 * Web service that change status of subject to {statusTo}.
	 * 
	 * @param sessionId
	 * @param subjectObjId
	 * @return
	 */
	@Path("/changeSubjectStatus/{sessionId}/{subjectObjId}/{statusFrom}/{statusTo}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeSubjectStatus(@PathParam("sessionId") String sessionId,
			@PathParam("subjectObjId") Long subjectObjId, @PathParam("statusFrom") String statusFrom,
			@PathParam("statusTo") String statusTo) {
		ResponseHandler jrh = new ResponseHandler();
		Writer wr = null;
		try (SvReader svr = new SvReader(sessionId); SvWriter svw = new SvWriter(svr)) {
			List<String> validStatuses = Arrays.asList(SN.VALID, SN.CLOSED);
			if (validStatuses.contains(statusFrom) && validStatuses.contains(statusTo)
					&& !statusFrom.equals(statusTo)) {
				wr = new Writer();
				DbDataObject dboUser = SvReader.getUserBySession(sessionId);
				DbDataObject dboSubject = svr.getObjectById(subjectObjId, SvReader.getTypeIdByName(SN.SUBJECT), null);
				if (dboUser != null && dboSubject != null && dboSubject.getStatus().equals(statusFrom)) {
					dboSubject.setStatus(statusTo);
					svw.saveObject(dboSubject);
					svw.dbCommit();
					// sent automatic message only if archive subject
					if (statusFrom.equals(SN.VALID) && statusTo.equals(SN.CLOSED)) {
						wr.sendAutomaticMessage(dboSubject, svw, svr);
					}
				}
				jrh.create(MessageType.SUCCESS,
						I18n.getText(SN.svarog_notifications_SUCCESS_SUCESSFULLY_CHANGED_SUBJECT_STATUS),
						I18n.getText(SN.svarog_notifications_SUCCESS_SUCESSFULLY_CHANGED_SUBJECT_STATUS), new JsonArray());
			} else {
				jrh.create(MessageType.ERROR, I18n.getText(SN.svarog_notifications_ERROR_INVALID_STATUS_VALUES),
						I18n.getText(SN.svarog_notifications_ERROR_INVALID_STATUS_VALUES));
			}
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_CHANGE_SUBJECT_STATUS);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}
	
	/**
	 * Web service that change status of subject to VALID. Automatic message is
	 * sent to all users in that subjects when the status is changed to CLOSED
	 * 
	 * @param sessionId
	 * @param subjectObjId
	 * @return
	 */
	@Path("/archiveSubject/{sessionId}/{subjectObjId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response archiveSubject(@PathParam("sessionId") String sessionId,
			@PathParam("subjectObjId") Long subjectObjId) {
		return changeSubjectStatus(sessionId, subjectObjId, SN.VALID, SN.CLOSED);
	}

	/**
	 * Web service that change status of subject from CLOSED to VALID
	 * 
	 * @param sessionId
	 * @param subjectObjId
	 * @return
	 */
	@Path("/unArchiveSubject/{sessionId}/{subjectObjId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response unArchiveSubject(@PathParam("sessionId") String sessionId,
			@PathParam("subjectObjId") Long subjectObjId) {
		return changeSubjectStatus(sessionId, subjectObjId, SN.CLOSED, SN.VALID);
	}

	/**
	 * Web service that updates the status of link between message and user to
	 * VALID
	 * 
	 * @param sessionId
	 * @param messageObjId
	 * @return
	 */
	@Path("/updateStatusOfLinkBetweenMessageAndUser/{sessionId}/{messageObjId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateStatusOfLinkBetweenMessageAndUser(@PathParam("sessionId") String sessionId,
			@PathParam("messageObjId") Long messageObjId) {
		ResponseHandler jrh = new ResponseHandler();
		String result = SN.svarog_notifications_INFO_FAILED_TO_CHANGE_STATUS_OF_THE_LINK;
		Writer wr = null;
		try (SvReader svr = new SvReader(sessionId); SvWriter svw = new SvWriter(svr)) {
			wr = new Writer();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null && messageObjId != null
					&& wr.updateStatusOfLinkBetweenMessageAndUser(dboUser, messageObjId, svw, svr)) {
				result = SN.svarog_notifications_SUCCESS_SUCCESSFULLY_UPDATED_STATUS_OF_LINK;
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(result), I18n.getText(result), new JsonArray());
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_CHANGE_STATUS_OF_LINK);
		} 
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	/**
	 * Web service that return the number of unread messages per user
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getNumberOfUnreadMessagesPerUser/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getNumberOfUnreadMessagesPerUser(@PathParam("sessionId") String sessionId) {
		String result = SN.EMPTY_STRING;
		Reader rdr = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				result = String.valueOf(rdr.getNumberOfUnreadMessagesPerUser(dboUser, svr));
			}
		} catch (Exception e) {
			log4j.error("General error in processing getNumberOfUnreadMessagesPerUser:", e);
		}
		return Response.status(200).entity(result).build();
	}
	
	/**
	 * Method that return number of unread inbox & archived subjects per user
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getNumberOfUnreadInboxAndArchivedMessagesPerUser/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNumberOfUnreadInboxAndArchivedMessagesPerUser(@PathParam("sessionId") String sessionId) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		DbDataArray dbArrUnreadMessages = null;
		int counterValid = 0;
		int counterClosed = 0;
		JsonObject jsonObject = null;
		try (SvReader svr = new SvReader(sessionId)){
			rdr = new Reader();
			jsonObject = new JsonObject();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrUnreadMessages = rdr.getListOfUnreadMessagesPerUser(dboUser, svr);
				if (dbArrUnreadMessages != null && !dbArrUnreadMessages.getItems().isEmpty()) {
					for (DbDataObject dboTemp : dbArrUnreadMessages.getItems()) {
						DbDataObject dboMessage = svr.getObjectById((Long) dboTemp.getVal(SN.LINK_OBJ_ID_1),
								SvReader.getTypeIdByName(SN.MESSAGE), null);
						DbDataObject dboSubject = svr.getObjectById(dboMessage.getParentId(),
								SvReader.getTypeIdByName(SN.SUBJECT), null);
						switch (dboSubject.getStatus()) {
						case SN.VALID:
							counterValid++;
							break;
						case SN.CLOSED:
							counterClosed++;
							break;
						default:
							break;
						}
					}
				}
				jsonObject.addProperty(SN.NUMBER_OF_UNREAD_SUBJECTS_INBOX, counterValid);
				jsonObject.addProperty(SN.NUMBER_OF_UNREAD_SUBJECTS_ARCHIVED, counterClosed);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA), jsonObject);
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_DATA);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	/**
	 * Web service that return org unit per org unit type
	 * 
	 * @param sessionId
	 * @param orgUnitType
	 *            Org unit type (REGIONAL_OFFICE/MINICIPALITY_OFFICE)
	 * @return
	 */
	@Path("/getOrgUnitPerOrgUnitType/{sessionId}/{orgUnitType}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrgUnitPerOrgUnitType(@PathParam("sessionId") String sessionId,
			@PathParam("orgUnitType") String orgUnitType) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		DbDataArray dbArrOrgUnits = null;
		try (SvSecurity svs = new SvSecurity(); SvReader svr = new SvReader(svs)){
			svs.switchUser(svCONST.serviceUser);
			rdr = new Reader();
			dbArrOrgUnits = new DbDataArray();
			dbArrOrgUnits = rdr.searchForOrgUnit(orgUnitType, svr);
			if (dbArrOrgUnits != null && !dbArrOrgUnits.getItems().isEmpty()) {
				//jarr = rdr.convertDbDataArrayToGridJson(dbArrOrgUnits, SN.SVAROG_ORG_UNITS, false, SN.PKID, SN.DESC,
				//		svr);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_DATA);
		} 
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}
	
	// NOT IN USE FOR NOW
	@Path("/getInboxMessages/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInboxMessages(@PathParam("sessionId") String sessionId) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = new Reader();
		DbDataArray finalMessageList = new DbDataArray();
		DbDataArray directMessages = new DbDataArray();
		try (SvReader svr = new SvReader(sessionId)){
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				// get directly assigned messages
				DbSearchCriterion cr1 = new DbSearchCriterion(SN.ASSIGNED_TO, DbCompareOperand.EQUAL,
						dboUser.getObjectId());
				DbSearchCriterion cr2 = new DbSearchCriterion(SN.STATUS, DbCompareOperand.EQUAL, SN.VALID);
				directMessages = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
						SvReader.getTypeIdByName(SN.MESSAGE), null, 0, 0);
				if (!directMessages.getItems().isEmpty()) {
					finalMessageList = directMessages;
				}
				// get messages through link
				rdr.getInboxMessagesThroughLink(dboUser, finalMessageList, svr);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA), finalMessageList.toSimpleJson());
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_INBOX_MESSAGES);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	@Path("/getAdditionalMessageInfo/{sessionId}/{msgObjId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdditionalMessageInfo(@PathParam("sessionId") String sessionId,
			@PathParam("msgObjId") Long msgObjId) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		JsonObject json = null;
		DbDataArray dbArrMsgTo = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			json = new JsonObject();
			dbArrMsgTo = new DbDataArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataObject dboMessage = svr.getObjectById(msgObjId, SvReader.getTypeIdByName(SN.MESSAGE), null);
			if (dboUser != null && dboMessage != null) {
				if (rdr.checkIfLinkExists(dboMessage, dboUser, SN.MSG_TO, null, svr)
						|| rdr.checkIfLinkExists(dboMessage, dboUser, SN.MSG_BCC, null, svr)
						|| dboMessage.getVal(SN.CREATED_BY).equals(dboUser.getObjectId())
						|| (dboMessage.getVal(SN.ASSIGNED_TO) != null
								&& dboMessage.getVal(SN.ASSIGNED_TO).equals(dboUser.getObjectId()))) {
					dbArrMsgTo = rdr.getUsersLinkedToMessage(dboMessage, svr);
					json.add(SN.MSG_TO, dbArrMsgTo.toSimpleJson());
					json.add(SN.MSG_CC, rdr.getUsersLinkedToMessage(dboMessage, SN.MSG_CC, svr).toSimpleJson());
					json.add(SN.MSG_BCC, rdr.getUsersLinkedToMessage(dboMessage, SN.MSG_BCC, svr).toSimpleJson());
				}
				if (rdr.checkIfLinkExists(dboMessage, dboUser, SN.MSG_CC, null, svr)) {
					dbArrMsgTo = rdr.getUsersLinkedToMessage(dboMessage, svr);
					json.add(SN.MSG_TO, dbArrMsgTo.toSimpleJson());
				}
				json.addProperty(SN.DATE_OF_CREATION, dboMessage.getDtInsert().toString());
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA), json);
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_DATA);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	@Path("/changeMessageStatus/{sessionId}/{msgObjId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeMessageStatus(@PathParam("sessionId") String sessionId,
			@PathParam("msgObjId") Long msgObjId) {
		ResponseHandler jrh = new ResponseHandler();
		String result = SN.svarog_notifications_ERROR_FAILED_TO_CHANGE_MESSAGE_STATUS;
		try (SvReader svr = new SvReader(sessionId); SvWriter svw = new SvWriter(svr)){
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataObject dboMessage = svr.getObjectById(msgObjId, SvReader.getTypeIdByName(SN.MESSAGE), null);
			if (dboUser != null && dboMessage != null && dboMessage.getStatus().equals(SN.VALID)) {
				dboMessage.setStatus(SN.CLOSED);
				svw.saveObject(dboMessage);
				svw.dbCommit();
				result = SN.svarog_notifications_SUCCESS_SUCCESSFULLY_CHANGED_MESSAGE_STATUS;
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(result), I18n.getText(result), new JsonArray());
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_CHANGE_MESSAGE_STATUS);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}

	@Path("/getMessages/{sessionId}/{status}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMessagesByStatus(@PathParam("sessionId") String sessionId, @PathParam ("status") String status) {
		ResponseHandler jrh = new ResponseHandler();
		DbDataArray dbArrMessages = null;
		try (SvReader svr = new SvReader(sessionId)){
			dbArrMessages = new DbDataArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				DbSearchExpression exp = new DbSearchExpression();
				DbSearchCriterion cr2 = new DbSearchCriterion(SN.STATUS, DbCompareOperand.EQUAL, status);
				if (status.equals(SN.VALID)) {
					DbSearchCriterion cr1 = new DbSearchCriterion(SN.CREATED_BY, DbCompareOperand.EQUAL,
						dboUser.getObjectId());
					exp.addDbSearchItem(cr1);
				}
				exp.addDbSearchItem(cr2);
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA), dbArrMessages.toSimpleJson());
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_DATA);
		}
		return Response.status(200).entity(jrh.getAll().toString()).build();
	}
	
	@Path("/getArchivedMessages/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getArchivedMessages(@PathParam("sessionId") String sessionId) {
		return getMessagesByStatus(sessionId, SN.CLOSED);
	}
	
	
	@Path("/getSentMessages/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSentMessages(@PathParam("sessionId") String sessionId) {
		return getMessagesByStatus(sessionId, SN.VALID);
	}
	
	/**
	 * Web service to get the schema for react UI , UI schema to be stored in
	 * SVAROG_FIELDS , field GUI_METADATA sub_object "react" , sub_object
	 * "uischema", it will just read the full object as it is and add it to
	 * return string with the same field name to be paired to the object
	 * returned by getTableJSONSchema WS
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param table_name
	 *            String table name for which we want to insert new element
	 *            (record)
	 * 
	 * @return Json string with UI json for all fields in the table
	 */
	@Path("/getTableUISchema/{sessionId}/{table_name}")
	@GET
	@Produces("application/json")
	public Response getTableUISchema(@PathParam("sessionId") String sessionId,
			@PathParam("table_name") String tableName, @Context HttpServletRequest httpRequest) {
		JsonObject jsonData = new JsonObject();
		Gson gson = new Gson();
		try (SvReader svr = new SvReader(sessionId);) {
			DbDataArray allFields = new DbDataArray();
			if (tableName != null) {
				Long tableID = findTableType(tableName);
				DbDataArray typetoGet = svr.getObjectsByParentId(tableID, svCONST.OBJECT_TYPE_FIELD, null, 0, 0,
						SN.SORT_ORDER);
				// case for MESSAGE + custom_fields
				switch (tableName) {
				case SN.MESSAGE:
					// custom_fields
					DbDataArray customFields = getCustomFields(tableName, svr);
					for (DbDataObject field : customFields.getItems()) {
						allFields.addDataItem(field);
					}
					// SUBJECT
					DbDataArray subjectTableFields = svr.getObjectsByParentId(
							SvCore.getDbtByName(SN.SUBJECT).getObjectId(), svCONST.OBJECT_TYPE_FIELD, null, 0, 0,
							SN.SORT_ORDER);
					for (DbDataObject field : subjectTableFields.getItems()) {
						allFields.addDataItem(field);
					}
					// MESSAGE
					for (DbDataObject messageFieldsDataObject : typetoGet.getItems()) {
						allFields.addDataItem(messageFieldsDataObject);
					}
					// custom_attachment
					allFields.addDataItem(getCustomField("ATTACHMENT"));
					break;
				default:
					allFields = typetoGet;
					break;
				}

				// case for add classNames by tableName
				if (tableName.equals(SN.MESSAGE)) {
					jsonData.addProperty(SN.CLASS_NAMES, "title-message-form");
				}
				for (int i = 0; i < allFields.getItems().size(); i++) {
					JsonObject jsonreactGUI = null;
					JsonObject jsonObj = null;
					JsonObject jsonUISchema = null;
					DbDataObject tempDboField = allFields.getItems().get(i);
					String tmpField = tempDboField.getVal(SN.FIELD_NAME).toString();
					if (tmpField.equals(SN.PRIORITY)
							&& tempDboField.getVal(SN.LABEL_CODE).toString().equals("message.priority")) {
						tmpField = "MESSAGE_PRIORITY";
					}
					if (processField(tmpField)) {
						if (tempDboField.getVal(SN.GUI_METADATA) != null) {
							jsonObj = gson.fromJson(tempDboField.getVal(SN.GUI_METADATA).toString(), JsonObject.class);
						}
						if (jsonObj != null && jsonObj.has(SN.REACT)) {
							jsonreactGUI = (JsonObject) jsonObj.get(SN.REACT);
						}
						if (jsonreactGUI != null && jsonreactGUI.has(SN.UISCHEMA)) {
							jsonUISchema = (JsonObject) jsonreactGUI.get(SN.UISCHEMA);
						}
						// case: enable code execution if grouppath exists
						if (jsonUISchema != null || jsonreactGUI.has(SN.GROUPPATH)) {
							String groupPath = null;
							if (jsonreactGUI != null && jsonreactGUI.has(SN.GROUPPATH)) { // grouppath
																							// found
								groupPath = jsonreactGUI.get(SN.GROUPPATH).getAsString();
								// cases for adding parameter 'classNames' by
								// grouppath value
								String className = SN.EMPTY_STRING;
								switch (groupPath) {
								case "message.custom_info":
									className = "message-users-info";
									break;
								case "subject.basic_info":
									className = "subject-form";
									break;
								case "message.basic_info":
									className = "new-message-form";
									break;
								case "subject.title":
									className = "subject-title";
									break;
								case "attachment.custom_info":
									className = "attachment-info";
									break;
								default:
									break;
								}

								JsonObject groupValues = null;
								if (jsonData.has(groupPath)) {
									groupValues = (JsonObject) jsonData.get(groupPath);
								}
								if (groupValues == null) {
									groupValues = new JsonObject();
								}
								if (!className.equals(SN.EMPTY_STRING)) {
									groupValues.addProperty(SN.CLASS_NAMES, className);
								}
								// case because of the previous case
								if (!Objects.isNull(jsonUISchema)) {
									groupValues.add(tmpField, jsonUISchema);
								}

								jsonData.add(groupPath, groupValues);
							} else { // no grouppath found
								// case for field has_atachment
								if (tmpField.equals(SN.HAS_ATTACHMENT) && tableName.equals(SN.MESSAGE)) {
									jsonUISchema.addProperty(SN.CLASS_NAMES, "attach-form");
								}
								jsonData.add(tmpField, jsonUISchema);
							}
						}
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(401).entity(e.getFormattedMessage()).build();
		}
		return Response.status(200).entity(jsonData.toString()).build();
	}
	
	/**
	 * Web service for adding new record in a table , return is JSON
	 * react-jsonschema-form compatible string
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param table_name
	 *            String table name for which we want to insert new element
	 *            (record)
	 * 
	 * @return Json string with all fields ( field names pulled from database)
	 *         and drop-down values translated
	 */
	@Path("/getTableJSONSchema/{sessionId}/{table_name}")
	@GET
	@Produces("application/json")
	public Response getTableJSONSchema(@PathParam("sessionId") String sessionId,
			@PathParam("table_name") String tableName, @Context HttpServletRequest httpRequest) {
		JsonObject jData = new JsonObject();
		Gson gson = new Gson();
		try (SvReader svr = new SvReader(sessionId); SvParameter svp = new SvParameter(svr)){
			ArrayList<String> listRequired = new ArrayList<>();
			HashMap<String, String> listRequiredWithLink = new HashMap<>();
			Set<String> set1 = new LinkedHashSet<>();
			DbDataArray allFields = new DbDataArray();
			DbDataObject tableObject = SvCore.getDbtByName(tableName);
			DbDataArray dboFieldsPerTable = svr.getObjectsByParentId(tableObject.getObjectId(),
					svCONST.OBJECT_TYPE_FIELD, null, 0, 0, SN.SORT_ORDER);
			// case for custom_fields + SUBJECT + MESSAGE
			switch (tableName) {
			case SN.MESSAGE:
				// custom_fields
				DbDataArray customFields = getCustomFields(tableName, svr);
				for (DbDataObject field : customFields.getItems()) {
					allFields.addDataItem(field);
				}
				// SUBJECT
				DbDataArray subjectTableFields = svr.getObjectsByParentId(SvCore.getDbtByName(SN.SUBJECT).getObjectId(),
						svCONST.OBJECT_TYPE_FIELD, null, 0, 0, SN.SORT_ORDER);
				for (DbDataObject field : subjectTableFields.getItems()) {
					allFields.addDataItem(field);
				}
				// MESSAGE
				for (DbDataObject messageFieldsDataObject : dboFieldsPerTable.getItems()) {
					allFields.addDataItem(messageFieldsDataObject);
				}
				break;
			default:
				allFields = dboFieldsPerTable;
				break;
			}
			
			jData.addProperty(SN.TITLE_WITH_LOWERCASE, I18n.getText(getLocaleId(svr), tableObject.getVal(SN.LABEL_CODE).toString()));
			jData.addProperty(SN.TYPE_WITH_LOWERCASE, SN.OBJECT);
			JsonObject jFields = new JsonObject();
			JsonObject jDependencies = new JsonObject();
			for (DbDataObject tempDboField : allFields.getItems()) {
				String tmpField = tempDboField.getVal(SN.FIELD_NAME).toString();
				// cases for custom field name; key
				String customFieldName = SN.EMPTY_STRING;
				if (tmpField.equals(SN.PRIORITY)
						&& tempDboField.getVal(SN.LABEL_CODE).toString().equals("message.priority")) {
					customFieldName = "MESSAGE_PRIORITY";
				}
				
				if (processField(tmpField)) {
					JsonObject jsonreactGUI = getReactGuiDataByField(tempDboField);
					JsonObject jLeaf = new JsonObject();
					// make a list and set of required fields, list of groups
					// (paths) and list of combination of those 2
					if (("false").equalsIgnoreCase(tempDboField.getVal(SN.IS_NULL).toString())) {
						//	|| moreMandatoryFields.contains(tempDboField.getVal(SN.FIELD_NAME))
						if (tempDboField.getVal(SN.REFERENTIAL_TABLE) == null) {
							if (!customFieldName.equals(SN.EMPTY_STRING)) {
								listRequired.add(customFieldName);
							} else {
								listRequired.add(tmpField);
							}
							if (jsonreactGUI != null && jsonreactGUI.has(SN.GROUPPATH)) {
								String pathString = jsonreactGUI.get(SN.GROUPPATH).getAsString();
								set1.add(pathString);
								listRequiredWithLink.put(tmpField, pathString);
							}
						}
					}

					Boolean createField = true;
					if (jsonreactGUI != null && jsonreactGUI.has(SN.IDDEPENDENTFIELD)) {
						createField = false;
					}

					if (createField) {
						// cases for field title
						String fieldTitle = SN.EMPTY_STRING;
						switch (tableName) {
						case SN.MESSAGE:
							if (tmpField.equals(SN.TITLE)) {
								fieldTitle = I18n.getText(getLocaleId(svr), "message.title_subject");
							} else if (tmpField.equals(SN.TEXT)) {
								fieldTitle = I18n.getText(getLocaleId(svr), "message.text_message");
							} else {
								fieldTitle = I18n.getText(getLocaleId(svr),
										tempDboField.getVal(SN.LABEL_CODE).toString());
							}
							break;
						default:
							fieldTitle = I18n.getText(getLocaleId(svr), tempDboField.getVal(SN.LABEL_CODE).toString());
							break;
						}
						jLeaf = addFieldTypeToJsonObject(tempDboField, jLeaf, true);
						jLeaf.addProperty(SN.TITLE_WITH_LOWERCASE, fieldTitle);
						if (SN.NVARCHAR.equals(tempDboField.getVal(SN.FIELD_TYPE).toString())
								&& ((Long) tempDboField.getVal(SN.FIELD_SIZE)) != null
								&& ((Long) tempDboField.getVal(SN.FIELD_SIZE)) > 0)
							jLeaf.addProperty(SN.MAX_LENGTH_LCASE, (Long) tempDboField.getVal(SN.FIELD_SIZE));
						if (jsonreactGUI != null && jsonreactGUI.has(SN.MIN_LENGTH_LCASE))
							jLeaf.addProperty(SN.MIN_LENGTH_LCASE, jsonreactGUI.get(SN.MIN_LENGTH_LCASE).getAsNumber());
						if (jsonreactGUI != null && jsonreactGUI.has(SN.MAX_LENGTH_LCASE))
							jLeaf.addProperty(SN.MAX_LENGTH_LCASE, jsonreactGUI.get(SN.MAX_LENGTH_LCASE).getAsLong());
						if (SN.NUMERIC.equals(tempDboField.getVal(SN.FIELD_TYPE).toString()))
							if (jsonreactGUI != null && jsonreactGUI.has(SN.MAXIMUM_LCASE)) {
								if (jsonreactGUI.get(SN.MAXIMUM_LCASE) != null
										&& !jsonreactGUI.get(SN.MAXIMUM_LCASE).getAsString().trim().equals(SN.EMPTY_STRING)) {
									jLeaf.addProperty(SN.MAXIMUM_LCASE, jsonreactGUI.get(SN.MAX_LENGTH_LCASE).getAsLong());
								}
							} else {
								jLeaf.addProperty(SN.MAXIMUM_LCASE, 999999999999999L);
							}
						jLeaf = prepareFormJsonCodeList1(tempDboField, jLeaf, svr);
						jFields = prepareFormJsonGroup(tempDboField, customFieldName, jFields, jLeaf);
					} else {
						jDependencies = prepareFormJsonDependentDropDown(tempDboField, jDependencies, jFields, svr);

					}
				}
			}
			
			// case add attachment json - getCustomJsonObject
			jFields.add("attachment.custom_info", getCustomJsonObject(SN.ATTACHMENT));
			
			
			jData.add(SN.PROPERTIES, jFields);
			jData.add(SN.DEPENDENCIES, jDependencies);
			/*
			 * parse the lists of required elements, create JsonElement for
			 * every group and add in proper level for the JsonData
			 */
			if (!listRequired.isEmpty()) {
				Iterator itr = set1.iterator();
				while (itr.hasNext()) {
					ArrayList<String> listPathRequired = new ArrayList<>();
					String vPath = itr.next().toString();
					itr.remove();
					Iterator<Entry<String, String>> it = listRequiredWithLink.entrySet().iterator();
					while (it.hasNext()) {
						Entry<String, String> pair = it.next();
						String tmpStr1 = pair.getValue();
						if (vPath.equals(tmpStr1)) {
							it.remove();
							listPathRequired.add(pair.getKey());
							String tmpStr2 = pair.getKey();
							for (int k = 0; k < listRequired.size(); k++)
								if (tmpStr2.equals(listRequired.get(k)))
									listRequired.remove(k);
						}
					}
					JsonElement element1 = gson.toJsonTree(listPathRequired, new TypeToken<List<String>>() {
					}.getType());
					JsonObject tmpData = (JsonObject) jData.get(SN.PROPERTIES);
					JsonObject tmpData1 = (JsonObject) tmpData.get(vPath);
					tmpData1.add(SN.REQUIRED, element1);
					tmpData.add(vPath, tmpData1);
					jData.add(SN.PROPERTIES, tmpData);
				}
				JsonElement element = gson.toJsonTree(listRequired, new TypeToken<List<String>>() {
				}.getType());
				if (element.isJsonArray()) {
					jData.add(SN.REQUIRED, element);
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(401).entity(e.getFormattedMessage()).build();
		}
		return Response.status(200).entity(jData.toString()).build();
	}

	private DbDataArray getCustomFields(String tableName, SvReader svr) throws SvException {
		DbDataArray fields = new DbDataArray();
		switch (tableName) {
		case SN.MESSAGE:
			List<String> customFieldNames = Arrays.asList("CUSTOM_RECIPIENTS", "CUSTOM_CC", "CUSTOM_BCC");
			for (String customFieldName : customFieldNames) {
				DbDataObject custom = new Reader().searchDbObjectBySingleFilter(svCONST.OBJECT_TYPE_FIELD, SN.FIELD_NAME, customFieldName, svr);
				if (Objects.isNull(custom)) {
					custom = new DbDataObject();
					custom.setObjectType(svCONST.OBJECT_TYPE_FIELD);
					custom.setVal(SN.FIELD_NAME, customFieldName);
					custom.setVal(SN.FIELD_TYPE, SN.NVARCHAR);
					custom.setVal(SN.FIELD_SIZE, 500L);
					if (!customFieldName.equals("CUSTOM_RECIPIENTS")) {
						custom.setVal(SN.IS_NULL, true);
					} else {
						custom.setVal(SN.IS_NULL, false);
					}
					custom.setVal("IS_UNIQUE", false);
					custom.setVal("IS_PRIMARY_KEY", false);
					custom.setVal("LABEL_CODE", "custom_field." + customFieldName.toLowerCase());
					custom.setVal(SN.GUI_METADATA, "{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"uischema\":{\"ui:widget\":\"CustomMultiSelectDropdown\"},\"grouppath\":\"message.custom_info\"}}");
				}
				fields.addDataItem(custom);
			}
			break;
		default:
			break;
		}
		return fields;
	}
	
	private DbDataObject getCustomField(String field) {
		DbDataObject custom = new DbDataObject();
		custom.setObjectType(svCONST.OBJECT_TYPE_FIELD);
		switch(field) {
		case SN.ATTACHMENT:
			custom.setVal(SN.FIELD_NAME, SN.ATTACHMENT);
			custom.setVal(SN.FIELD_TYPE, "array");
			custom.setVal(SN.IS_NULL, true);
			custom.setVal("IS_UNIQUE", false);
			custom.setVal("IS_PRIMARY_KEY", false);
			custom.setVal("LABEL_CODE", "custom_field." + field.toLowerCase());
			custom.setVal(SN.GUI_METADATA, "{\"react\":{\"filterable\":true,\"visible\":true,\"sortable\":true,\"resizable\":true,\"editable\":true,\"uischema\":{\"ui:options\":{\"orderable\":false}},\"grouppath\":\"attachment.custom_info\"}}");
			break;
		default:
			break;
		}
		return custom;
	}
	
	private JsonObject getCustomJsonObject(String field) {
		JsonObject obj = new JsonObject();
		switch(field) {
		case SN.ATTACHMENT:
			obj.addProperty(SN.TITLE_WITH_LOWERCASE, SN.ATTACHMENT);
			obj.addProperty(SN.TYPE_WITH_LOWERCASE, "array");
			JsonObject item = new JsonObject();
			item.addProperty(SN.TYPE_WITH_LOWERCASE, "string");
			item.addProperty("default", "Attach item");
			obj.add("items", item);
			break;
		default:
			break;
		}
		return obj;
	}
	
	@Path("getObjectsByParentId/{sessionId}/{parentId}/{tableName}/{orderByField}/{sortOrder}")
	@GET
	@Produces("application/json")
	public Response getObjectsByParentId(@PathParam("sessionId") String sessionId, @PathParam("parentId") Long parentId,
			@PathParam("tableName") String tableName, @PathParam("orderByField") String orderByField,
			@PathParam("sortOrder") String sortOrder) {
		ResponseHandler jrh = null;
		Reader rdr = null;
		Writer wr = null;
		JsonArray jArr = null;
		JsonObject jObj = null;
		try (SvReader svr = new SvReader(sessionId)) {
			jrh = new ResponseHandler();
			rdr = new Reader();
			wr = new Writer();
			jArr = new JsonArray();
			jObj = new JsonObject();
			if (parentId != null && tableName != null) {
				DbDataObject dboTypeDesc = SvReader.getDbtByName(tableName.toUpperCase());
				DbDataArray arrResult = svr.getObjectsByParentId(parentId, dboTypeDesc.getObjectId(), null, 0, 0);
				jArr = rdr.sortAndPrepareCustomJsonArray(arrResult, jObj, tableName, wr);
				jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFLLY_GET_MESSAGES),
						I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFLLY_GET_MESSAGES));
			}
		} catch (SvException e) {
			return handleException(e, jrh, SN.svarog_notifications_SUCCESS_SUCCESSFLLY_GET_MESSAGES);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}
	
	@Path("getInboxSubjectRecipientInfo/{sessionId}")
	@GET
	@Produces("application/json")
	public Response getInboxSubjectRecipientInfo(@PathParam("sessionId") String sessionId) {
		ResponseHandler jrh = null;
		DbDataArray inboxMessages = null;
		DbDataArray inboxSubjects = null;
		DbDataArray subjectsWithoutDuplicates = null;
		JsonArray jsonArray = null;
		Reader rdr = null;
		try (SvReader svr = new SvReader(sessionId)) {
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			jrh = new ResponseHandler();
			rdr = new Reader();
			jsonArray = new JsonArray();
			if (dboUser != null) {
				inboxMessages = rdr.getMessagesByCriteria(dboUser, svr);
				inboxSubjects = rdr.getSubjectsInArray(inboxMessages, svr);
				subjectsWithoutDuplicates = rdr.removeDuplicatesFromDbDataArray(inboxSubjects);
				jsonArray = rdr.getInboxSubjectsWithMessageRecipientInfo(subjectsWithoutDuplicates, dboUser, svr);
			}
		} catch (SvException e) {
			return handleException(e, jrh, SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECT_RECIPIENT_INFO);
		}
		return Response.status(200).entity(jsonArray.toString()).build();
	}

	@Path("getSentOrArchivedSubjectRecipientInfo/{sessionId}/{subjectStatus}")
	@GET
	@Produces("application/json")
	public Response getSentOrArchivedSubjectRecipientInfo(@PathParam("sessionId") String sessionId,
			@PathParam("subjectStatus") String subjectStatus) {
		ResponseHandler jrh = null;
		DbDataArray sentOrArchivedSubjects = null;
		DbDataArray subjectsWithoutDuplicates = null;
		JsonArray jsonArray = null;
		Reader rdr = null;
		try (SvReader svr = new SvReader(sessionId)) {
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			jrh = new ResponseHandler();
			rdr = new Reader();
			jsonArray = new JsonArray();
			if (dboUser != null) {
				sentOrArchivedSubjects = rdr.getSentOrArchiveSubjects(dboUser, subjectStatus, svr);
				subjectsWithoutDuplicates = rdr.removeDuplicatesFromDbDataArray(sentOrArchivedSubjects);
				jsonArray = rdr.getSentOrArchivedSubjectsWithMessageRecipientInfo(subjectsWithoutDuplicates,
						svr);
			}
		} catch (SvException e) {
			return handleException(e, jrh, SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECT_RECIPIENT_INFO);
		}
		return Response.status(200).entity(jsonArray.toString()).build();
	}
	
	@Path("/getTableFieldList/{session_id}/{table_name}")
	@GET
	@Produces("application/json")
	public Response getTableFieldList(@PathParam("session_id") String sessionId,
			@PathParam("table_name") String tableName, @Context HttpServletRequest httpRequest) {
		JsonArray jArray = new JsonArray();
		try (SvReader svr = new SvReader(sessionId)) {
			DbDataObject tableObject = SvCore.getDbtByName(tableName);
			DbDataArray typetoGet = svr.getObjectsByParentId(tableObject.getObjectId(), svCONST.OBJECT_TYPE_FIELD, null,
					0, 0, SN.SORT_ORDER);
			for (int i = 0; i < typetoGet.getItems().size(); i++) {
				String tmpField = typetoGet.getItems().get(i).getVal(SN.FIELD_NAME).toString();
				if (processField(tmpField)) {
					JsonObject tryObject = prapareObjectField1(tableName, typetoGet.getItems().get(i), svr);
					if (tryObject.toString().length() > 5)
						jArray.add(tryObject);
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(401).entity(e.getFormattedMessage()).build();
		}
		return Response.status(200).entity(jArray.toString()).build();
	}
	
	public static JsonObject prapareObjectField1(String tableName, DbDataObject oField, SvReader svr) {
		/*
		 * prepare the FieldList part of the field object, all the drop-downs or
		 * edit options, and all properties that can be read from GUI_METADATA
		 * of the field or from CODE_LIST_ID if it exist for the field, in the
		 * end
		 */
		JsonObject jsonreactGUI = null;
		Long width = 0L;
		Gson gson = new Gson();
		Boolean visiblefield = true;
		Boolean sortablefield = false;
		JsonObject jData = new JsonObject();
		// try to get react metadata and visibility of the field
		JsonObject jsonObj = null;
		try {
			if (oField.getVal(SN.GUI_METADATA) != null && !"".equals(oField.getVal(SN.GUI_METADATA)))
				jsonObj = gson.fromJson(oField.getVal(SN.GUI_METADATA).toString(), JsonObject.class);
		} catch (Exception e) {
			debugException(e);
		}
		if (jsonObj != null && jsonObj.has(SN.REACT)) {
			jsonreactGUI = (JsonObject) jsonObj.get(SN.REACT);
			if (jsonreactGUI != null && jsonreactGUI.has(SN.VISIBLE))
				visiblefield = jsonreactGUI.get(SN.VISIBLE).getAsBoolean();
			if (jsonreactGUI != null && jsonreactGUI.has(SN.WIDTH))
				width = jsonreactGUI.get(SN.WIDTH).getAsLong();
			if (jsonreactGUI != null && jsonreactGUI.has(SN.SORTABLE))
				sortablefield = jsonreactGUI.get(SN.SORTABLE).getAsBoolean();
		} else {
			// if there is no react metadata we just show the field :(
			jsonreactGUI = null;
			visiblefield = true;
		}
		if (visiblefield) {
			jData.addProperty("key", tableName + "." + oField.getVal(SN.FIELD_NAME));
			jData.addProperty(SN.TABLE_NAME, tableName);
			jData.addProperty(SN.FIELD_NAME, oField.getVal(SN.FIELD_NAME).toString());
			String fieldType = "";
			if (oField.getVal(SN.FIELD_TYPE) != null)
				fieldType = oField.getVal(SN.FIELD_TYPE).toString();
			switch (fieldType.toUpperCase()) {
			case "DATE":
				jData.addProperty(SN.DATETYPE_LCASE, "shortdate");
				break;
			case "TIMESTAMP":
			case "DATETIME":
				jData.addProperty(SN.DATETYPE_LCASE, "longdate");
				break;
			default:
			}
			jData.addProperty("name", I18n.getText(getLocaleId(svr), oField.getVal(SN.LABEL_CODE).toString()));
			if (width > 0)
				jData.addProperty(SN.WIDTH, width);
			if (sortablefield)
				jData.addProperty(SN.SORTABLE, sortablefield);
			for (Map.Entry<String, JsonElement> entry : prepareJsonCodeList1(oField, 2, true, svr).entrySet()) {
				jData.add(entry.getKey(), entry.getValue());
			}
			for (Map.Entry<String, JsonElement> entry : prepareJsonGUI1(jsonreactGUI).entrySet()) {
				jData.add(entry.getKey(), entry.getValue());
			}
		}
		return jData;
	}
	
	public static JsonObject prepareJsonGUI1(JsonObject jsonreactGUI) {
		JsonObject jData = new JsonObject();
		if (jsonreactGUI == null) {
			jData.addProperty(SN.FILTERABLE, true);
			jData.addProperty(SN.RESIZABLE, true);
		} else {
			jData = jsonreactGUI;
			if (jData.has(SN.INLINE_EDITABLE) && jData.get(SN.INLINE_EDITABLE).getAsBoolean()) {
				jData.addProperty(SN.EDITABLE, true);
			} else
				jData.addProperty(SN.EDITABLE, false);
			jData.remove(SN.INLINE_EDITABLE);
			jData.remove(SN.UISCHEMA);
		}
		return jData;
	}
	
	public static JsonObject prepareJsonCodeList1(DbDataObject tmpField, int formaterType, Boolean editableField,
			SvReader svr) {
		JsonObject jData = new JsonObject();
		if (tmpField != null) {
			Long plistCodeId = (Long) tmpField.getVal(SN.CODE_LIST_ID);
			/*
			 * when using forms/documents type is string, but on normal table
			 * fields type could be number , or something else. we try to get
			 * list by code, if we get 0 results we also try by gui_metadata. if
			 * field can be edited we must include all codes in editorOptions,
			 * and put editorType
			 */
			String fieldType = SN.NVARCHAR;
			if (tmpField.getVal(SN.FIELD_TYPE) != null)
				fieldType = tmpField.getVal(SN.FIELD_TYPE).toString();
			String guiMetadata = "";
			if (tmpField.getVal(SN.GUI_METADATA) != null)
				guiMetadata = tmpField.getVal(SN.GUI_METADATA).toString();
			JsonArray jarr = prepareJsonCodeListById(plistCodeId, fieldType, svr);
			if (jarr.size() == 0)
				jarr = prepareJsonCodeListByMetadata(guiMetadata, fieldType, svr);
			if (jarr.size() > 0) {
				switch (formaterType) {
				case 1:
					jData.addProperty("editorType", "AutoCompleteEditor");
					break;
				case 2:
					jData.addProperty("formatterType", "DropDownFormatter");
					if (editableField)
						jData.addProperty("editorType", "DropDownEditor");
					break;
				default:
				}
				if (editableField)
					jData.add("editorOptions", jarr);
				jData.add("formatterOptions", jarr);
			}
		}
		return jData;
	}
	
	private static JsonArray prepareJsonCodeListByMetadata(String guiMetadata, String fieldType, SvReader svr) {
		/*
		 * we create the code-list by gui_metadata that is saved in GUI_METADATA
		 * of the field that we process. First we read if there is "react"
		 * JsonObject, and then IDTABLE/IDGETFIELD pair that will tell us from
		 * what table and what is the name of the field that we want to show.
		 * last part is IDFIELD/IDVALUE that works as extra filter ex. we want
		 * to display all USERNAME(idgetfield) from table USERS(idtable) that
		 * are of TYPE(idfield) INTERNAL(idvalue)
		 */
		JsonArray jarr = new JsonArray();
		JsonObject jLeaf = null;
		Gson gson = new Gson();
		DbSearchExpression expr = null;
		if (guiMetadata != null && !"".equals(guiMetadata))
			try {
				JsonObject jsonObj = gson.fromJson(guiMetadata, JsonObject.class);
				JsonObject jsonreactGUI = null;
				if (jsonObj != null && jsonObj.has(SN.REACT))
					jsonreactGUI = (JsonObject) jsonObj.get(SN.REACT);
				if (jsonreactGUI != null && jsonreactGUI.has(SN.IDTABLE) && jsonreactGUI.has(SN.IDGETFIELD)) {
					DbDataObject tableObject = SvCore.getDbtByName(jsonreactGUI.get(SN.IDTABLE).getAsString());
					if (jsonreactGUI.has(SN.IDFIELD_LCASE) && jsonreactGUI.has(SN.IDVALUE_LCASE)) {
						DbSearchCriterion critU = new DbSearchCriterion(jsonreactGUI.get(SN.IDFIELD_LCASE).getAsString(),
								DbCompareOperand.EQUAL, jsonreactGUI.get(SN.IDVALUE_LCASE).getAsString());
						expr = new DbSearchExpression();
						expr.addDbSearchItem(critU);
					}
					DbDataArray vData = svr.getObjects(expr, tableObject.getObjectId(), null, 0, 0);
					String tmpLocale = getLocaleId(svr);
					String tmpStrValue = jsonreactGUI.get(SN.IDGETFIELD).getAsString();
					Long dontTranslate1 = SvCore.getTypeIdByName("MINERALS_SUBJECTS");
					Long dontTranslate2 = SvCore.getTypeIdByName("LAND_USE_CODE");
					for (int j = 0; j < vData.getItems().size(); j++) {
						jLeaf = new JsonObject();
						String tmpString = vData.getItems().get(j).getObjectId().toString();
						if (fieldType.equals(SN.NVARCHAR)) {
							jLeaf.addProperty(SN.ID, tmpString);
							jLeaf.addProperty(SN.VALUE_LC, tmpString);
						} else if (fieldType.equals(SN.NUMERIC)) {
							jLeaf.addProperty(SN.ID, Long.parseLong(tmpString));
							jLeaf.addProperty(SN.VALUE_LC, Long.parseLong(tmpString));
						}
						if (tableObject.getObjectId().equals(dontTranslate1)
								|| tableObject.getObjectId().equals(dontTranslate2)) {
							tmpString = vData.getItems().get(j).getVal(tmpStrValue).toString();
						} else
							tmpString = I18n.getText(tmpLocale, vData.getItems().get(j).getVal(tmpStrValue).toString());
						jLeaf.addProperty(SN.TITLE, tmpString);
						jLeaf.addProperty(SN.TEXT, tmpString);
						jarr.add(jLeaf);
					}
				}
			} catch (Exception e) {
				debugException(e);
			}
		return jarr;
	}
	
	private static JsonArray prepareJsonCodeListById(Long plistCodeId, String fieldType, SvReader svr) {
		/*
		 * we get new CodeList and read the list of codes by the given code ID ,
		 * then we create array of JosnObjects, each object has 4 items ,ID,
		 * value, text, title. Value is saved according to fieldType paramter as
		 * string or number
		 */
		JsonArray jarr = new JsonArray();
		CodeList cl = null;
		if (plistCodeId != null)
			try {
				cl = new CodeList(svr);
				HashMap<String, String> listMap;
				listMap = cl.getCodeList(getLocaleId(svr), plistCodeId, true);
				Iterator<Entry<String, String>> it = listMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, String> pair = it.next();
					it.remove();
					JsonObject jLeaf = new JsonObject();
					if (fieldType.equals(SN.NVARCHAR)) {
						jLeaf.addProperty(SN.ID, (String) pair.getKey());
						jLeaf.addProperty(SN.VALUE_LC, (String) pair.getKey());
					} else if (fieldType.equals(SN.NUMERIC)) {
						jLeaf.addProperty(SN.ID, Long.parseLong((String) pair.getKey()));
						jLeaf.addProperty(SN.VALUE_LC, Long.parseLong((String) pair.getKey()));
					} else if (fieldType.equals(SN.BOOLEAN)) {
						if ("true".equalsIgnoreCase(pair.getKey())) { // if true
							if ("mk_MK".equalsIgnoreCase(SvConf.getDefaultLocale())) {// if
																						// mk
								jLeaf.addProperty(SN.ID, I18n.getText(SvConf.getDefaultLocale(), "mk.yes"));
								jLeaf.addProperty(SN.VALUE_LC, I18n.getText(SvConf.getDefaultLocale(), "mk.yes"));
							} else {
								jLeaf.addProperty(SN.ID, I18n.getText(SvConf.getDefaultLocale(), "yes"));
								jLeaf.addProperty(SN.VALUE_LC, I18n.getText(SvConf.getDefaultLocale(), "yes"));
							}

						} else { // if false
							if ("mk_MK".equalsIgnoreCase(SvConf.getDefaultLocale())) {
								jLeaf.addProperty(SN.ID, I18n.getText(SvConf.getDefaultLocale(), "mk.no"));
								jLeaf.addProperty(SN.VALUE_LC, I18n.getText(SvConf.getDefaultLocale(), "mk.no"));
							} else {
								jLeaf.addProperty(SN.ID, I18n.getText(SvConf.getDefaultLocale(), "no"));
								jLeaf.addProperty(SN.VALUE_LC, I18n.getText(SvConf.getDefaultLocale(), "no"));
							}
						}
					}
					jLeaf.addProperty(SN.TITLE_LC, (String) pair.getValue());
					jLeaf.addProperty(SN.TEXT_LC, (String) pair.getValue());
					jarr.add(jLeaf);
				}
			} catch (Exception e) {
				debugException(e);
			} finally {
				if (cl != null)
					cl.release();
			}
		return jarr;
	}
	
	/**
	 * Web service that return inbox subjects with pagination
	 * 
	 * @param sessionId
	 * @param from
	 *            Interval start
	 * @param to
	 *            Interval end
	 * @return
	 */
	@Path("/getInboxSubjectsWithPagination/{sessionId}/{from}/{to}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInboxSubjectsWithPagination(@PathParam("sessionId") String sessionId,
			@PathParam("from") int from, @PathParam("to") int to) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		Writer wr = null;
		DbDataArray dbArrMessages = null;
		JsonObject jObj = null;
		JsonArray jArr = null;
		List<Long> subjects = null;
		List<Long> subjectWithoutDuplicates = null;
		DbDataArray finalList = null;
		DbDataArray result = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			wr = new Writer();
			dbArrMessages = new DbDataArray();
			jObj = new JsonObject();
			jArr = new JsonArray();
			subjects = new ArrayList<Long>();
			subjectWithoutDuplicates = new ArrayList<Long>();
			finalList = new DbDataArray();
			result = new DbDataArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrMessages = rdr.getMessagesByCriteria(dboUser, svr);
				subjects = rdr.getSubjectIds(dbArrMessages);
				subjectWithoutDuplicates = rdr.removeDuplicates(subjects);
				finalList = rdr.prepareDbDataArrayFromList(subjectWithoutDuplicates, svr);
				result = rdr.getObjectsFromInterval(finalList, from, to, SN.VALID);
				for (DbDataObject subjectToList : result.getItems()) {
					jObj = wr.createSubjectJson(subjectToList);
					jArr.add(jObj);
				}
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSULLY_GET_INBOX_SUBJECTS),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSULLY_GET_INBOX_SUBJECTS));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_INBOX_SUBJECTS);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}
	
	/**
	 * Web service that return sent or archived subjects with pagination
	 * 
	 * @param sessionId
	 * @param from
	 * @param to
	 * @param subjectStatus
	 * @return
	 */
	@Path("/getSentOrArchivedSubjectsWithPagination/{sessionId}/{from}/{to}/{subjectStatus}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSentOrArchivedSubjectsWithPagination(@PathParam("sessionId") String sessionId,
			@PathParam("from") int from, @PathParam("to") int to, @PathParam("subjectStatus") String subjectStatus) {
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		Writer wr = null;
		DbDataArray dbArrMessages = null;
		JsonObject jObj = null;
		JsonArray jArr = null;
		List<Long> subjects = null;
		List<Long> subjectWithoutDuplicates = null;
		DbDataArray finalList = null;
		DbDataArray result = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			wr = new Writer();
			dbArrMessages = new DbDataArray();
			jObj = new JsonObject();
			jArr = new JsonArray();
			subjects = new ArrayList<Long>();
			subjectWithoutDuplicates = new ArrayList<Long>();
			finalList = new DbDataArray();
			result = new DbDataArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrMessages = rdr.getSentMessages(dboUser, svr);
				subjects = rdr.getSubjectIds(dbArrMessages);
				subjectWithoutDuplicates = rdr.removeDuplicates(subjects);
				finalList = rdr.prepareDbDataArrayFromList(subjectWithoutDuplicates, svr);
				result = rdr.getObjectsFromInterval(finalList, from, to, subjectStatus);
				for (DbDataObject subjectToList : result.getItems()) {
					jObj = wr.createSubjectJson(subjectToList);
					jArr.add(jObj);
				}
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECTS),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_SUBJECTS));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_SUBJECTS);
		}
		return Response.status(200).entity(jArr.toString()).build();
	}
	
	/**
	 * Web service that return number of total objects from defined category
	 * (INBOX / SENT)
	 * 
	 * @param sessionId
	 * @param inboxOrSent
	 * @return
	 */
	@Path("/countTotalNumberOfObjectsPerCategory/{sessionId}/{inboxOrSent}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response countTotalNumberOfObjectsPerCategory(@PathParam("sessionId") String sessionId,
			@PathParam("inboxOrSent") String inboxOrSent) {
		int result = 0;
		ResponseHandler jrh = new ResponseHandler();
		Reader rdr = null;
		try (SvReader svr = new SvReader(sessionId)) {
			rdr = new Reader();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				switch (inboxOrSent) {
				case SN.SENT:
					result = rdr.getNumberOfSentSubjects(dboUser.getObjectId(), svr);
					break;
				case SN.INBOX:
					result = rdr.getNumberOfInboxSubjects(dboUser.getVal(SN.USER_NAME).toString(), svr);
					break;
				case SN.ARCHIVED:
					result = rdr.getNumberOfArchivedSubjects(dboUser.getVal(SN.USER_NAME).toString(),
							dboUser.getObjectId(), svr);
					break;
				default:
					break;
				}
			}
			jrh.create(MessageType.SUCCESS, I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA),
					I18n.getText(SN.svarog_notifications_SUCCESS_SUCCESSFULLY_GET_DATA));
		} catch (Exception e) {
			return handleException(e, jrh, SN.svarog_notifications_ERROR_FAILED_TO_GET_DATA);
		}
		return Response.status(200).entity(String.valueOf(result)).build();
	}
}