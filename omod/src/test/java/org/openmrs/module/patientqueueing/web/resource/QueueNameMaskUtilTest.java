package org.openmrs.module.patientqueueing.web.resource;

import org.junit.Assert;
import org.junit.Test;

/**
 * Regression tests for the name masking applied to every public-facing queue response. These pin
 * the contract the kiosk, display and self check-in endpoints rely on: only initials survive.
 */
public class QueueNameMaskUtilTest {
	
	@Test
	public void maskPatientName_shouldMaskTwoPartName() {
		Assert.assertEquals("J. D***", QueueNameMaskUtil.maskPatientName("John Doe"));
	}
	
	@Test
	public void maskPatientName_shouldMaskThreePartName() {
		Assert.assertEquals("M. J. S***", QueueNameMaskUtil.maskPatientName("Mary Jane Smith"));
	}
	
	@Test
	public void maskPatientName_shouldMaskSinglePartName() {
		Assert.assertEquals("J***", QueueNameMaskUtil.maskPatientName("Jane"));
	}
	
	@Test
	public void maskPatientName_shouldHandleExtraWhitespace() {
		Assert.assertEquals("J. D***", QueueNameMaskUtil.maskPatientName("  John   Doe  "));
	}
	
	@Test
	public void maskPatientName_shouldReturnNullForNull() {
		Assert.assertNull(QueueNameMaskUtil.maskPatientName(null));
	}
	
	@Test
	public void maskPatientName_shouldReturnNullForBlank() {
		Assert.assertNull(QueueNameMaskUtil.maskPatientName("   "));
	}
}
