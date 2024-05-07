package com.prtech.svarog_notifications;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataObject;

public class Message {
	static final Logger log4j = LogManager.getLogger(Message.class.getName());
	public String text;
	public Long createdBy;
	public String createdByUserName;
	public String priority;

	public Message(String text, Long createdBy, String createdByUserName, String priority) {
		this.text = text;
		this.createdBy = createdBy;
		this.createdByUserName = createdByUserName;
		this.priority = priority;
	}

	public String getText() {
		return text;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public String getCreatedByUserName() {
		return createdByUserName;
	}

	public String getPriority() {
		return priority;
	}

	public DbDataObject createMessage(DbDataObject dboSubject) throws SvException {
		DbDataObject dbo = new DbDataObject();
		dbo.setObjectType(SvReader.getTypeIdByName(SN.MESSAGE));
		if (dboSubject != null) {
			dbo.setParentId(dboSubject.getObjectId());
			if (priority != null && !priority.isEmpty()) {
				dbo.setVal(SN.PRIORITY, priority);
			} else {
				dbo.setVal(SN.PRIORITY, dboSubject.getVal(SN.PRIORITY).toString());
			}
		}
		dbo.setVal(SN.TEXT, text);
		dbo.setVal(SN.CREATED_BY, createdBy);
		dbo.setVal(SN.CREATED_BY_USERNAME, createdByUserName);
		return dbo;
	}
}