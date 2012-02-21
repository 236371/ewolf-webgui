package il.technion.ewolf.posts;

import il.technion.ewolf.PostID;
import il.technion.ewolf.exceptions.ForgeryException;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.crypto.Signable;

import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.inject.Inject;

public abstract class Post extends Signable {

	private static final long serialVersionUID = 4438192494855165945L;

	protected transient SocialFS socialFS;
	protected transient SocialMail socialMail;
	
	@Expose private long timestamp;
	@Expose private UserID ownerID;
	@Expose private PostID postID;
	
	@Inject
	protected Post(PostID postID, SocialFS socialFS) {
		this.postID = postID;
		this.socialFS = socialFS;
		this.timestamp = System.currentTimeMillis();
	}
	
	public Post setTransientParams(SocialFS socialFS) {
		this.socialFS = socialFS;
		return this;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public Post setOwner(Profile owner) {
		this.ownerID = owner.getUserId();
		System.out.println("owner: "+this.ownerID);
		setPrvSigKey(owner.getPrvSigKey());
		setPubSigKey(owner.getPubSigKey());
		return this;
	}
	
	public Profile getOwner() throws ProfileNotFoundException {
		return socialFS.findProfile(ownerID);
	}
	
	@Override
	protected void updateSignature(Signature sig) throws SignatureException {
		sig.update(Long.toString(timestamp).getBytes());
	}
	
	public void validateOwner() throws ForgeryException, ProfileNotFoundException {
		if (!getOwner().getPubSigKey().equals(this.getPubSigKey()))
			throw new ForgeryException();
	}
	
	public void comment(Post p) {
		
	}
	
	public List<CommentPost> getAllComments() {
		return null;
	}
	/*
	public abstract void exportToJson(JsonWriter writer) throws IOException;
	
	
	protected void exportPostToJson(JsonWriter writer) throws IOException {
		writer
			.name("timestamp").value(timestamp)
			.name("owner_name").value(getOwner().getName())
			.name("owner_uid").value(getOwner().getUserId().toBase64());
	}
	*/
	
	public PostID getPostId() {
		return postID;
	}
	
	
}
