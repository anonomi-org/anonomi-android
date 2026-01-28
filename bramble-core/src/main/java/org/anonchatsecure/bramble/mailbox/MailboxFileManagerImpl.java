package org.anonchatsecure.bramble.mailbox;

import org.anonchatsecure.bramble.api.connection.ConnectionManager;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.mailbox.MailboxDirectory;
import org.anonchatsecure.bramble.api.plugin.PluginManager;
import org.anonchatsecure.bramble.api.plugin.TransportConnectionReader;
import org.anonchatsecure.bramble.api.plugin.TransportConnectionWriter;
import org.anonchatsecure.bramble.api.plugin.event.TransportActiveEvent;
import org.anonchatsecure.bramble.api.plugin.simplex.SimplexPlugin;
import org.anonchatsecure.bramble.api.properties.TransportProperties;
import org.anonchatsecure.bramble.api.sync.OutgoingSessionRecord;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.api.lifecycle.LifecycleManager.LifecycleState.RUNNING;
import static org.anonchatsecure.bramble.api.mailbox.MailboxConstants.ID;
import static org.anonchatsecure.bramble.api.plugin.file.FileConstants.PROP_PATH;
import static org.anonchatsecure.bramble.util.IoUtils.delete;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.briarproject.nullsafety.NullSafety.requireNonNull;

@ThreadSafe
@NotNullByDefault
class MailboxFileManagerImpl implements MailboxFileManager, EventListener {

	private static final Logger LOG =
			getLogger(MailboxFileManagerImpl.class.getName());

	// Package access for testing
	static final String DOWNLOAD_DIR_NAME = "downloads";
	static final String UPLOAD_DIR_NAME = "uploads";

	private final Executor ioExecutor;
	private final PluginManager pluginManager;
	private final ConnectionManager connectionManager;
	private final LifecycleManager lifecycleManager;
	private final File mailboxDir;
	private final EventBus eventBus;
	private final CountDownLatch orphanLatch = new CountDownLatch(1);

	@Inject
	MailboxFileManagerImpl(@IoExecutor Executor ioExecutor,
			PluginManager pluginManager,
			ConnectionManager connectionManager,
			LifecycleManager lifecycleManager,
			@MailboxDirectory File mailboxDir,
			EventBus eventBus) {
		this.ioExecutor = ioExecutor;
		this.pluginManager = pluginManager;
		this.connectionManager = connectionManager;
		this.lifecycleManager = lifecycleManager;
		this.mailboxDir = mailboxDir;
		this.eventBus = eventBus;
	}

	@Override
	public File createTempFileForDownload() throws IOException {
		return createTempFile(DOWNLOAD_DIR_NAME);
	}

	@Override
	public File createAndWriteTempFileForUpload(ContactId contactId,
			OutgoingSessionRecord sessionRecord) throws IOException {
		File f = createTempFile(UPLOAD_DIR_NAME);
		// We shouldn't reach this point until the plugin has been started
		SimplexPlugin plugin =
				(SimplexPlugin) requireNonNull(pluginManager.getPlugin(ID));
		TransportProperties p = new TransportProperties();
		p.put(PROP_PATH, f.getAbsolutePath());
		TransportConnectionWriter writer = plugin.createWriter(p);
		if (writer == null) {
			delete(f);
			throw new IOException();
		}
		MailboxFileWriter decorated = new MailboxFileWriter(writer);
		LOG.info("Writing file for upload");
		connectionManager.manageOutgoingConnection(contactId, ID, decorated,
				sessionRecord);
		if (decorated.awaitDisposal()) {
			// An exception was thrown during the session - delete the file
			delete(f);
			throw new IOException();
		}
		return f;
	}

