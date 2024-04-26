package com.prtech.svarog_notifications;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbQueryExpression;
import com.prtech.svarog_common.DbQueryObject;
import com.prtech.svarog_common.DbQueryObject.DbJoinType;
import com.prtech.svarog_common.DbQueryObject.LinkType;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;

import com.prtech.svarog_common.DbSearchExpression;

public class Reader {

	static final Logger log4j = LogManager.getLogger(Reader.class.getName());
	
	/**
	 * Simple help method for fetching DB object by single filter
	 * 
	 * @param objectType
	 * @param columnName
	 * @param columnValue
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject searchDbObjectBySingleFilter(Long objectType, String columnName, Object columnValue,
			SvReader svr) {
		return searchDbObjectBySingleFilter(DbCompareOperand.EQUAL, objectType, columnName, columnValue, svr);
	}

	/**
	 * Simple method for searching object by single filter
	 * 
	 * @param objectType
	 * @param columnName
	 * @param value
	 * @param svr
	 * @return
	 */
	public DbDataObject searchDbObjectBySingleFilter(DbCompareOperand operand, Long objectType, String columnName,
			Object value, SvReader svr) {
		DbDataObject dbo = null;
		try {
			DbSearchCriterion cr1 = new DbSearchCriterion(columnName, operand, value);
			DbDataArray arrFoundDbObjects = svr.getObjects(cr1, objectType, null, 1, 0);
			if (!arrFoundDbObjects.isEmpty()) {
				dbo = arrFoundDbObjects.get(0);
			}
		} catch (SvException e) {
			log4j.error(e);
		}
		return dbo;
	}
	
	/**
	 * Method that search messages by subject (title/category) and messages
	 * (priority) criteria. Additionally, the user can see the messages only if
	 * has one of this links with message/s (MSG_TO/MSG_CC/MSG_BCC)
	 * 
	 * @param dboUser
	 *            Current user
	 * @param subjectTitle
	 *            Title of the subject
	 * @param subjectCategory
	 *            Category of the subject
	 * @param subjectPriority
	 *            Priority of the subject
	 * @param messageText
	 *            Text message
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSubjectsBySubjectAndMessageCriteria(DbDataObject dboUser, String subjectTitle,
			String subjectCategory, String subjectPriority, String messageText, SvReader svr) throws SvException {
		DbDataObject dboSubjectDesc = SvReader.getDbt(SvReader.getTypeIdByName(SN.SUBJECT));
		DbDataObject dboMessageDesc = SvReader.getDbt(SvReader.getTypeIdByName(SN.MESSAGE));
		String cr1 = " and tbl0.title ilike " + "'%" + subjectTitle + "%'";
		String cr2 = " and tbl0.category = " + "'" + subjectCategory + "'";
		String cr3 = " and tbl0.priority = " + "'" + subjectPriority + "'";
		String cr4 = " and tbl1.text ilike " + "'%" + messageText + "%'";
		if (subjectTitle == null || subjectTitle.trim().equals("")) {
			cr1 = SN.EMPTY_STRING;
		}
		if (subjectCategory == null || subjectCategory.trim().equals("")) {
			cr2 = SN.EMPTY_STRING;
		}
		if (subjectPriority == null || subjectPriority.trim().equals("")) {
			cr3 = SN.EMPTY_STRING;
		}
		if (messageText == null || messageText.trim().equals("")) {
			cr4 = SN.EMPTY_STRING;
		}
		DbQueryObject dqoSubject = new DbQueryObject(dboSubjectDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		dqoSubject.setCustomFreeTextJoin(SN.SEARCH_FREE_TEXT + cr1 + cr2 + cr3 + cr4);
		DbQueryObject dqoMessage = new DbQueryObject(dboMessageDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		DbQueryExpression dbqe = new DbQueryExpression();
		dbqe.addItem(dqoSubject);
		dbqe.addItem(dqoMessage);
		DbDataArray dbArrQuery = svr.getObjects(dbqe, 0, 0);
		DbDataArray dbArrMessages = new DbDataArray();
		if (dbArrQuery != null && !dbArrQuery.getItems().isEmpty()) {
			for (DbDataObject dboLinkedObject : dbArrQuery.getItems()) {
				dboMessageDesc = svr.getObjectById(
						Long.valueOf(dboLinkedObject.getVal("TBL1_" + SN.OBJECT_ID).toString()),
						SvReader.getTypeIdByName(SN.MESSAGE), null);
				dboSubjectDesc = svr.getObjectById(
						Long.valueOf(dboLinkedObject.getVal("TBL0_" + SN.OBJECT_ID).toString()),
						SvReader.getTypeIdByName(SN.SUBJECT), null);
				if (checkIfLinkExists(dboMessageDesc, dboUser, SN.MSG_TO, null, svr)
						|| checkIfLinkExists(dboMessageDesc, dboUser, SN.MSG_CC, null, svr)
						|| checkIfLinkExists(dboMessageDesc, dboUser, SN.MSG_BCC, null, svr)
						|| dboMessageDesc.getVal(SN.CREATED_BY).equals(dboUser.getObjectId())) {
					dbArrMessages.addDataItem(dboSubjectDesc);
				}
			}
		}
		return dbArrMessages;
	}

	/**
	 * Method that remove duplicates from array of subjects
	 * 
	 * @param dbArrSubjects
	 *            DbDataArray of subjects
	 * @return
	 */
	public DbDataArray removeDuplicatesFromDbDataArray(DbDataArray dbArrSubjects) {
		DbDataArray dbArrFinal = new DbDataArray();
		HashSet<DbDataObject> hs = new HashSet<DbDataObject>();
		if (dbArrSubjects != null && !dbArrSubjects.getItems().isEmpty()) {
			for (DbDataObject dboSubject : dbArrSubjects.getItems()) {
				hs.add(dboSubject);
			}
			for (DbDataObject dbo : hs) {
				dbArrFinal.addDataItem(dbo);
			}
		}
		return dbArrFinal;
	}

