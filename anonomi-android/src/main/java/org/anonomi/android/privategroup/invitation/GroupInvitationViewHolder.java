package org.anonomi.android.privategroup.invitation;

import android.view.View;

import org.anonomi.R;
import org.anonomi.android.sharing.InvitationAdapter.InvitationClickListener;
import org.anonomi.android.sharing.InvitationViewHolder;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationItem;

import javax.annotation.Nullable;

import static org.anonomi.android.util.UiUtils.getContactDisplayName;

class GroupInvitationViewHolder
		extends InvitationViewHolder<GroupInvitationItem> {

	GroupInvitationViewHolder(View v) {
		super(v);
	}

	@Override
	public void onBind(@Nullable GroupInvitationItem item,
			InvitationClickListener<GroupInvitationItem> listener) {
		super.onBind(item, listener);
		if (item == null) return;

		sharedBy.setText(
				sharedBy.getContext().getString(R.string.groups_created_by,
						getContactDisplayName(item.getCreator())));
	}

}