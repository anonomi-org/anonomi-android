package org.anonomi.android.sharing;

import android.os.Bundle;

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.contactselection.ContactSelectorController;
import org.anonomi.android.contactselection.ContactSelectorFragment;
import org.anonomi.android.contactselection.SelectableContactItem;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import static org.anonomi.android.activity.BriarActivity.GROUP_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ShareForumFragment extends ContactSelectorFragment {

	public static final String TAG = ShareForumFragment.class.getName();

	@Inject
	ShareForumController controller;

	public static ShareForumFragment newInstance(GroupId groupId) {
		Bundle args = new Bundle();
		args.putByteArray(GROUP_ID, groupId.getBytes());
		ShareForumFragment fragment = new ShareForumFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	protected ContactSelectorController<SelectableContactItem> getController() {
		return controller;
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

}