	/**
	 * Method that return array of user/s linked to message (TO/CC/BCC)
	 * 
	 * @param dboMessage
	 *            DbDataObject of message
	 * @param linkType
	 *            Type of link (TO/CC/BCC)
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUsersLinkedToMessage(DbDataObject dboMessage, String linkType, SvReader svr)
			throws SvException {
		DbDataArray dbArrUsers = null;
		DbDataObject dbLink = SvLink.getLinkType(linkType, SvReader.getTypeIdByName(SN.MESSAGE),
				svCONST.OBJECT_TYPE_USER);
		dbArrUsers = svr.getObjectsByLinkedId(dboMessage.getObjectId(), SvReader.getTypeIdByName(SN.MESSAGE), dbLink,
				svCONST.OBJECT_TYPE_USER, false, null, 0, 0);

		return dbArrUsers;
	}

	/**
	 * Method that return array of user/s linked to message (MSG_TO)
	 * 
	 * 
	 * @param dboMessage
	 *            Message object
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUsersLinkedToMessage(DbDataObject dboMessage, SvReader svr) throws SvException {
		DbDataArray dbArrMsgTo = null;
		dbArrMsgTo = getUsersLinkedToMessage(dboMessage, SN.MSG_TO, svr);
		return dbArrMsgTo;
	}

	/**
	 * Method that return VALID subjects sent by user or ARCHIVED subjects
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param subjectStatus
	 *            Status of subject
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSentOrArchiveSubjects(DbDataObject dboUser, String subjectStatus, SvReader svr)
			throws SvException {
		DbDataArray dbArrSubjects = new DbDataArray();
		DbDataObject dboSubjectDesc = SvReader.getDbt(SvReader.getTypeIdByName(SN.SUBJECT));
		DbDataObject dboMessageDesc = SvReader.getDbt(SvReader.getTypeIdByName(SN.MESSAGE));
		String cr1 = " and tbl1.created_by =" + dboUser.getObjectId();
		String cr2 = " and tbl0.status =" + "'" + subjectStatus + "'";
		DbQueryObject dqoSubject = new DbQueryObject(dboSubjectDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		if (subjectStatus.equals(SN.VALID)) {
			dqoSubject.setCustomFreeTextJoin(SN.SEARCH_FREE_TEXT + cr1 + cr2);
		} else {
			dqoSubject.setCustomFreeTextJoin(SN.SEARCH_FREE_TEXT + cr2);
		}
		DbQueryObject dqoMessage = new DbQueryObject(dboMessageDesc, null, DbJoinType.INNER, null,
				LinkType.CUSTOM_FREETEXT, null, null);
		DbQueryExpression dbqe = new DbQueryExpression();
		dbqe.addItem(dqoSubject);
		dbqe.addItem(dqoMessage);
		DbDataArray dbArrQuery = svr.getObjects(dbqe, 0, 0);
		if (dbArrQuery != null && !dbArrQuery.getItems().isEmpty()) {
			for (DbDataObject dboLinkedObject : dbArrQuery.getItems()) {
				dboSubjectDesc = svr.getObjectById(
						Long.valueOf(dboLinkedObject.getVal("TBL0_" + SN.OBJECT_ID).toString()),
						SvReader.getTypeIdByName(SN.SUBJECT), null);
				dbArrSubjects.addDataItem(dboSubjectDesc);
			}
		}
		return dbArrSubjects;
	}

	/**
	 * Method that get SENT or ARCHIVED subjects in appropriate JSON format
	 *
	 * @param dbArrSubjects
	 *            DbDataArray of subjects
	 * @param dboUser
	 *            DbDataObject of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public JsonArray getSentOrArchivedSubjectsWithMessageRecipientInfo(DbDataArray dbArrSubjects, SvReader svr)
			throws SvException {
		DbDataObject dboMessage = new DbDataObject();
		DbDataArray dbArrMsgTo = null;
		JsonArray jsonArray = new JsonArray();
		if (dbArrSubjects != null && !dbArrSubjects.getItems().isEmpty()) {
			for (DbDataObject dbo : dbArrSubjects.getItems()) {
				JsonObject json = new JsonObject();
				json.add(SN.SUBJECT, dbo.toSimpleJson());
				DbDataArray dbArrMessages = svr.getObjectsByParentId(dbo.getObjectId(),
						SvReader.getTypeIdByName(SN.MESSAGE), null, 0, 0);
				if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
					if (dbArrMessages.size() == 1) {
						dboMessage = dbArrMessages.get(0);
					} else if (dbArrMessages.size() > 1) {
						dboMessage = dbArrMessages.get(dbArrMessages.size() - 1);
					}
					dbArrMsgTo = getUsersLinkedToMessage(dboMessage, svr);
					json.add(SN.MSG_TO, dbArrMsgTo.toSimpleJson());
					json.add(SN.MSG_CC, getUsersLinkedToMessage(dboMessage, SN.MSG_CC, svr).toSimpleJson());
					json.add(SN.MSG_BCC, getUsersLinkedToMessage(dboMessage, SN.MSG_BCC, svr).toSimpleJson());
					json.addProperty(SN.CREATED_BY_USERNAME, dboMessage.getVal(SN.CREATED_BY_USERNAME).toString());
					// json.addProperty(SN.DATE_OF_CREATION,
					// dboMessage.getDtInsert().toString());
					// json.addProperty(SN.LINK_STATUS,
					// getStatusOfLinkObjectBetweenMessageAndUser(dbArrMessages,
					// dboUser.getObjectId(), svr));
				}
				jsonArray.add(json);
			}
		}
		return jsonArray;
	}

	/**
	 * Method that get MESSAGE/s by criteria (TO/CC/BCC)
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getMessagesByCriteria(DbDataObject dboUser, SvReader svr) throws SvException {
		DbDataArray finalMessageList = new DbDataArray();
		getInboxMessagesThroughLink(dboUser, finalMessageList, svr);
		return finalMessageList;
	}

	/**
	 * Method that put SUBJECT/s in array with status VALID
	 * 
	 * @param dbArrMessages
	 *            DbDataArray of messages
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSubjectsInArray(DbDataArray dbArrMessages, SvReader svr) throws SvException {
		DbDataArray dbArrSubjects = new DbDataArray();
		if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
			for (DbDataObject dboTempMessage : dbArrMessages.getItems()) {
				DbDataObject dboSubject = svr.getObjectById(dboTempMessage.getParentId(),
						SvReader.getTypeIdByName(SN.SUBJECT), null);
				if (dboSubject != null && dboSubject.getStatus().equals(SN.VALID)) {
					dbArrSubjects.addDataItem(dboSubject);
				}
			}
		}
		return dbArrSubjects;
	}

	/**
	 * Method that get INBOX subjects in appropriate JSON format
	 * 
	 * @param dbArrMessages
	 *            DbDataArray of messages
	 * @param dboUser
	 *            DbDataObject of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public JsonArray getInboxSubjectsWithMessageRecipientInfo(DbDataArray dbArrMessages, DbDataObject dboUser,
			SvReader svr) throws SvException {
		DbDataArray dbArrMsgTo = null;
		JsonArray jsonArray = new JsonArray();
		DbDataObject dboMessage = new DbDataObject();
		if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
			for (DbDataObject dbo : dbArrMessages.getItems()) {
				JsonObject json = new JsonObject();
				json.add(SN.SUBJECT, dbo.toSimpleJson());
				DbDataArray dbMessages = svr.getObjectsByParentId(dbo.getObjectId(),
						SvReader.getTypeIdByName(SN.MESSAGE), null, 0, 0);
				if (dbMessages != null && !dbMessages.getItems().isEmpty()) {
					if (dbMessages.size() == 1) {
						dboMessage = dbMessages.get(0);
					} else if (dbMessages.size() > 1) {
						dboMessage = dbMessages.get(dbMessages.size() - 1);
					}
					if (checkIfLinkExists(dboMessage, dboUser, SN.MSG_TO, null, svr)
							|| checkIfLinkExists(dboMessage, dboUser, SN.MSG_BCC, null, svr)
							|| dboMessage.getVal(SN.CREATED_BY).equals(dboUser.getObjectId())) {
						dbArrMsgTo = getUsersLinkedToMessage(dboMessage, svr);
						json.add(SN.MSG_TO, dbArrMsgTo.toSimpleJson());
						json.add(SN.MSG_CC, getUsersLinkedToMessage(dboMessage, SN.MSG_CC, svr).toSimpleJson());
						json.add(SN.MSG_BCC, getUsersLinkedToMessage(dboMessage, SN.MSG_BCC, svr).toSimpleJson());
					}
					if (checkIfLinkExists(dboMessage, dboUser, SN.MSG_CC, null, svr)) {
						dbArrMsgTo = getUsersLinkedToMessage(dboMessage, svr);
						json.add(SN.MSG_TO, dbArrMsgTo.toSimpleJson());
					}
					json.addProperty(SN.CREATED_BY_USERNAME, dboMessage.getVal(SN.CREATED_BY_USERNAME).toString());
					json.addProperty(SN.DATE_OF_CREATION, dboMessage.getDtInsert().toString());
					json.addProperty(SN.LINK_STATUS,
							getStatusOfLinkObjectBetweenMessageAndUser(dbMessages, dboUser.getObjectId(), svr));
				}
				jsonArray.add(json);
			}
		}
		return jsonArray;
	}

	/**
	 * Method that return status of link between message and user
	 * 
	 * @param dbArrMessages
	 *            DbDataArray of messages in subject
	 * @param dboUserObjId
	 *            Object id of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public String getStatusOfLinkObjectBetweenMessageAndUser(DbDataArray dbArrMessages, Long dboUserObjId, SvReader svr)
			throws SvException {
		String result = SN.VALID;
		DbDataArray dbArr = null;
		for (DbDataObject dboMessage : dbArrMessages.getItems()) {
			DbSearchCriterion cr1 = new DbSearchCriterion(SN.LINK_OBJ_ID_1, DbCompareOperand.EQUAL,
					dboMessage.getObjectId());
			DbSearchCriterion cr2 = new DbSearchCriterion(SN.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUserObjId);
			DbSearchCriterion cr3 = new DbSearchCriterion(SN.STATUS, DbCompareOperand.EQUAL, SN.UNSEEN);
			dbArr = svr.getObjects(
					new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3),
					svCONST.OBJECT_TYPE_LINK, new DateTime(), 0, 0);
			if (dbArr != null && !dbArr.getItems().isEmpty()) {
				result = SN.UNSEEN;
			}
		}
		return result;
	}

	/**
	 * Method that return number of links between message and user with status
	 * UNSEEN
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public int getNumberOfUnreadMessagesPerUser(DbDataObject dboUser, SvReader svr) throws SvException {
		DbDataArray dbArr = null;
		DbSearchCriterion cr1 = new DbSearchCriterion(SN.STATUS, DbCompareOperand.EQUAL, SN.UNSEEN);
		DbSearchCriterion cr2 = new DbSearchCriterion(SN.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUser.getObjectId());
		dbArr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_LINK, null, 0, 0);
		return dbArr.size();
	}

	/**
	 * Method that return list of links between message and user with status
	 * UNSEEN
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getListOfUnreadMessagesPerUser(DbDataObject dboUser, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(SN.STATUS, DbCompareOperand.EQUAL, SN.UNSEEN);
		DbSearchCriterion cr2 = new DbSearchCriterion(SN.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUser.getObjectId());
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
				svCONST.OBJECT_TYPE_LINK, null, 0, 0);
	}

	/**
	 * Method that return org units per org unit type
	 * 
	 * @param orgUnitType
	 *            Org unit type
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray searchForOrgUnit(String orgUnitType, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(SN.ORG_UNIT_TYPE, DbCompareOperand.EQUAL, orgUnitType);
		DbSearchExpression dbs = new DbSearchExpression().addDbSearchItem(cr1);
		return svr.getObjects(dbs, svCONST.OBJECT_TYPE_ORG_UNITS, null, 0, 0);
	}

//	/**
//	 * Method that returns data in grid type Json format.
//	 * 
//	 * @param dbArray
//	 * 
//	 * @param tableName
//	 *            Name of the table
//	 * @return
//	 * @throws SvException
//	 */
//	public JSONArray convertDbDataArrayToGridJson(DbDataArray dbArray, String tableName, boolean skipRepoFields,
//			SvReader svr) throws SvException {
//		return convertDbDataArrayToGridJson(dbArray, tableName, skipRepoFields, null, null, svr);
//	}

