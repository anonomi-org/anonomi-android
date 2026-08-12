package org.anonomi.android.util;

import android.os.SystemClock;

public class AndroidPasscodeClock implements PasscodeThrottle.Clock {

	@Override
	public long wallClockMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public long elapsedRealtimeMillis() {
		return SystemClock.elapsedRealtime();
	}
}
