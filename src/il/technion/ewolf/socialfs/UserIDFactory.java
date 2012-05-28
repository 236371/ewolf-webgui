package il.technion.ewolf.socialfs;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import com.google.inject.Inject;

public class UserIDFactory {

	private final KeyFactory keyFactory;
	
	@Inject
	UserIDFactory(KeyFactory keyFactory) {
		this.keyFactory = keyFactory;
		
	}
	
	public UserID create(Profile p) {
		return create(p.getPubSigKey());
	}

	public UserID create(PublicKey pubSigKey) {
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(pubSigKey.getEncoded());
			return new UserID(keyFactory.create(in));
		} catch(IOException e) {
			throw new AssertionError();
		} finally {
			try { in.close(); } catch (Exception e) {}
		}
	}
	
	public UserID getFromKey(Key key) {
		return new UserID(key);
	}
	
	public UserID getFromBase64(String base64Encoded) {
		return getFromKey(keyFactory.get(base64Encoded));
	}
	
}