	public void getInboxMessagesThroughLink(DbDataObject dboUser, DbDataArray finalMessageList, SvReader svr)
			throws SvException {
		// get ASSIGN_TO messages through link
		getInboxMessagePerUser(dboUser, SN.MSG_TO, finalMessageList, svr);
		// get ASSIGN_CC messages through link
		getInboxMessagePerUser(dboUser, SN.MSG_CC, finalMessageList, svr);
		// get ASSIGN_BCC messages through link
		getInboxMessagePerUser(dboUser, SN.MSG_BCC, finalMessageList, svr);
	}

	/**
	 * Method that return INBOX messages per user
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param linkCode
	 *            Link type (TO / CC / BCC)
	 * @param finalMessageList
	 *            DbDataArray of final message list to save
	 * @param svr
	 *            SvReader instance
	 * @throws SvException
	 */
	public void getInboxMessagePerUser(DbDataObject dboUser, String linkCode, DbDataArray finalMessageList,
			SvReader svr) throws SvException {
		DbDataObject dbLinkMessageAndUser = SvReader.getLinkType(linkCode, SvReader.getTypeIdByName(SN.MESSAGE),
				svCONST.OBJECT_TYPE_USER);
		DbDataArray linkedTo = svr.getObjectsByLinkedId(dboUser.getObjectId(), svCONST.OBJECT_TYPE_USER,
				dbLinkMessageAndUser, SvReader.getTypeIdByName(SN.MESSAGE), true, null, 0, 0);
		if (linkedTo != null && !linkedTo.getItems().isEmpty()) {
			for (DbDataObject dbo : linkedTo.getItems()) {
				if (dbo.getStatus().equals(SN.VALID)) {
					finalMessageList.addDataItem(dbo);
				}
			}
		}
	}

