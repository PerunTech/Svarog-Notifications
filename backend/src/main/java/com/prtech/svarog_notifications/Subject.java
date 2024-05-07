package com.prtech.svarog_notifications;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataObject;

public class Subject {
	static final Logger log4j = LogManager.getLogger(Subject.class.getName());
	public String moduleName;
	public String title;
	public String category;
	public String priority;

	public Subject(String moduleName, String title, String category, String priority) {
		this.moduleName = moduleName;
		this.title = title;
		this.category = category;
		this.priority = priority;
	}

	public String getTitle() {
		return title;
	}

	public String getCategory() {
		return category;
	}

	public String getPriority() {
		return priority;
	}
	
	public String getModuleName() {
		return moduleName;
	}

	public DbDataObject createSubject() throws SvException {
		DbDataObject dbo = new DbDataObject();
		dbo.setObjectType(SvReader.getTypeIdByName(SN.SUBJECT));
		dbo.setVal(SN.MODULE_NAME, moduleName);
		dbo.setVal(SN.TITLE, title);
		dbo.setVal(SN.CATEGORY, category);
		dbo.setVal(SN.PRIORITY, priority);
		return dbo;
	}
}
