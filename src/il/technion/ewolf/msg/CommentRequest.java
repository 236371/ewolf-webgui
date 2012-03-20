package il.technion.ewolf.msg;

import il.technion.ewolf.PostID;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;

import java.util.List;

import com.google.inject.Inject;

public class CommentRequest extends SocialMessage {

	private static final long serialVersionUID = 2715548453173544909L;

	private Post comment;
	private UserID wallOwner;
	private List<PostID> postIdPath;
	
	@Inject
	CommentRequest(SocialFS socialFS) {
		super(socialFS);
		// TODO Auto-generated constructor stub
	}
	
	public Post getComment() {
		return comment;
	}
	
	public List<PostID> getPostIdPath() {
		return postIdPath;
	}
	
	public UserID getWallOwner() {
		return wallOwner;
	}
	
	
}
