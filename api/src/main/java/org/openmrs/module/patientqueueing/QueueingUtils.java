/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QueueingUtils {
	
	private QueueingUtils() {
	}
	
	/**
	 * This Method converts date to date format of set globalproperty
	 * patientqueueing.defaultDateFormat and adds time incase its set
	 * 
	 * @param date This is the date to be converted into set date format
	 * @param time The time String which is appended to the date in-case it is not null
	 * @return Date which is converted to required format
	 * @throws ParseException
	 */
	public static Date convertDateToDefaultDateFormat(Date date, String time) throws ParseException {
		AdministrationService administrationService = Context.getAdministrationService();
		SimpleDateFormat formatter = new SimpleDateFormat(
		        administrationService.getGlobalProperty("patientqueueing.displayDateFormat"));
		String formattedDate;
		SimpleDateFormat formatterExt = new SimpleDateFormat(
		        administrationService.getGlobalProperty("patientqueueing.defaultDateFormat"));
		if (time != null) {
			formattedDate = formatterExt.format(date) + " " + time;
		} else {
			formattedDate = formatterExt.format(date);
		}
		
		return formatter.parse(formattedDate);
	}
	
	/**
	 * This Method converts date to a string date format.
	 * 
	 * @param date This is the date to be converted into set date format and returns a string of
	 *            that format
	 * @param time The time String which is appended to the date in-case it is not null
	 * @return String date which is converted to required format
	 */
	public static String convertDateToDefaultDateFormatAsString(Date date, String time) {
		
		SimpleDateFormat formatterExt = new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		    "patientqueueing.defaultDateFormat"));
		
		String formattedDate = "";
		
		if (time != null) {
			formattedDate = formatterExt.format(date) + " " + time;
		} else {
			formattedDate = formatterExt.format(date);
		}
		return formattedDate;
	}
}
