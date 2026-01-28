package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DbException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.db.JdbcUtils.tryToClose;

class Migration51_52 implements Migration<Connection> {

	private static final Logger LOG = getLogger(Migration51_52.class.getName());

	@Override
	public int getStartVersion() {
		return 51;
	}

	@Override
	public int getEndVersion() {
		return 52;
	}

	@Override
	public void migrate(Connection txn) throws DbException {
		Statement s = null;
		try {
			s = txn.createStatement();
			s.execute("ALTER TABLE contacts ADD COLUMN distorted_voice BOOLEAN DEFAULT FALSE");
		} catch (SQLException e) {
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}
}