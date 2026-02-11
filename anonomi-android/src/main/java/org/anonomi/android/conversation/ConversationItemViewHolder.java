package org.anonomi.android.conversation;

import android.content.Context;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.anonomi.R;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.anonchatsecure.bramble.util.StringUtils.trim;
import static org.anonomi.android.util.UiUtils.formatDate;
import static org.anonomi.android.util.UiUtils.formatDuration;
import static org.anonomi.android.util.UiUtils.makeLinksClickable;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;

@UiThread
@NotNullByDefault
abstract class ConversationItemViewHolder extends ViewHolder {

	protected final ConversationListener listener;
	private final View root;
	protected final ConstraintLayout layout;
	@Nullable
	private final OutItemViewHolder outViewHolder;
	@Nullable
	private final TextView topNotice;
	private final TextView text;
	protected final TextView time;
	protected final ImageView bomb;
	@Nullable
	private String itemKey = null;

	ConversationItemViewHolder(View v, ConversationListener listener,
			boolean isIncoming) {
		super(v);
		this.listener = listener;
		outViewHolder = isIncoming ? null : new OutItemViewHolder(v);
		root = v;
		View maybeTopNotice = v.findViewById(R.id.topNotice);
		if (maybeTopNotice instanceof TextView) {
			topNotice = (TextView) maybeTopNotice;
		} else {
			topNotice = null;
		}
		layout = v.findViewById(R.id.layout);
		text = v.findViewById(R.id.text);
		time = v.findViewById(R.id.time);
		bomb = v.findViewById(R.id.bomb);
	}

	@CallSuper
	void bind(ConversationItem item, boolean selected) {
		itemKey = item.getKey();
		root.setActivated(selected);

		setTopNotice(item);

		String messageTextRaw = item.getText();

		if (messageTextRaw != null && text != null) {
			String trimmedText = trim(messageTextRaw);

			if (isMapMessage(trimmedText)) {
				MapMessageData mapData = parseMapMessage(trimmedText);
				String displayText = "\uD83D\uDCCD" + mapData.label +
						"\n   " + mapData.latitude +
						"\n   " + mapData.longitude +
						"\n   " + text.getContext().getString(
						R.string.tap_to_view_on_map);
				text.setText(displayText);
				text.setOnClickListener(v -> listener.onMapMessageClicked(mapData));
			} else {
				text.setText(trimmedText);
				Linkify.addLinks(text, Linkify.WEB_URLS);
				makeLinksClickable(text, listener::onLinkClick);
			}
		}

		time.setText(formatDate(time.getContext(), item.getTime()));
		bomb.setVisibility(item.getAutoDeleteTimer() != NO_AUTO_DELETE_TIMER ? VISIBLE : GONE);

		if (outViewHolder != null) outViewHolder.bind(item);
	}

	boolean isIncoming() {
		return outViewHolder == null;
	}

	@Nullable
	String getItemKey() {
		return itemKey;
	}

	private void setTopNotice(ConversationItem item) {
		if (topNotice == null) return;

		if (item.isTimerNoticeVisible()) {
			Context ctx = itemView.getContext();
			topNotice.setVisibility(VISIBLE);
			boolean enabled = item.getAutoDeleteTimer() != NO_AUTO_DELETE_TIMER;
			String duration = enabled ?
					formatDuration(ctx, item.getAutoDeleteTimer()) : "";
			String tapToLearnMore = ctx.getString(R.string.tap_to_learn_more);
			String text;
			if (item.isIncoming()) {
				String name = item.getContactName().getValue();
				text = enabled ?
						ctx.getString(R.string.auto_delete_msg_contact_enabled,
								name, duration, tapToLearnMore) :
						ctx.getString(R.string.auto_delete_msg_contact_disabled,
								name, tapToLearnMore);
			} else {
				text = enabled ?
						ctx.getString(R.string.auto_delete_msg_you_enabled,
								duration, tapToLearnMore) :
						ctx.getString(R.string.auto_delete_msg_you_disabled,
								tapToLearnMore);
			}
			topNotice.setText(text);
			topNotice.setOnClickListener(
					v -> listener.onAutoDeleteTimerNoticeClicked());
		} else {
			topNotice.setVisibility(GONE);
		}
	}

	private boolean isMapMessage(String text) {
		return text.startsWith("::map:");
	}

	private MapMessageData parseMapMessage(String text) {
		String payload = text.substring(6); // Remove "::map:"
		String[] parts = payload.split(";");
		String label = (parts.length > 0) ? parts[0].trim() : "Unknown";
		String location = (parts.length > 1) ? parts[1].trim() : "";
		String zoom = (parts.length > 2) ? parts[2].trim() : "";

		try {
			String[] latLon = location.split(",");
			String latStr = (latLon.length > 0) ? latLon[0].trim().replace(":", "") : "0";
			String lonStr = (latLon.length > 1) ? latLon[1].trim().replace(":", "") : "0";
			double lat = Double.parseDouble(latStr);
			double lon = Double.parseDouble(lonStr);
			return new MapMessageData(label, lat, lon, zoom);
		} catch (Exception e) {
			return new MapMessageData(label, 0, 0, zoom);
		}
	}
}