	/**
	 * Method that checks if SvLink exists between two DB objects
	 * 
	 * @param dbo1
	 *            The first DbDataObject
	 * 
	 * @param dbo2
	 *            The second DbDataObject
	 * 
	 * @param linkName
	 *            name of the link type
	 * 
	 * @param refDate
	 *            The reference date on which we want to get the data set
	 * 
	 * @param svr
	 *            SvReader instance
	 * 
	 * @throws SvException
	 * @return Boolean
	 */
	public boolean checkIfLinkExists(DbDataObject dbo1, DbDataObject dbo2, String linkName, DateTime refDate,
			SvReader svr) throws SvException {
		Boolean result = false;
		DbDataObject dbLink = SvLink.getLinkType(linkName, dbo1.getObjectType(), dbo2.getObjectType());
		DbDataArray allItems = svr.getObjectsByLinkedId(dbo1.getObjectId(), dbLink, refDate, 0, 0);
		for (DbDataObject dbo : allItems.getItems()) {
			if (dbo.getObjectId().equals(dbo2.getObjectId())) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	public LinkedHashMap<String, JsonElement> getDbDataObjectsAsLinkedHashMap(DbDataObject dbo, String tableName,
			boolean skipRepoFields, SvReader svr) throws SvException {
		DbDataObject dboField = null;
		String localeId = SN.EMPTY_STRING;
		JsonObject convertedJObj = dbo.toJson().getAsJsonObject(dbo.getClass().getCanonicalName());
		LinkedHashMap<String, JsonElement> lhmObj = new LinkedHashMap<>();
		if (svr != null)
			localeId = svr.getUserLocaleId(svr.getInstanceUser());
		for (Entry<String, JsonElement> tempConverted : convertedJObj.entrySet()) {
			if (!tempConverted.getKey().equals("values")) {
				if (!skipRepoFields) {
					lhmObj.put(tableName + "." + tempConverted.getKey().toUpperCase(), tempConverted.getValue());
				}
			} else {
				JsonArray jsonArray = tempConverted.getValue().getAsJsonArray();
				for (JsonElement je : jsonArray) {
					for (Entry<String, JsonElement> value : je.getAsJsonObject().entrySet()) {
						// check if field has flag Tc.SV_ISLABEL:true
						if (svr != null) {
							dboField = SvReader.getFieldByName(tableName, value.getKey().toUpperCase());
							if (dboField != null && dboField.getVal(SN.SV_ISLABEL) != null
									&& dboField.getVal(SN.SV_ISLABEL).equals(true)) {
								lhmObj.put(tableName + "." + value.getKey().toUpperCase() + "_CODE", value.getValue());
								StringBuilder sb = new StringBuilder(
										"\"" + (I18n.getText(localeId, value.getValue().toString().replace("\"", "")))
												+ "\"");
								lhmObj.put(tableName + "." + value.getKey().toUpperCase(),
										new JsonParser().parse(sb.toString()));
							} else {
								lhmObj.put(tableName + "." + value.getKey().toUpperCase(), value.getValue());
							}
						} else {
							lhmObj.put(tableName + "." + value.getKey().toUpperCase(), value.getValue());
						}
					}
				}
			}
		}
		return lhmObj;
	}

	// Method for msgTo,msgCC,msgBcc assign to users processing
	public List<Long> convertStringIntoLongList(String s) {
		List<Long> result = new ArrayList<Long>();
		if (s != null && !s.trim().equals("")) {
			s = s.replace("[", "");
			s = s.replace("]", "");
			s = s.trim();
			if (!s.equals("")) {
				String[] s_list = s.split(SN.COMMA_SEPARATOR);
				for (String tempStr : s_list) {
					result.add(Long.valueOf(tempStr.trim()));
				}
			}
		}
		return result;
	}

	/**
	 * Method that return users linked to org unit.
	 * 
	 * @param orgUnitObjId
	 *            Org unit object id
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getUsersLinkedToOrgUnit(Long orgUnitObjId, SvReader svr) throws SvException {
		DbDataArray dbArrUsers = null;
		DbDataObject dbLink = SvLink.getLinkType(SN.POA, svCONST.OBJECT_TYPE_USER, svCONST.OBJECT_TYPE_ORG_UNITS);
		dbArrUsers = svr.getObjectsByLinkedId(orgUnitObjId, svCONST.OBJECT_TYPE_ORG_UNITS, dbLink,
				svCONST.OBJECT_TYPE_USER, true, null, 0, 0);
		return dbArrUsers;
	}
		
	public DbDataArray sortDbDataArrayByDtInsertCriteria(DbDataArray dbArr) {
		DbDataArray sortedAsc = new DbDataArray();
		ArrayList<DbDataObject> items = new ArrayList<>();
		for (DbDataObject dbo : dbArr.getItems()) {
			items.add(dbo);
		}
		Collections.sort(items, new Comparator<DbDataObject>() {
			public int compare(DbDataObject o1, DbDataObject o2) {
				return o1.getDtInsert().compareTo(o2.getDtInsert());
			}
		});
		for (DbDataObject dbo : items) {
			sortedAsc.addDataItem(dbo);
		}
		return sortedAsc;
	}
	
	/**
	 * Method that sort and prepare custom JsonArray of JsonObject/s
	 * 
	 * @param dbArr
	 *            DbDataArray of subjects
	 * @param jObj
	 *            JsonObject jObj
	 * @param tableName
	 *            Name of the table
	 * @param wr
	 *            Writer instance
	 * @throws SvException
	 */
	public JsonArray sortAndPrepareCustomJsonArray(DbDataArray dbArr, JsonObject jObj, String tableName, Writer wr)
			throws SvException {
		JsonArray jArr = new JsonArray();
		DbDataArray sortedDesc = new DbDataArray();
		DbDataArray result = sortDbDataArrayByDtInsertCriteria(dbArr);
		for (int i = result.size() - 1; i >= 0; i--) {
			sortedDesc.addDataItem(result.get(i));
		}
		for (DbDataObject dbo : sortedDesc.getItems()) {
			switch (tableName) {
			case SN.SUBJECT:
				jObj = wr.createSubjectJson(dbo);
				break;
			case SN.MESSAGE:
				jObj = wr.createMessageJson(dbo);
				break;
			default:
				break;
			}
			jArr.add(jObj);
		}
		return jArr;
	}
	
	public List<Long> getSubjectIds(DbDataArray dbArrMessages) {
		List<Long> subjectIds = new ArrayList<>();
		if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
			for (DbDataObject dboTempMessage : dbArrMessages.getItems()) {
				subjectIds.add(dboTempMessage.getParentId());
			}
		}
		return subjectIds;
	}

	public List<Long> removeDuplicates(List<Long> subjectIds) {
		List<Long> result = new ArrayList<>();
		HashSet<Long> hs = new HashSet<>();
		if (subjectIds != null && !subjectIds.isEmpty()) {
			for (Long subjectObjId : subjectIds) {
				hs.add(subjectObjId);
			}
			for (Long currentItem : hs) {
				result.add(currentItem);
			}
		}
		return result;
	}

	public DbDataArray prepareDbDataArrayFromList(List<Long> subjectIds, SvReader svr) throws SvException {
		DbDataArray arr = new DbDataArray();
		for (Long temp : subjectIds) {
			DbDataObject dbo = svr.getObjectById(temp, SvReader.getTypeIdByName(SN.SUBJECT), null);
			if (dbo != null) {
				arr.addDataItem(dbo);
			}
		}
		return arr;
	}

	/**
	 * Method that return objects from defined interval
	 * 
	 * @param arr
	 *            DbDataArray of objects to return
	 * @param from
	 *            Start of the interval
	 * @param to
	 *            End of the interval
	 * @param subjectStatus
	 *            Status of the subject
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getObjectsFromInterval(DbDataArray arr, int from, int to, String subjectStatus)
			throws SvException {
		DbDataArray result = new DbDataArray();
		DbDataArray sortedDesc = new DbDataArray();
		DbDataArray sortedAsc = sortDbDataArrayByDtInsertCriteria(arr);
		for (int i = sortedAsc.size() - 1; i >= 0; i--) {
			sortedDesc.addDataItem(sortedAsc.get(i));
		}
		for (int i = from; i < to && i < sortedDesc.size(); i++) {
			if (sortedDesc.get(i).getStatus().equals(subjectStatus)) {
				result.addDataItem(sortedDesc.get(i));
			}
		}
		return result;
	}
	
	/**
	 * Method that return sent messages by user object ID
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public DbDataArray getSentMessages(DbDataObject dboUser, SvReader svr) throws SvException {
		DbSearchCriterion cr1 = new DbSearchCriterion(SN.CREATED_BY, DbCompareOperand.EQUAL, dboUser.getObjectId());
		return svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1), SvReader.getTypeIdByName(SN.MESSAGE), null,
				0, 0);
	}
	
	/**
	 * Method that return number of total SENT subjects
	 * 
	 * @param userObjId
	 *            Object ID of the current user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SQLException
	 * @throws SvException
	 */
	public int getNumberOfSentSubjects(Long userObjId, SvReader svr) throws SQLException, SvException {
		int total = 0;
		Connection conn = svr.dbGetConn();
		String countOfSentSubject = "SELECT COUNT (DISTINCT tbl0.TITLE) as TOTAL " + "FROM PDNA.VSUBJECT as tbl0 "
				+ "JOIN PDNA.VMESSAGE as tbl1 ON tbl0.object_id = tbl1.parent_id "
				+ "WHERE  ( (tbl0.DT_DELETE > now()) " + "AND (tbl1.DT_DELETE > now()) " + "AND (tbl1.CREATED_BY = ?))";
		ResultSet rs = null;
		try (PreparedStatement selectStatement = conn.prepareStatement(countOfSentSubject, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);) {
			selectStatement.setLong(1, userObjId);

			rs = selectStatement.executeQuery();
			while (rs.next()) {
				total = rs.getInt(SN.TOTAL);
			}
		} catch (Exception e) {
			throw (new SvException(SN.svarog_notifications_ERROR_GETTING_SELECTED_RESULT, svr.getInstanceUser(), e));
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				log4j.error(new SvException(SN.svarog_notifications_ERROR_GETTING_SELECTED_RESULT, svr.getInstanceUser(), e));
			}
		}
		return total;
	}
	
