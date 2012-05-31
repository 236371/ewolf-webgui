package il.technion.ewolf.socialfs;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.stash.LazyChunkDecryptor;
import il.technion.ewolf.stash.Stash;
import il.technion.ewolf.stash.crypto.Signable;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Profile extends Signable implements KeyHolder {

	private static final long serialVersionUID = -2057276201123630029L;
	
	private transient Stash stash;
	private transient Cache<SFSFile> fileCache;
	private transient UserIDFactory uidFactory;
	
	private final PublicKey pubEncKey;
	private final String name;
	private final Key rootKey;
	
	@Inject
	Profile(
			PrivateKey prvSigKey,
			PublicKey pubSigKey,
			PublicKey pubEncKey,
			String name,
			Key rootKey,
			Stash stash,
			UserIDFactory uidFactory,
			@Named("socialfs.cache.filecache") Cache<SFSFile> fileCache) throws InvalidKeyException, IOException {
		
		super(prvSigKey, pubSigKey);
		
		this.pubEncKey = pubEncKey;
		this.name = name;
		this.rootKey = rootKey;
		
		this.stash = stash;
		this.uidFactory = uidFactory;
		this.fileCache = fileCache;
	}
	
	Profile setTransientParams(Stash stash, Cache<SFSFile> fileCache, UserIDFactory uidFactory) {
		this.stash = stash;
		this.uidFactory = uidFactory;
		this.fileCache = fileCache;
		return this;
	}
	
	Key getRootKey() {
		return rootKey;
	}
	
	public SFSFile getRootFile() throws FileNotFoundException {
		List<LazyChunkDecryptor> files = stash.get(rootKey);
		
		for (LazyChunkDecryptor p : files) {
			
			try {
				SFSFile f = p.downloadAndDecrypt(SFSFile.class);
				
				if (!f.getPubSigKey().equals(getPubSigKey()))
					throw new InvalidKeyException("wrong pub key");
				
				f.setPrvSigKey(getPrvSigKey());
				
				return f.setTransientParams(stash, fileCache);
				
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (GroupNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		throw new FileNotFoundException();
	}
	
	public String getName() {
		return name;
	}
	
	public PublicKey getPubEncKey() {
		return pubEncKey;
	}
	
	public UserID getUserId() {
		return uidFactory.create(this);
	}
	
	protected void updateSignature(Signature sig) throws SignatureException {
		sig.update(pubEncKey.getEncoded());
		sig.update(name.getBytes());
		sig.update(rootKey.getBytes());
	}
	
	@Override
	public int hashCode() {
		return getPubEncKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		
		Profile p = (Profile)obj;
		
		return p.getPubEncKey().equals(getPubEncKey());
	}
	
	
	@Override
	public String toString() {
		return 
				"pubEncKey: "+pubEncKey+"\n\n"+
				"name: "+name+"\n\n"+
				"rootKey: "+rootKey.toBase64()+"\n\n"+
				"pubSigKey: "+getPubSigKey()+"\n\n";
				//"userID: " + getUserId()+ "\n\n";
				//"privSigKey: "+getPrivSigKey()+"\n\n";
	}

	@Override
	public Key getKey() {
		return getUserId().getKey();
	}
	
}
