package org.anonomi.android.privategroup.creation;

import org.anonomi.android.fragment.BaseFragment.BaseFragmentListener;

interface CreateGroupListener extends BaseFragmentListener {

	void onGroupNameChosen(String name);
}
