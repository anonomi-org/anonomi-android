package org.anonomi.android.panic;

import org.anonomi.android.panic.PanicSequenceDetector.LoadResult;
import org.anonomi.android.util.SecureValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reading the panic settings used to be wrapped in
 * {@code catch (Exception e) { enabled = false; }}, so any storage failure
 * turned the panic trigger off without telling anyone.
 */
public class PanicSequenceLoadTest {

	private static final String THREE_STEPS = "U:S,D:L,U:S";

	@Test
	public void unreadableSequenceIsReportedAsAnErrorNotAsUnconfigured() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.present("true"),
				SecureValue.unreadable("decryption failed"));
		assertTrue("A sequence that could not be read was passed off as no " +
				"sequence being configured", result.loadError);
		// There is no sequence left to match, so the detector cannot be armed
		// - but that must be visible rather than silent.
		assertFalse(result.enabled);
	}

	/**
	 * Nothing tells us the trigger was switched off, so it stays on.
	 */
	@Test
	public void unreadableEnabledFlagDoesNotDisablePanic() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.unreadable("decryption failed"),
				SecureValue.present(THREE_STEPS));
		assertTrue(result.enabled);
		assertTrue(result.loadError);
		assertEquals(3, result.sequence.size());
	}

	@Test
	public void absentSettingsAreNotAnError() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.absent(), SecureValue.absent());
		assertFalse("Never configuring panic was reported as a read failure",
				result.loadError);
		assertFalse(result.enabled);
	}

	@Test
	public void aStoredSequenceArmsTheDetector() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.absent(), SecureValue.present(THREE_STEPS));
		assertTrue(result.enabled);
		assertFalse(result.loadError);
		assertEquals(3, result.sequence.size());
	}

	@Test
	public void anExplicitlyDisabledTriggerStaysDisabled() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.present("false"),
				SecureValue.present(THREE_STEPS));
		assertFalse(result.enabled);
		assertFalse(result.loadError);
	}

	@Test
	public void tooShortASequenceDoesNotArmTheDetector() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.absent(), SecureValue.present("U:S,D:L"));
		assertFalse(result.enabled);
		assertFalse(result.loadError);
	}

	/**
	 * The rule the panic settings screen and the PTT conflict check share
	 * with the detector. Neither "never set" nor "cannot be read" is a reason
	 * to treat the trigger as switched off - only an explicit stored value
	 * that is not "true".
	 */
	@Test
	public void onlyAnExplicitStoredValueCanDisableTheTrigger() {
		assertTrue(PanicSequenceDetector
				.isTriggerEnabled(SecureValue.present("true")));
		assertFalse(PanicSequenceDetector
				.isTriggerEnabled(SecureValue.present("false")));
		assertTrue("Never setting the flag switched the trigger off",
				PanicSequenceDetector
						.isTriggerEnabled(SecureValue.absent()));
		assertTrue("An unreadable flag switched the trigger off",
				PanicSequenceDetector.isTriggerEnabled(
						SecureValue.unreadable("decryption failed")));
	}

	@Test
	public void anEmptySequenceIsNotAnError() {
		LoadResult result = PanicSequenceDetector.resolveLoad(
				SecureValue.absent(), SecureValue.present(""));
		assertFalse(result.enabled);
		assertFalse(result.loadError);
	}
}
