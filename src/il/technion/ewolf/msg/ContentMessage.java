package il.technion.ewolf.msg;

import java.security.Signature;
import java.security.SignatureException;

import il.technion.ewolf.socialfs.SocialFS;

import com.google.inject.Inject;

public class ContentMessage extends SocialMessage {

	private static final long serialVersionUID = -6381142109479266623L;
	
	private String msg;
	
	@Inject
	ContentMessage(SocialFS socialFS) {
		super(socialFS);
	}


	public ContentMessage setMessage(String msg) {
		this.msg = msg;
		return this;
	}
	
	public String getMessage() {
		return msg;
	}
	
	@Override
	protected void updateSignature(Signature sig) throws SignatureException {
		sig.update(msg.getBytes());
		super.updateSignature(sig);
	}
	
	@Override
	public String toString() {
		return "ContentMessage: "+getMessage()+" timestamp: "+getTimestamp();
	}
}
