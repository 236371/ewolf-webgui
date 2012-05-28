package il.technion.ewolf.msg;

import il.technion.ewolf.exceptions.ForgeryException;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.crypto.Signable;

import java.security.Signature;
import java.security.SignatureException;

import com.google.inject.Inject;

public abstract class SocialMessage extends Signable {
	
	private static final long serialVersionUID = -5118607641755967396L;

	protected transient SocialFS socialFS;
	
	private long timestamp;
	private UserID sender;
	
	@Inject
	SocialMessage(SocialFS socialFS) {
		this.socialFS = socialFS;
		timestamp = System.currentTimeMillis();
	}
	
	public SocialMessage setSender(Profile sender) {
		this.sender = sender.getUserId();
		setPrvSigKey(sender.getPrvSigKey());
		setPubSigKey(sender.getPubSigKey());
		return this;
	}
	
	public Profile getSender() throws ProfileNotFoundException {
		return socialFS.findProfile(sender);
	}

	@Override
	protected void updateSignature(Signature sig) throws SignatureException {
		sig.update(Long.toString(timestamp).getBytes());
		sig.update(sender.getKey().getBytes());
	}
	
	SocialMessage setTransientParams(SocialFS socialFS) {
		this.socialFS = socialFS;
		return this;
	}
	
	public void verifySender(UserIDFactory uidFactory) throws ForgeryException {
		UserID expectingUID = uidFactory.create(getPubSigKey());
		if (!expectingUID.equals(sender))
			throw new ForgeryException("expecting uid: "+expectingUID+", but got: "+sender);
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
}
