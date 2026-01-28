package org.anonomi.android.blog;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Patterns;

import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;
import org.anonomi.android.blog.RssImportResult.FileImportError;
import org.anonomi.android.blog.RssImportResult.FileImportSuccess;
import org.anonomi.android.blog.RssImportResult.UrlImportError;
import org.anonomi.android.blog.RssImportResult.UrlImportSuccess;
import org.anonomi.android.viewmodel.DbViewModel;
import org.anonomi.android.viewmodel.LiveEvent;
import org.anonomi.android.viewmodel.LiveResult;
import org.anonomi.android.viewmodel.MutableLiveEvent;
import org.anonchatsecure.anonchat.api.feed.Feed;
import org.anonchatsecure.anonchat.api.feed.FeedManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.LogUtils.now;

@NotNullByDefault
class RssFeedViewModel extends DbViewModel {

	private static final Logger LOG =
			getLogger(RssFeedViewModel.class.getName());

	private final FeedManager feedManager;
	private final Executor ioExecutor;
	private final Executor dbExecutor;

	private final MutableLiveData<LiveResult<List<Feed>>> feeds =
			new MutableLiveData<>();

	private final MutableLiveData<Boolean> isImporting =
			new MutableLiveData<>(false);
	private final MutableLiveEvent<RssImportResult> importResult =
			new MutableLiveEvent<>();

	@Inject
	RssFeedViewModel(Application app,
			FeedManager feedManager,
			@IoExecutor Executor ioExecutor,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor) {
		super(app, dbExecutor, lifecycleManager, db, androidExecutor);
		this.feedManager = feedManager;
		this.ioExecutor = ioExecutor;
		this.dbExecutor = dbExecutor;

		loadFeeds();
	}

	@Nullable
	String validateAndNormaliseUrl(String url) {
		if (!Patterns.WEB_URL.matcher(url).matches()) return null;
		try {
			return new URL(url).toString();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	LiveData<LiveResult<List<Feed>>> getFeeds() {
		return feeds;
	}

	private void loadFeeds() {
		loadFromDb(this::loadFeeds, feeds::setValue);
	}

	@DatabaseExecutor
	private List<Feed> loadFeeds(Transaction txn) throws DbException {
		long start = now();
		List<Feed> feeds = feedManager.getFeeds(txn);
		logDuration(LOG, "Loading feeds", start);
		return feeds;
	}

	void removeFeed(GroupId groupId) {
		dbExecutor.execute(() -> {
			List<Feed> updated = removeListItems(getList(feeds), feed -> {
				if (feed.getBlogId().equals(groupId)) {
					try {
						feedManager.removeFeed(feed);
						return true;
					} catch (DbException e) {
						handleException(e);
					}
				}
				return false;
			});
			if (updated != null) {
				feeds.postValue(new LiveResult<>(updated));
			}
		});
	}

	LiveEvent<RssImportResult> getImportResult() {
		return importResult;
	}

	LiveData<Boolean> getIsImporting() {
		return isImporting;
	}

	void importFeed(String url) {
		isImporting.setValue(true);
		ioExecutor.execute(() -> {
			try {
				Feed feed = feedManager.addFeed(url);
				// Update the feed if it was already present
				List<Feed> feedList = getList(feeds);
				if (feedList == null) feedList = new ArrayList<>();
				List<Feed> updated = updateListItems(feedList,
						f -> f.equals(feed), f -> feed);
				// Add the feed if it wasn't already present
				if (updated == null) {
					feedList.add(feed);
					updated = feedList;
				}
				feeds.postValue(new LiveResult<>(updated));
				importResult.postEvent(new UrlImportSuccess());
			} catch (DbException | IOException e) {
				logException(LOG, WARNING, e);
				importResult.postEvent(new UrlImportError(url));
			} finally {
				isImporting.postValue(false);
			}
		});
	}

	@UiThread
	void importFeed(Uri uri) {
		ContentResolver contentResolver = getApplication().getContentResolver();
		ioExecutor.execute(() -> {
			try (InputStream is = contentResolver.openInputStream(uri)) {
				Feed feed = feedManager.addFeed(is);
				// Update the feed if it was already present
				List<Feed> feedList = getList(feeds);
				if (feedList == null) feedList = new ArrayList<>();
				List<Feed> updated = updateListItems(feedList,
						f -> f.equals(feed), f -> feed);
				// Add the feed if it wasn't already present
				if (updated == null) {
					feedList.add(feed);
					updated = feedList;
				}
				feeds.postValue(new LiveResult<>(updated));
				importResult.postEvent(new FileImportSuccess());
			} catch (IOException | DbException e) {
				logException(LOG, WARNING, e);
				importResult.postEvent(new FileImportError());
			}
		});
	}
}
