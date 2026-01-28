package org.anonomi.android.privategroup.memberlist;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.controller.DbController;
import org.anonomi.android.controller.handler.ResultExceptionHandler;

import java.util.Collection;

public interface GroupMemberListController extends DbController {

	void loadMembers(GroupId groupId,
			ResultExceptionHandler<Collection<MemberListItem>, DbException> handler);

}
