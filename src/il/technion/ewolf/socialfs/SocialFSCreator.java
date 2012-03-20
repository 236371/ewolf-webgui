package il.technion.ewolf.socialfs;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.ChunKeeperModule;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;
import il.technion.ewolf.stash.Group;
import il.technion.ewolf.stash.Stash;
import il.technion.ewolf.stash.StashModule;
import il.technion.ewolf.stash.crypto.EncryptedObject;

import java.security.MessageDigest;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class SocialFSCreator {

	private final Credentials cred;
	private final KeyFactory keyFactory;
	private final String username;
	private final String password;
	private final String credentialsEncryptionAlgorithm;
	private final Provider<MessageDigest> passwordDigestProvider;
	private final ChunKeeper chunkeeper;
	private final Stash stash;
	private final SFSFileFactory fileFactory;
	
	
	@Inject
	SocialFSCreator(Credentials cred,
			KeyFactory keyFactory,
			@Named("socialfs.user.username") String username,
			@Named("socialfs.user.password") String password,
			@Named("socialfs.cred.encryption.algo") String credentialsEncryptionAlgorithm,
			@Named("socialfs.cred.digest") Provider<MessageDigest> passwordDigestProvider,
			ChunKeeper chunkeeper,
			Stash stash,
			SFSFileFactory fileFactory) {
		
		this.cred = cred;
		this.keyFactory = keyFactory;
		this.username = username;
		this.password = password;
		this.credentialsEncryptionAlgorithm = credentialsEncryptionAlgorithm;
		this.passwordDigestProvider = passwordDigestProvider;
		this.chunkeeper = chunkeeper;
		this.stash = stash;
		this.fileFactory = fileFactory;
	}
	
	private void createRootFile() throws Exception {
		System.out.println("creating root file");
		
		Group g = stash.createGroup();
		System.out.println("all groups: "+stash.getAllGroups());
		SFSFile rootFile = fileFactory.createNewFolder()
			.setName("sfs")
			.setData(null)
			.setFileKey(cred.getProfile().getRootKey())
			.setParentKey(cred.getProfile().getRootKey())
			.setGroupId(g.getGroupId());
		
		System.out.println("storing root file");
		
		stash.put(cred.getProfile().getRootKey(), rootFile, g);
	}
	
	public String getPassword() {
		return password;
	}
	
	public void create() throws Exception {
		//System.out.println(cred);
		
		
		Key credKey = keyFactory.create(password, username);
		
		MessageDigest md = passwordDigestProvider.get();
		
		md.update(password.getBytes());
		SecretKey credSecretKey = new SecretKeySpec(md.digest(), credentialsEncryptionAlgorithm);
		
		System.out.println("encrypting credentials");
		EncryptedObject<Credentials> encryptedCred = new EncryptedObject<Credentials>().encrypt(cred, credSecretKey);
		
		System.out.println("storing credentials");
		chunkeeper.store(credKey, encryptedCred);
		
		System.out.println("stash login");
		stash.login(cred.getGroupsMasterKey());
		fileFactory.login(cred);
		
		createRootFile();
		
		/*
		try {
			Set<Chunk> chunks = chunkeeper.findChunk(credKey);
			for (Chunk c : chunks) {
				EncryptedObject<Credentials> x = (EncryptedObject<Credentials>)c.download();
				Credentials cr = x.decrypt(credSecretKey);
				cr.setCredentialsKey(credSecretKey);
				System.err.println(cr);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	
	public static void main(String[] args) throws Exception {
		Injector injector = Guice.createInjector(
				new KadNetModule()
					.setProperty("openkad.net.udp.port", "5555"),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", "5555"),
				
				new ChunKeeperModule(),
				
				new StashModule(),
					
				new SocialFSCreatorModule()
					.setProperty("socialfs.user.password", "1234"),
				
				new SocialFSModule()
		);
		
		// start the Keybased routing
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// bind the http connector
		HttpConnector connector = injector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		SocialFSCreator socialFSCreator = injector.getInstance(SocialFSCreator.class);
		
		socialFSCreator.create();
		
		
		SocialFS socialFS = injector.getInstance(SocialFS.class);
		
		socialFS.login("1234");
		
		Thread.sleep(1000);
		
		SFSFile rootFile = socialFS.getCredentials().getProfile().getRootFile();
		
		System.out.println(rootFile.getName());
		
		
		Stash stash = injector.getInstance(Stash.class);
		SFSFileFactory fileFactory = injector.getInstance(SFSFileFactory.class);
		SFSFile testFile = fileFactory.createNewFile()
			.setData("some data")
			.setName("testFile");
		
		Group group = stash.createGroup();
		
		rootFile.append(testFile, group);
		
		Thread.sleep(1000);
		rootFile = socialFS.getCredentials().getProfile().getRootFile();
		
		//System.out.println(rootFile.getName());
		testFile = rootFile.getSubFiles(0);
		System.out.println(testFile.getName());
		System.out.println(testFile.getData());
	}
	
}
