/*
 * Briar Desktop
 * Copyright (C) 2025 The Briar Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anonchatsecure.anonchat.feed;

import org.anonchatsecure.bramble.api.FeatureFlags;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.feed.FeedManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class FeedModule {

	public static class EagerSingletons {
		@Inject
		FeedManager feedManager;
	}

	@Provides
	@Singleton
	FeedManager provideFeedManager(FeedManagerImpl feedManager,
			LifecycleManager lifecycleManager, EventBus eventBus,
			BlogManager blogManager, FeatureFlags featureFlags) {
		if (!featureFlags.shouldEnableBlogsInCore()) {
			return feedManager;
		}
		lifecycleManager.registerOpenDatabaseHook(feedManager);
		eventBus.addListener(feedManager);
		blogManager.registerRemoveBlogHook(feedManager);
		return feedManager;
	}

	@Provides
	FeedFactory provideFeedFactory(FeedFactoryImpl feedFactory) {
		return feedFactory;
	}

	@Provides
	FeedMatcher provideFeedMatcher(FeedMatcherImpl feedMatcher) {
		return feedMatcher;
	}
}
