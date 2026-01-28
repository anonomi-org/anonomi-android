package org.anonomi.android.blog;

import org.anonchatsecure.anonchat.api.blog.Blog;

class BlogItem {

	private final Blog blog;
	private final boolean ours, removable;

	BlogItem(Blog blog, boolean ours, boolean removable) {
		this.blog = blog;
		this.ours = ours;
		this.removable = removable;
	}

	Blog getBlog() {
		return blog;
	}

	boolean isOurs() {
		return ours;
	}

	boolean canBeRemoved() {
		return removable;
	}
}
