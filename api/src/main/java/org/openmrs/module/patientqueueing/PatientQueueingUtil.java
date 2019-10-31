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

import java.util.ArrayList;
import java.util.List;

public class PatientQueueingUtil {
	
	/**
	 * Converts a delimited string by the delimiter parameter set to a list
	 * 
	 * @param delimitedString The string that contains items to be split
	 * @param delimiter the separator that will be used to split the sting into objects
	 * @return List a list of items after conversion
	 */
	public static List delimitedStringToList(String delimitedString, String delimiter) {
		List ret = new ArrayList();
		String[] tokens = delimitedString.split(delimiter);
		String[] arr$ = tokens;
		int len$ = tokens.length;
		
		for (int i$ = 0; i$ < len$; ++i$) {
			String token = arr$[i$];
			token = token.trim();
			if (token.length() != 0) {
				ret.add(token);
			}
		}
		return ret;
	}
}
