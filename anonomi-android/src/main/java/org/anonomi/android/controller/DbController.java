package org.anonomi.android.controller;

import org.briarproject.nullsafety.NotNullByDefault;

@Deprecated
@NotNullByDefault
public interface DbController {

	void runOnDbThread(Runnable task);
}
