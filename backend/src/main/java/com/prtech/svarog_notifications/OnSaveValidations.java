package com.prtech.svarog_notifications;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.ISvOnSave;

public class OnSaveValidations implements ISvOnSave {

	static final Logger log4j = LogManager.getLogger(OnSaveValidations.class.getName());

	static ArrayList<Long> handledSvTypes = null;

	static boolean isTypeHandled(Long typeId) {
		if (handledSvTypes == null) {
			handledSvTypes = new ArrayList<>();
		}
		return handledSvTypes.indexOf(typeId) >= 0;
	}

	@Override
	public boolean beforeSave(SvCore parentCore, DbDataObject dbo) throws SvException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void afterSave(SvCore parentCore, DbDataObject dbo) throws SvException {
		// TODO Auto-generated method stub

	}

}