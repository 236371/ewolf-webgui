package il.technion.ewolf.posts;

import il.technion.ewolf.PostID;
import il.technion.ewolf.exceptions.ForgeryException;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import com.google.inject.Inject;

public class CommentPost extends Post {

	private static final long serialVersionUID = -4197262985870360975L;

	private Post comment;
	
	@Inject
	CommentPost(PostID postID, SocialFS socialFS) {
		super(postID, socialFS);
	}

	
	@Override
	public void validateOwner() throws ForgeryException, ProfileNotFoundException {
		super.validateOwner();
		getComment().validateOwner();
	}
	
	public Post getComment() {
		return comment;
	}
}
