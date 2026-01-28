package org.anonchatsecure.bramble.api.plugin.simplex;

import org.anonchatsecure.bramble.api.plugin.PluginFactory;
import org.briarproject.nullsafety.NotNullByDefault;

/**
 * Factory for creating a plugin for a simplex transport.
 */
@NotNullByDefault
public interface SimplexPluginFactory extends PluginFactory<SimplexPlugin> {
}
