package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.api.db.CommitAction;
import org.anonchatsecure.bramble.api.db.DbCallable;
import org.anonchatsecure.bramble.api.db.TaskAction;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

class RunTransactionWithResultAction implements Action {

	private final Transaction txn;

	RunTransactionWithResultAction(Transaction txn) {
		this.txn = txn;
	}

	@Override
	public Object invoke(Invocation invocation) throws Throwable {
		DbCallable task = (DbCallable) invocation.getParameter(1);
		Object result = task.call(txn);
		for (CommitAction action : txn.getActions()) {
			if (action instanceof TaskAction)
				((TaskAction) action).getTask().run();
		}
		return result;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("runs a task inside a database transaction");
	}
}