	/**
	 * Method that return number of total INBOX subjects
	 * 
	 * @param userName
	 *            Username of the current user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SQLException
	 * @throws SvException
	 */
	public int getNumberOfInboxSubjects(String userName, SvReader svr) throws SQLException, SvException {
		int total = 0;
		Connection conn = svr.dbGetConn();
		String countOfInboxSubjects = "SELECT COUNT (DISTINCT vs.TITLE) as TOTAL " + "FROM PDNA.VSUBJECT as vs "
				+ "JOIN PDNA.VMESSAGE as vm ON vs.object_id = vm.parent_id "
				+ "JOIN PDNA.VSVAROG_LINK as vsl ON vm.object_id = vsl.link_obj_id_1 "
				+ "JOIN PDNA.VSVAROG_LINK_TYPE as vslt ON vsl.link_type_id = vslt.object_id "
				+ "JOIN PDNA.VSVAROG_USERS as vsu ON vsl.link_obj_id_2 = vsu.object_id "
				+ "WHERE ( (vs.DT_DELETE > now()) " + "AND (vm.DT_DELETE > now()) " + "AND (vsl.DT_DELETE > now()) "
				+ "AND (vslt.DT_DELETE > now()) " + "AND vsu.DT_DELETE > now()) " + "AND (vsu.USER_NAME = ?) "
				+ "AND (vslt.LINK_TYPE IN ('MSG_TO', 'MSG_CC', 'MSG_BCC'))";
		ResultSet rs = null;
		try (PreparedStatement selectStatement = conn.prepareStatement(countOfInboxSubjects,
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);) {
			selectStatement.setString(1, userName);

			rs = selectStatement.executeQuery();
			while (rs.next()) {
				total = rs.getInt(SN.TOTAL);
			}
		} catch (Exception e) {
			throw (new SvException(SN.svarog_notifications_ERROR_GETTING_SELECTED_RESULT, svr.getInstanceUser(), e));
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				log4j.error(new SvException(SN.svarog_notifications_ERROR_GETTING_SELECTED_RESULT, svr.getInstanceUser(), e));
			}
		}
		return total;
	}
	
	/**
	 * Method that return number of total ARCHIVED subjects
	 * 
	 * @param userName
	 *            Username of the current user
	 * @param userObjId
	 *            Object ID of the current user
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SQLException
	 * @throws SvException
	 */
	public int getNumberOfArchivedSubjects(String userName, Long userObjId, SvReader svr)
			throws SQLException, SvException {
		int total = 0;
		Connection conn = svr.dbGetConn();
		String countOfSentSubject = "SELECT COUNT (DISTINCT vs.TITLE) as TOTAL " + "FROM PDNA.VSUBJECT as vs "
				+ "JOIN PDNA.VMESSAGE as vm ON vs.object_id = vm.parent_id "
				+ "JOIN PDNA.VSVAROG_LINK as vsl ON vm.object_id = vsl.link_obj_id_1 "
				+ "JOIN PDNA.VSVAROG_LINK_TYPE as vslt ON vsl.link_type_id = vslt.object_id "
				+ "JOIN PDNA.VSVAROG_USERS as vsu ON vsl.link_obj_id_2 = vsu.object_id "
				+ "WHERE ( (vs.DT_DELETE > now()) " + "AND (vm.DT_DELETE > now()) " + "AND (vsl.DT_DELETE > now()) "
				+ "AND (vslt.DT_DELETE > now()) " + "AND vsu.DT_DELETE > now()) "
				+ "AND ((vsu.USER_NAME = ? OR vm.CREATED_BY = ?)) "
				+ "AND (vslt.LINK_TYPE IN ('MSG_TO', 'MSG_CC', 'MSG_BCC'))" + "AND vs.STATUS = 'CLOSED'";
		ResultSet rs = null;
		try (PreparedStatement selectStatement = conn.prepareStatement(countOfSentSubject, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);) {
			selectStatement.setString(1, userName);
			selectStatement.setLong(2, userObjId);
			rs = selectStatement.executeQuery();
			while (rs.next()) {
				total = rs.getInt(SN.TOTAL);
			}
		} catch (Exception e) {
			throw (new SvException(SN.svarog_notifications_ERROR_GETTING_SELECTED_RESULT, svr.getInstanceUser(), e));
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				log4j.error(new SvException(SN.svarog_notifications_ERROR_GETTING_SELECTED_RESULT, svr.getInstanceUser(), e));
			}
		}
		return total;
	}
}