	private File createTempFile(String dirName) throws IOException {
		// Wait for orphaned files to be handled before creating new files
		try {
			orphanLatch.await();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
		File dir = createDirectoryIfNeeded(dirName);
		return File.createTempFile("mailbox", ".tmp", dir);
	}

	private File createDirectoryIfNeeded(String name) throws IOException {
		File dir = new File(mailboxDir, name);
		//noinspection ResultOfMethodCallIgnored
		dir.mkdirs();
		if (!dir.isDirectory()) {
			throw new IOException("Failed to create directory '" + name + "'");
		}
		return dir;
	}

	@Override
	public void handleDownloadedFile(File f) {
		// We shouldn't reach this point until the plugin has been started
		SimplexPlugin plugin =
				(SimplexPlugin) requireNonNull(pluginManager.getPlugin(ID));
		TransportProperties p = new TransportProperties();
		p.put(PROP_PATH, f.getAbsolutePath());
		TransportConnectionReader reader = plugin.createReader(p);
		if (reader == null) {
			LOG.warning("Failed to create reader for downloaded file");
			return;
		}
		TransportConnectionReader decorated = new MailboxFileReader(reader, f);
		LOG.info("Reading downloaded file");
		connectionManager.manageIncomingConnection(ID, decorated,
				exception -> isHandlingComplete(exception, true));
	}

	private boolean isHandlingComplete(boolean exception, boolean recognised) {
		// If we've successfully read the file then we're done
		if (!exception && recognised) return true;
		// If the app is shutting down we may get spurious IO exceptions
		// due to executors being shut down. Leave the file in the download
		// directory and we'll try to read it again at the next startup
		return !lifecycleManager.getLifecycleState().isAfter(RUNNING);
	}

	@Override
	public void eventOccurred(Event e) {
		// Wait for the transport to become active before handling orphaned
		// files so that we can get the plugin from the plugin manager
		if (e instanceof TransportActiveEvent) {
			TransportActiveEvent t = (TransportActiveEvent) e;
			if (t.getTransportId().equals(ID)) {
				ioExecutor.execute(this::handleOrphanedFiles);
				eventBus.removeListener(this);
			}
		}
	}

	/**
	 * This method is called at startup, as soon as the plugin is started, to
	 * delete any files that were left in the upload directory at the last
	 * shutdown and handle any files that were left in the download directory.
	 */
	@IoExecutor
	private void handleOrphanedFiles() {
		try {
			File uploadDir = createDirectoryIfNeeded(UPLOAD_DIR_NAME);
			File[] orphanedUploads = uploadDir.listFiles();
			if (orphanedUploads != null) {
				for (File f : orphanedUploads) delete(f);
			}
			File downloadDir = createDirectoryIfNeeded(DOWNLOAD_DIR_NAME);
			File[] orphanedDownloads = downloadDir.listFiles();
			// Now that we've got the list of orphaned downloads, new files
			// can be created in the download directory
			orphanLatch.countDown();
			if (orphanedDownloads != null) {
				for (File f : orphanedDownloads) handleDownloadedFile(f);
			}
		} catch (IOException e) {
			logException(LOG, WARNING, e);
		}
	}

	private class MailboxFileReader implements TransportConnectionReader {

		private final TransportConnectionReader delegate;
		private final File file;

		private MailboxFileReader(TransportConnectionReader delegate,
				File file) {
			this.delegate = delegate;
			this.file = file;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return delegate.getInputStream();
		}

		@Override
		public void dispose(boolean exception, boolean recognised)
				throws IOException {
			delegate.dispose(exception, recognised);
			if (isHandlingComplete(exception, recognised)) {
				LOG.info("Deleting downloaded file");
				delete(file);
			}
		}
	}

	private static class MailboxFileWriter
			implements TransportConnectionWriter {

		private final TransportConnectionWriter delegate;
		private final BlockingQueue<Boolean> disposalResult =
				new ArrayBlockingQueue<>(1);

		private MailboxFileWriter(TransportConnectionWriter delegate) {
			this.delegate = delegate;
		}

		@Override
		public long getMaxLatency() {
			return delegate.getMaxLatency();
		}

		@Override
		public int getMaxIdleTime() {
			return delegate.getMaxIdleTime();
		}

		@Override
		public boolean isLossyAndCheap() {
			return delegate.isLossyAndCheap();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return delegate.getOutputStream();
		}

		@Override
		public void dispose(boolean exception) throws IOException {
			delegate.dispose(exception);
			disposalResult.add(exception);
		}

		/**
		 * Waits for the delegate to be disposed and returns true if an
		 * exception occurred.
		 */
		private boolean awaitDisposal() {
			try {
				return disposalResult.take();
			} catch (InterruptedException e) {
				LOG.info("Interrupted while waiting for disposal");
				return true;
			}
		}
	}
}
