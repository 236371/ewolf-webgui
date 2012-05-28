package il.technion.ewolf.socialfs;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.socialfs.background.ReinsertProfileTask;
import il.technion.ewolf.socialfs.exception.CredentialsNotFoundException;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.Stash;
import il.technion.ewolf.stash.crypto.EncryptedObject;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class SocialFS {

	// state
	private Credentials cred;
	
	// dependencies
	private final KeyFactory keyFactory;
	private final String username;
	private final String credentialsEncryptionAlgorithm;
	private final ChunKeeper chunkeeper;
	private final Provider<ReinsertProfileTask> reinsertProfileTaskProvider;
	private final Provider<MessageDigest> passwordDigestProvider;
	private final DHT profileDHT;
	private final Stash stash;
	private final SFSFileFactory fileFactory;
	private final Cache<SFSFile> fileCache;
	private final Cache<Profile> profileCache;
	private final UserIDFactory uidFactory;
	
	@Inject
	SocialFS(KeyFactory keyFactory,
			@Named("socialfs.user.username") String username,
			@Named("socialfs.cred.encryption.algo") String credentialsEncryptionAlgorithm,
			ChunKeeper chunkeeper,
			Provider<ReinsertProfileTask> reinsertProfileTaskProvider,
			@Named("socialfs.cred.digest") Provider<MessageDigest> passwordDigestProvider,
			@Named("socialfs.profile.dht") DHT profileDHT,
			Stash stash,
			SFSFileFactory fileFactory,
			@Named("socialfs.cache.filecache") Cache<SFSFile> fileCache,
			@Named("socialfs.cache.profilecache") Cache<Profile> profileCache,
			UserIDFactory uidFactory) {
		
		this.keyFactory = keyFactory;
		this.username = username;
		this.credentialsEncryptionAlgorithm = credentialsEncryptionAlgorithm;
		this.chunkeeper = chunkeeper;
		this.reinsertProfileTaskProvider = reinsertProfileTaskProvider;
		this.passwordDigestProvider = passwordDigestProvider;
		this.profileDHT = profileDHT;
		this.stash = stash;
		this.fileFactory = fileFactory;
		this.fileCache = fileCache;
		this.profileCache = profileCache;
		this.uidFactory = uidFactory;
	}
	
	public void login(String password) throws CredentialsNotFoundException {
		System.out.println("searching credentials with password "+password);
		if (cred != null)
			throw new IllegalStateException("already logged in !");
		
		Key credKey = keyFactory.create(password, username);
		MessageDigest md = passwordDigestProvider.get();
		md.update(password.getBytes());
		SecretKey credSecretKey = new SecretKeySpec(md.digest(), credentialsEncryptionAlgorithm);
		
		for (Chunk c : chunkeeper.findChunk(credKey)) {
			try {
				Serializable s = c.download();
				if (!s.getClass().equals(EncryptedObject.class))
					continue;
				
				@SuppressWarnings("unchecked")
				EncryptedObject<Credentials> encCred = (EncryptedObject<Credentials>)s;
				
				cred = encCred.decrypt(credSecretKey)
							.setCredentialsKey(credSecretKey);
				
			} catch (Exception e) {
				// wrong credentials, moving on to the next
				e.printStackTrace();
			}
		}
		if (cred == null)
			throw new CredentialsNotFoundException();
		
		System.out.println("found credentials: "+cred.getCredentialsKey());
		
		cred.getProfile()
			.setTransientParams(stash, fileCache, uidFactory);
		
		// TODO set chunkeeper keys
		//chunkeeper.login(cred.getChunkeeperKeys());
		
		fileFactory.login(cred);
		stash.login(cred.getGroupsMasterKey());
		
		reinsertProfileTaskProvider.get()
			.setCredentials(cred)
			.register();
	}
	
	public Credentials getCredentials() {
		return cred;
	}
	
	public Profile findProfile(UserID uid) throws ProfileNotFoundException {
		if (cred.getProfile().getUserId().equals(uid))
			return cred.getProfile();
		
		Key k = uid.getKey();
		Profile cachedProfile = profileCache.search(k);
		if (cachedProfile != null)
			return cachedProfile;
		
		List<Serializable> profiles = profileDHT.get(k);
		
		for (Serializable s : profiles) {
			
			if (!(s instanceof Profile))
				continue;
			
			Profile p = ((Profile)s)
				.setTransientParams(stash, fileCache, uidFactory);
			
			if (p.getUserId().equals(uid)) {
				profileCache.insert(p);
				return p;
			}
		}
		
		throw new ProfileNotFoundException();
	}
	
	public SFSFileFactory getSFSFileFactory() {
		return fileFactory;
	}
	
	public Stash getStash() {
		return stash;
	}

	
	private String __toString(String prefix, SFSFile f) throws Exception {
		prefix += f.getName();
		int i=0;
		String $ = "";
		do {
			try {
				$ += __toString(prefix + "/", f.getSubFiles(i++));
			} catch (FileNotFoundException e) {
				return i == 1 ? prefix + "\n" : prefix + "/\n"+$;
			}
		} while (true);
	}
	
	@Override
	public String toString() {
		try {
			SFSFile root = getCredentials().getProfile().getRootFile();
			return __toString("/", root);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
