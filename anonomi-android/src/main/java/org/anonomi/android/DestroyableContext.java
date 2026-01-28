package org.anonomi.android;

public interface DestroyableContext {

	void runOnUiThreadUnlessDestroyed(Runnable runnable);
}
