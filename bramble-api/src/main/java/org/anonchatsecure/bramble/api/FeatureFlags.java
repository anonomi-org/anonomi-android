package org.anonchatsecure.bramble.api;

/**
 * Interface for specifying which features are enabled in a build.
 */
public interface FeatureFlags {

	boolean shouldEnableImageAttachments();

	boolean shouldEnableProfilePictures();

	boolean shouldEnableDisappearingMessages();

	boolean shouldEnablePrivateGroupsInCore();

	boolean shouldEnableForumsInCore();

	boolean shouldEnableBlogsInCore();
}
