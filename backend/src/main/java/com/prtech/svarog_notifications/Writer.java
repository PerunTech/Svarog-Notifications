package com.prtech.svarog_notifications;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;


public class Writer {

	static final Logger log4j = LogManager.getLogger(Writer.class.getName());
	
	/**
	 * Method that create Subject from form
	 * 
	 * @param moduleName
	 *            Name of the module for which this subject is created
	 *            (codelist)
	 * @param title
	 *            Title of the message thread / subject
	 * @param category
	 *            Name of the category for which this subject is created
	 *            (codelist)
	 * @param priority
	 *            Level of priority for this subject (codelist)
	 * @param svw
	 *            SvWriter instance
	 * @return
	 */
	public DbDataObject createSubject(String moduleName, String title, String category, String priority, SvWriter svw) {
		DbDataObject dboSubject = null;
		try {
			if (moduleName == null || moduleName.trim().equals(SN.EMPTY_STRING) || title == null
					|| title.trim().equals(SN.EMPTY_STRING) || category == null
					|| category.trim().equals(SN.EMPTY_STRING) || priority == null
					|| priority.trim().equals(SN.EMPTY_STRING)) {
				throw new SvException(SN.SVAROG_NOTIFICATIONS_ERROR_MANDATORY_FIELDS_ARE_MISSING, svw.getInstanceUser());
			}
			dboSubject = new DbDataObject();
			dboSubject.setObjectType(SvReader.getTypeIdByName(SN.SUBJECT));
			dboSubject.setVal(SN.MODULE_NAME, moduleName);
			dboSubject.setVal(SN.TITLE, title);
			dboSubject.setVal(SN.CATEGORY, category);
			dboSubject.setVal(SN.PRIORITY, priority);
			svw.saveObject(dboSubject, false);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return dboSubject;
	}
	
	/**
	 * Method that create Message from form
	 * 
	 * @param text
	 *            Text of the message
	 * @param priority
	 *            Priority of the message (if null inherits from the subject)
	 * @param dboSubject
	 *            DbDataObject of the subject
	 * @param svw
	 *            SvWriter instance
	 * @return
	 */
	public DbDataObject createMessage(String text, String priority, DbDataObject dboSubject, SvWriter svw) {
		DbDataObject dboMessage = null;
		try {
			if (text == null || text.trim().equals(SN.EMPTY_STRING) || dboSubject == null) {
				throw new SvException(SN.SVAROG_NOTIFICATIONS_ERROR_MANDATORY_FIELDS_ARE_MISSING, svw.getInstanceUser());
			}
			dboMessage = new DbDataObject();
			dboMessage.setObjectType(SvReader.getTypeIdByName(SN.MESSAGE));
			dboMessage.setParentId(dboSubject.getObjectId());
			dboMessage.setVal(SN.TEXT, text);
			if (priority != null && !priority.isEmpty()) {
				dboMessage.setVal(SN.PRIORITY, priority);
			} else {
				dboMessage.setVal(SN.PRIORITY, dboSubject.getVal(SN.PRIORITY).toString());
			}
			// get sender data
			dboMessage.setVal(SN.CREATED_BY, SvReader.getUserBySession(svw.getSessionId()).getObjectId());
			dboMessage.setVal(SN.CREATED_BY_USERNAME,
					SvReader.getUserBySession(svw.getSessionId()).getVal(SN.USER_NAME));
			svw.saveObject(dboMessage, false);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return dboMessage;
	}
	
	/**
	 * Pre step method of assigning message to user/s
	 * 
	 * @param dboMessage
	 *            DbDataObject of message
	 * @param msgTo
	 *            List of direct recipients [XXX,YYY,ZZZ]; if empty []
	 * @param msgCc
	 *            List of CC recipients [XXX,YYY,ZZZ]; if empty []
	 * @param msgBcc
	 *            List of BCC recipients [XXX,YYY,ZZZ]; if empty []
	 * @param svw
	 *            SvWriter instance
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
//	public DbDataArray assignMessageToUsers(DbDataObject dboMessage, String msgTo, String msgCc, String msgBcc,
//			SvWriter svw, SvReader svr) throws SvException {
//		Reader rdr = new Reader();
//		DbDataArray dbArr = null;
//		// process recipients data
//		dbArr = createLinkBetweenMessageAndUser(dbArr, dboMessage, SN.MSG_TO, rdr.convertStringIntoLongList(msgTo), svw, svr);
//		dbArr = createLinkBetweenMessageAndUser(dbArr, dboMessage, SN.MSG_CC, rdr.convertStringIntoLongList(msgCc), svw, svr);
//		dbArr = createLinkBetweenMessageAndUser(dbArr, dboMessage, SN.MSG_BCC, rdr.convertStringIntoLongList(msgBcc), svw,
//				svr);
//		return dbArr;
//	}
	
	/**
	 * Method that create Message from form
	 * 
	 * @param dboMessage
	 *            Carries dboMsgData
	 * @param svw
	 *            SvWriter instance
	 * @param svr
	 *            SvReader instance
	 * @return
	 */
	public DbDataObject processMessageAttachmentInfo(String msgAttachmentInfo, DbDataObject dboMessage, SvWriter svw,
			SvReader svr) {
		DbDataObject dboMessageAttch = null;
		String atchName = null;
		Long atchObjId = null;
		Long atchObjType = null;
		if (msgAttachmentInfo != null && !msgAttachmentInfo.trim().equals(SN.EMPTY_STRING)) {
			msgAttachmentInfo = msgAttachmentInfo.substring(1, msgAttachmentInfo.length() - 1);
			String[] msgAttachObjs = msgAttachmentInfo.split("},");
			for (String tempAtachObj : msgAttachObjs) {
				tempAtachObj = tempAtachObj.replace("{", "");
				tempAtachObj = tempAtachObj.replace("}", "");
				tempAtachObj = tempAtachObj.replace("\"", "");
				String[] tempAtachObjPropsProps = tempAtachObj.split(SN.COMMA_SEPARATOR);
				for (String tempAttchProp : tempAtachObjPropsProps) {
					String[] tempAtchElement = tempAttchProp.split(":");
					switch (tempAtchElement[0]) {
					case SN.NAME:
						atchName = tempAtchElement[1];
						break;
					case SN.ATCH_OBJ_ID:
						atchObjId = Long.valueOf(tempAtchElement[1]);
						break;
					case SN.ATCH_OBJ_TYPE:
						atchObjType = Long.valueOf(tempAtchElement[1]);
						break;
					default:
						break;
					}
				}
				dboMessageAttch = createMessageAttachment(dboMessage, atchName, atchObjId, atchObjType, svw, svr);
			}
		}
		return dboMessageAttch;
	}
	
	/**
	 * Method that create message attachment object
	 * 
	 * @param dboMessage
	 *            DbDataObject of the message
	 * @param atchName
	 *            Attachment name
	 * @param atchObjId
	 *            Attachment object ID
	 * @param atchObjType
	 *            Attachment object type
	 * @param svw
	 *            SvWriter instance
	 * @param svr
	 *            SvReader instance
	 */
	public DbDataObject createMessageAttachment(DbDataObject dboMessage, String atchName, Long atchObjId,
			Long atchObjType, SvWriter svw, SvReader svr) {
		DbDataObject dboMsgAttachment = null;
		try {
			dboMsgAttachment = new DbDataObject();
			dboMsgAttachment.setObjectType(SvReader.getTypeIdByName(SN.MSG_ATTACHEMENT));
			if (dboMessage == null || atchName == null || atchName.trim().equals(SN.EMPTY_STRING)) {
				throw new SvException(SN.SVAROG_NOTIFICATIONS_ERROR_MANDATORY_FIELDS_ARE_MISSING, svr.getInstanceUser());
			}
			dboMsgAttachment.setVal(SN.MSG_ID, dboMessage.getObjectId());
			dboMsgAttachment.setVal(SN.ATCH_OBJ_TYPE, atchObjType);
			dboMsgAttachment.setVal(SN.ATCH_OBJ_ID, atchObjId);
			dboMsgAttachment.setVal(SN.NAME, atchName);
			svw.saveObject(dboMsgAttachment);
			dboMessage.setVal(SN.HAS_ATTACHMENT, true);
			svw.saveObject(dboMessage);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return dboMsgAttachment;
	}

	/**
	 * Method that sends automatic message to users when the subject has been
	 * closed (archived)
	 * 
	 * @param dboSubject
	 *            DbDataObject of subject
	 * @param svw
	 *            SvWriter instance
	 * @param svr
	 *            SvReader instance
	 * @throws SvException
	 */
	public void sendAutomaticMessage(DbDataObject dboSubject, SvWriter svw, SvReader svr) throws SvException {
		DbDataArray dbArrUsersTo = null;
		DbDataArray dbArrUsersCc = null;
		DbDataArray dbArrUsersBcc = null;
		ArrayList<Long> convertedArrayListFromHashSet = null;
		HashSet<Long> objIds = new HashSet<>();
		Reader rdr = new Reader();
		DbDataArray dbArrMessages = svr.getObjectsByParentId(dboSubject.getObjectId(),
				SvReader.getTypeIdByName(SN.MESSAGE), null, 0, 0);
		if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
			for (DbDataObject dboMessage : dbArrMessages.getItems()) {
				dbArrUsersTo = rdr.getUsersLinkedToMessage(dboMessage, SN.MSG_TO, svr);
				dbArrUsersCc = rdr.getUsersLinkedToMessage(dboMessage, SN.MSG_CC, svr);
				dbArrUsersBcc = rdr.getUsersLinkedToMessage(dboMessage, SN.MSG_BCC, svr);
				if (!dbArrUsersTo.getItems().isEmpty()) {
					for (DbDataObject dbo : dbArrUsersTo.getItems()) {
						objIds.add(dbo.getObjectId());
					}
				}
				if (!dbArrUsersCc.getItems().isEmpty()) {
					for (DbDataObject dbo : dbArrUsersCc.getItems()) {
						if (objIds.add(dbo.getObjectId())) {
							objIds.add(dbo.getObjectId());
						}
					}
				}
				if (!dbArrUsersBcc.getItems().isEmpty()) {
					for (DbDataObject dbo : dbArrUsersBcc.getItems()) {
						if (objIds.add(dbo.getObjectId())) {
							objIds.add(dbo.getObjectId());
						}
					}
				}
				convertedArrayListFromHashSet = new ArrayList<>(objIds);
			}
			DbDataObject automaticMessage = createAutomaticMessage(
					"This is automatically sent message to inform you that this subject has been archived", null,
					dboSubject, svw);
			if (automaticMessage != null) {
				svw.dbCommit();
				createLinkBetweenMessageAndUser(automaticMessage, SN.MSG_TO, convertedArrayListFromHashSet, svw, svr);
			}
		}
	}
	
	/**
	 * Method that update status of link between message and user
	 * 
	 * @param dboUser
	 *            DbDataObject of user
	 * @param messageObjId
	 *            Object id of the message
	 * @param svw
	 *            SvWorkflow instance
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public boolean updateStatusOfLinkBetweenMessageAndUser(DbDataObject dboUser, Long messageObjId, SvWriter svw,
			SvReader svr) throws SvException {
		boolean isUpdated = false;
		DbDataArray dbArr = null;
		DbDataObject dboMessage = svr.getObjectById(messageObjId, SvReader.getTypeIdByName(SN.MESSAGE), new DateTime());
		DbSearchCriterion cr1 = new DbSearchCriterion(SN.STATUS, DbCompareOperand.EQUAL, SN.UNSEEN);
		DbSearchCriterion cr2 = new DbSearchCriterion(SN.LINK_OBJ_ID_1, DbCompareOperand.EQUAL,
				dboMessage.getObjectId());
		DbSearchCriterion cr3 = new DbSearchCriterion(SN.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, dboUser.getObjectId());
		dbArr = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2).addDbSearchItem(cr3),
				svCONST.OBJECT_TYPE_LINK, new DateTime(), 0, 0);
		if (dbArr != null && !dbArr.getItems().isEmpty()) {
			Long linkTypeId = (Long) dbArr.get(0).getVal(SN.LINK_TYPE_ID);
			invalidateLink(dbArr.get(0), false, svr);
			DbDataObject dboLinkType = svr.getObjectById(linkTypeId, svCONST.OBJECT_TYPE_LINK_TYPE, null);
			if (dboLinkType != null) {
				DbDataObject dboLink = createSvarogLink(dboLinkType.getObjectId(), dboMessage, dboUser);
				dboLink.setStatus(SN.VALID);
				svw.saveObject(dboLink);
				svw.dbCommit();
				isUpdated = true;
			}
		}
		return isUpdated;
	}
	
	/**
	 * Method that link message and user by different type of links(to,bc,cc)
	 * 
	 * @param dboMessage
	 *            Message object
	 * @param linkTypeId
	 *            link type id
	 * @param recipientsUserObjIds
	 *            Users objIds to link to
	 * @param svw
	 *            SvWriter instance
	 * @param svr
	 *            SvReader instance
	 * @return
	 * @throws SvException
	 */
	public void createLinkBetweenMessageAndUser(DbDataObject dboMessage, String linkTypeId,
			List<Long> recipientsUserObjIds, SvWriter svw, SvReader svr) throws SvException {
		DbDataArray dbArr = new DbDataArray();
		DbDataObject dboUser = null;
		DbDataObject dbLinkMessageAndUser = SvReader.getLinkType(linkTypeId, SvReader.getTypeIdByName(SN.MESSAGE),
				svCONST.OBJECT_TYPE_USER);
		if (recipientsUserObjIds != null && !recipientsUserObjIds.isEmpty()) {
			for (Long tempObjId : recipientsUserObjIds) {
				dboUser = svr.getObjectById(tempObjId, svCONST.OBJECT_TYPE_USER, null);
				if (dboMessage != null && dboUser != null) {
					DbDataObject dboLink = createSvarogLink(dbLinkMessageAndUser.getObjectId(), dboMessage, dboUser);
					dboLink.setStatus(SN.UNSEEN);
					dbArr.addDataItem(dboLink);
				}
			}
		}
		svw.saveObject(dbArr, true, true);
	}
	
	public Boolean invalidateLink(DbDataObject dboLink, SvCore parentCore) {
		return invalidateLink(dboLink, true, parentCore);
	}

	public Boolean invalidateLink(DbDataObject dboLink, Boolean autoCommit, SvCore parentCore) {
		Boolean result = false;
		try (SvWriter svw = new SvWriter(parentCore)) {
			if (dboLink != null) {
				svw.deleteObject(dboLink, autoCommit);
				svw.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return result;
	}
	
	public DbDataObject createSvarogLink(Long linkTypeObjId, DbDataObject dbo1, DbDataObject dbo2) {
		DbDataObject dbLink = new DbDataObject();
		dbLink.setObjectType(svCONST.OBJECT_TYPE_LINK);
		dbLink.setVal(SN.LINK_TYPE_ID, linkTypeObjId);
		dbLink.setVal(SN.LINK_OBJ_ID_1, dbo1.getObjectId());
		dbLink.setVal(SN.LINK_OBJ_ID_2, dbo2.getObjectId());
		return dbLink;
	}
	
	/**
	 * Method that create link between message and responsible users if there
	 * are multiple or directly assign message if there is only one user.
	 * 
	 * @param dboMessage
	 *            DbDataObject of message
	 * @param responsibleUsers
	 *            DbDataArray of responsible users
	 * @param svw
	 *            SvWriter instance
	 * @throws SvException
	 */
	public void createLinkBetweenMessageAndUserResponsibleToOrgUnit(DbDataObject dboMessage,
			DbDataArray responsibleUsers, SvWriter svw) throws SvException {
		DbDataArray dbArr = new DbDataArray();
		DbDataObject dbLinkMessageAndUser = SvReader.getLinkType(SN.MSG_TO, SvReader.getTypeIdByName(SN.MESSAGE),
				svCONST.OBJECT_TYPE_USER);
		if (responsibleUsers != null && !responsibleUsers.getItems().isEmpty()) {
			for (DbDataObject dboTempResponsibleUser : responsibleUsers.getItems()) {
				if (dboMessage != null && dboTempResponsibleUser != null) {
					DbDataObject dboLink = createSvarogLink(dbLinkMessageAndUser.getObjectId(), dboMessage,
							dboTempResponsibleUser);
					dbArr.addDataItem(dboLink);
				}
			}
			svw.saveObject(dbArr, true, true);
		}
	}
	
	/**
	 * Method that create message that is automatically sent to all users in
	 * that subject, to inform them that subject was archived
	 * 
	 * @param text
	 *            Message text
	 * @param priority
	 *            Message priority
	 * @param dboSubject
	 *            DbDataObject of subject
	 * @param svw
	 *            SvWriter instance
	 * @return
	 */
	public DbDataObject createAutomaticMessage(String text, String priority, DbDataObject dboSubject, SvWriter svw) {
		DbDataObject dboMessage = null;
		try {
			if (text == null || text.trim().equals(SN.EMPTY_STRING) || dboSubject == null) {
				throw new SvException(SN.SVAROG_NOTIFICATIONS_ERROR_MANDATORY_FIELDS_ARE_MISSING, svw.getInstanceUser());
			}
			dboMessage = new DbDataObject();
			dboMessage.setObjectType(SvReader.getTypeIdByName(SN.MESSAGE));
			dboMessage.setParentId(dboSubject.getObjectId());
			dboMessage.setVal(SN.TEXT, text);
			if (priority != null && !priority.isEmpty()) {
				dboMessage.setVal(SN.PRIORITY, priority);
			} else {
				dboMessage.setVal(SN.PRIORITY, dboSubject.getVal(SN.PRIORITY).toString());
			}
			dboMessage.setVal(SN.CREATED_BY, SvReader.getUserBySession(svw.getSessionId()).getObjectId());
			dboMessage.setVal(SN.CREATED_BY_USERNAME,
					SvReader.getUserBySession(svw.getSessionId()).getVal(SN.USER_NAME));
			svw.saveObject(dboMessage, false);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return dboMessage;
	}
	
	public JsonObject createSubjectJson(DbDataObject dbo) throws SvException {
		JsonObject job = new JsonObject();
		job.addProperty("SUBJECT.PKID", dbo.getPkid());
		job.addProperty("SUBJECT.OBJECT_ID", dbo.getObjectId());
		job.addProperty("SUBJECT.PARENT_ID", dbo.getParentId());
		job.addProperty("SUBJECT.OBJECT_TYPE", dbo.getObjectType());
		job.addProperty("SUBJECT.DT_INSERT", dbo.getDtInsert().toString());
		job.addProperty("SUBJECT.STATUS", dbo.getStatus());
		if (dbo.getVal(SN.TITLE) != null) {
			job.addProperty("SUBJECT.TITLE", dbo.getVal(SN.TITLE).toString());
		}
		if (dbo.getVal(SN.MODULE_NAME) != null) {
			job.addProperty("SUBJECT.MODULE_NAME", dbo.getVal(SN.MODULE_NAME).toString());
		}
		if (dbo.getVal(SN.CATEGORY) != null) {
			job.addProperty("SUBJECT.CATEGORY", dbo.getVal(SN.CATEGORY).toString());
		}
		if (dbo.getVal(SN.PRIORITY) != null) {
			job.addProperty("SUBJECT.PRIORITY", dbo.getVal(SN.PRIORITY).toString());
		}
		job.addProperty("SUBJECT.ARCHIVE", SN.EMPTY_STRING);
		return job;
	}
	
	public JsonObject createMessageJson(DbDataObject dbo) throws SvException {
		JsonObject job = new JsonObject();
		job.addProperty("MESSAGE.PKID", dbo.getPkid());
		job.addProperty("MESSAGE.OBJECT_ID", dbo.getObjectId());
		job.addProperty("MESSAGE.PARENT_ID", dbo.getParentId());
		job.addProperty("MESSAGE.OBJECT_TYPE", dbo.getObjectType());
		job.addProperty("MESSAGE.DT_INSERT", dbo.getDtInsert().toString());
		job.addProperty("MESSAGE.STATUS", dbo.getStatus());
		if (dbo.getVal(SN.TEXT) != null) {
			job.addProperty("MESSAGE.TEXT", dbo.getVal(SN.TEXT).toString());
		}
		if (dbo.getVal(SN.PRIORITY) != null) {
			job.addProperty("MESSAGE.PRIORITY", dbo.getVal(SN.PRIORITY).toString());
		}
		if (dbo.getVal(SN.CREATED_BY_USERNAME) != null) {
			job.addProperty("MESSAGE.CREATED_BY_USERNAME", dbo.getVal(SN.CREATED_BY_USERNAME).toString());
		}
		if (dbo.getVal(SN.HAS_ATTACHMENT) != null) {
			job.addProperty("MESSAGE.PRIORITY", dbo.getVal(SN.HAS_ATTACHMENT).toString());
		}
		return job;
	}
}
