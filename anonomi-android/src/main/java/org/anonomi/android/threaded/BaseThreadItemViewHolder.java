package org.anonomi.android.threaded;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import org.anonchatsecure.bramble.util.StringUtils;
import org.anonomi.R;
import org.anonomi.android.conversation.MapMessageData;
import org.anonomi.android.threaded.ThreadItemAdapter.ThreadItemListener;
import org.anonomi.android.view.AuthorView;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.CallSuper;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import static androidx.core.content.ContextCompat.getColor;
import static org.anonomi.android.util.UiUtils.makeLinksClickable;

@UiThread
@NotNullByDefault
public abstract class BaseThreadItemViewHolder<I extends ThreadItem>
		extends RecyclerView.ViewHolder {

	private final static int ANIMATION_DURATION = 5000;

	protected final TextView textView;
	private final ViewGroup layout;
	private final AuthorView author;

	public BaseThreadItemViewHolder(View v) {
		super(v);

		layout = v.findViewById(R.id.layout);
		textView = v.findViewById(R.id.text);
		author = v.findViewById(R.id.author);
	}

	@CallSuper
	public void bind(I item, ThreadItemListener<I> listener) {
		String trimmedText = StringUtils.trim(item.getText());
		if (trimmedText != null && trimmedText.startsWith("::map:")) {
			MapMessageData mapData = parseMapMessage(trimmedText);
			textView.setText("\uD83D\uDCCD" + mapData.label +
					"\n   " + mapData.latitude +
					"\n   " + mapData.longitude +
					"\n   " + getContext().getString(
					R.string.tap_to_view_on_map));
			textView.setOnClickListener(
					v -> listener.onMapMessageClicked(mapData));
			textView.setVisibility(View.VISIBLE);
		} else if (trimmedText != null && !trimmedText.isEmpty()) {
			textView.setText(trimmedText);
			Linkify.addLinks(textView, Linkify.WEB_URLS);
			makeLinksClickable(textView, listener::onLinkClick);
			textView.setVisibility(View.VISIBLE);
		} else {
			textView.setText(null);
			textView.setVisibility(View.GONE);
		}

		author.setAuthor(item.getAuthor(), item.getAuthorInfo());
		author.setDate(item.getTimestamp());

		if (item.isHighlighted()) {
			layout.setActivated(true);
		} else if (!item.isRead()) {
			layout.setActivated(true);
			animateFadeOut();
		} else {
			layout.setActivated(false);
		}
	}

	private void animateFadeOut() {
		setIsRecyclable(false);
		ValueAnimator anim = new ValueAnimator();
		int viewColor = getColor(getContext(), R.color.thread_item_highlight);
		anim.setIntValues(viewColor,
				getColor(getContext(), R.color.thread_item_background));
		anim.setEvaluator(new ArgbEvaluator());
		anim.setInterpolator(new AccelerateInterpolator());
		anim.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			}
			@Override
			public void onAnimationEnd(Animator animation) {
				layout.setBackgroundResource(
						R.drawable.list_item_thread_background);
				layout.setActivated(false);
				setIsRecyclable(true);
			}
			@Override
			public void onAnimationCancel(Animator animation) {
			}
			@Override
			public void onAnimationRepeat(Animator animation) {
			}
		});
		anim.addUpdateListener(valueAnimator -> layout.setBackgroundColor(
				(Integer) valueAnimator.getAnimatedValue()));
		anim.setDuration(ANIMATION_DURATION);
		anim.start();
	}

	private MapMessageData parseMapMessage(String text) {
		String payload = text.substring(6); // Remove "::map:"
		String[] parts = payload.split(";");
		String label = (parts.length > 0) ? parts[0].trim() : "Unknown";
		String location = (parts.length > 1) ? parts[1].trim() : "";
		String zoom = (parts.length > 2) ? parts[2].trim() : "";

		try {
			String[] latLon = location.split(",");
			String latStr = (latLon.length > 0) ?
					latLon[0].trim().replace(":", "") : "0";
			String lonStr = (latLon.length > 1) ?
					latLon[1].trim().replace(":", "") : "0";
			double lat = Double.parseDouble(latStr);
			double lon = Double.parseDouble(lonStr);
			return new MapMessageData(label, lat, lon, zoom);
		} catch (Exception e) {
			return new MapMessageData(label, 0, 0, zoom);
		}
	}

	protected Context getContext() {
		return textView.getContext();
	}

}
