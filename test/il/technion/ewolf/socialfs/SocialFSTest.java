package il.technion.ewolf.socialfs;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.ChunKeeperModule;
import il.technion.ewolf.dht.SimpleDHTModule;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;
import il.technion.ewolf.stash.Group;
import il.technion.ewolf.stash.StashModule;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SocialFSTest {

	private static final int BASE_PORT = 10000;
	private List<Injector> injectors = new LinkedList<Injector>();

	@After
	public void cleanup() {
		for (Injector inj: injectors) {
			inj.getInstance(KeybasedRouting.class).shutdown();
			inj.getInstance(HttpConnector.class).shutdown();
		}
		injectors.clear();
	}

	@Test
	public void itShouldCreateANewAccountAndShareAFile() throws Exception {

		for (int i=0; i < 5; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "20")
						.setProperty("openkad.bucket.kbuckets.maxsize", "20")
						.setProperty("openkad.net.udp.port", ""+(BASE_PORT+i)),

					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(BASE_PORT+i)),

					new SimpleDHTModule()
						.setProperty("chunkeeper.dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5)),
						
					new ChunKeeperModule(),
					
					new StashModule(),
						
					new SocialFSCreatorModule()
						.setProperty("socialfs.user.username", "user_"+i)
						.setProperty("socialfs.user.password", "1234"),
					
					new SocialFSModule()
			);
			injectors.add(injector);
		}
		
		for (Injector injector : injectors) {
			
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
		}
		
		
		for (int i=1; i < injectors.size(); ++i) {
			int port = BASE_PORT + i - 1;
			System.out.println(i+" ==> "+(i-1));
			KeybasedRouting kbr = injectors.get(i).getInstance(KeybasedRouting.class);
			kbr.join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		
		for (Injector injector : injectors) {
			System.out.println("creating...");
			SocialFSCreator socialFSCreator = injector.getInstance(SocialFSCreator.class);
			socialFSCreator.create();
			System.out.println("done\n");
			
		}
		
		for (Injector injector : injectors) {
			System.out.println("login ...");
			SocialFS socialFS = injector.getInstance(SocialFS.class);
			socialFS.login("1234");
		}
			
		
		SocialFS socialFS1 = injectors.get(1).getInstance(SocialFS.class);
		SocialFS socialFS2 = injectors.get(2).getInstance(SocialFS.class);
		
		SFSFile rootFile1 = socialFS1.getCredentials().getProfile().getRootFile();

		// create new file
		SFSFile f = socialFS1.getSFSFileFactory().createNewFile()
				.setName("test1")
				.setData("some data");

		// create group for new file
		Group group = socialFS1.getStash().createGroup();
		rootFile1.append(f, group);
		
		// share groups
		for (Group g : socialFS1.getStash().getAllGroups()) {
			socialFS2.getStash().addGroup(g);
		}
		
		// searching socialFS1's profile
		UserID uid1 = socialFS1.getCredentials().getProfile().getUserId();
		Profile profile = socialFS2.findProfile(uid1);
		
		// checking that we found the right profile
		Assert.assertEquals(socialFS1.getCredentials().getProfile(), profile);
		// checking private key was NOT transfered
		Assert.assertNull(profile.getPrvSigKey());

		// getting the new file
		SFSFile rootFile2 = profile.getRootFile();
		SFSFile testFile = rootFile2.getSubFiles(0);
		
		Assert.assertEquals("test1", testFile.getName());
		Assert.assertEquals("some data", testFile.getData());
		
		System.out.println(socialFS1);
	}
	
	
	@Test
	public void itShouldCreateAFileHirarchyAndSearchInIt() throws Exception {

		for (int i=0; i < 1; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "20")
						.setProperty("openkad.bucket.kbuckets.maxsize", "20")
						.setProperty("openkad.net.udp.port", ""+(BASE_PORT+i)),
						
					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(BASE_PORT+i)),
					
					new SimpleDHTModule()
						.setProperty("chunkeeper.dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5)),
						
					new ChunKeeperModule(),
					
					new StashModule(),
						
					new SocialFSCreatorModule()
						.setProperty("socialfs.user.username", "user_"+i)
						.setProperty("socialfs.user.password", "1234"),
					
					new SocialFSModule()
			);
			injectors.add(injector);
		}
		
		for (Injector injector : injectors) {
			
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
		}
		
		
		for (int i=1; i < injectors.size(); ++i) {
			int port = BASE_PORT + i - 1;
			System.out.println(i+" ==> "+(i-1));
			KeybasedRouting kbr = injectors.get(i).getInstance(KeybasedRouting.class);
			kbr.join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		
		for (Injector injector : injectors) {
			System.out.println("creating...");
			SocialFSCreator socialFSCreator = injector.getInstance(SocialFSCreator.class);
			socialFSCreator.create();
			System.out.println("done\n");
			
		}
		
		for (Injector injector : injectors) {
			System.out.println("login ...");
			SocialFS socialFS = injector.getInstance(SocialFS.class);
			socialFS.login("1234");
		}
			
		
		Thread.sleep(1000);
		
		SocialFS socialFS1 = injectors.get(0).getInstance(SocialFS.class);
		
		SFSFile rootFile1 = socialFS1.getCredentials().getProfile().getRootFile();

		// create new file
		SFSFile f = socialFS1.getSFSFileFactory().createNewFile()
				.setName("test1")
				.setData("some data 1");

		// create group for new file
		Group group = socialFS1.getStash().createGroup();
		rootFile1.append(f, group);
		
		// adding more files
		f = socialFS1.getSFSFileFactory().createNewFile()
				.setName("test2")
				.setData("some data 2");
		rootFile1.append(f, group);
		
		SFSFile testFolder = socialFS1.getSFSFileFactory().createNewFolder()
				.setName("folder1")
				.setData("folder data 1");
		rootFile1.append(testFolder, group);
		
		f = socialFS1.getSFSFileFactory().createNewFile()
				.setName("test3")
				.setData("some data 3");
		testFolder.append(f, group);
		
		f = socialFS1.getSFSFileFactory().createNewFile()
				.setName("test4")
				.setData("some data 4");
		rootFile1.append(f, group);
		
		rootFile1.getSubFile("/folder1/").append(socialFS1.getSFSFileFactory()
				.createNewFolder()
					.setName("folder2")
					.setData("folder data 2")
				, group);
		
		rootFile1.getSubFile("/folder1/folder2/").append(socialFS1.getSFSFileFactory()
				.createNewFile()
					.setName("test5")
					.setData("some data 5")
				, group);
		
		System.out.println(socialFS1);
		
		
		Assert.assertEquals("some data 1", rootFile1.getSubFile("/test1").getData());
		Assert.assertEquals("some data 2", rootFile1.getSubFile("/test2").getData());
		Assert.assertEquals("folder data 1", rootFile1.getSubFile("/folder1").getData());
		Assert.assertEquals("folder data 2", rootFile1.getSubFile("/folder1/folder2/").getData());
		Assert.assertEquals("some data 3", rootFile1.getSubFile("/folder1/test3").getData());
		Assert.assertEquals("some data 4", rootFile1.getSubFile("/test4").getData());
		Assert.assertEquals("some data 5", rootFile1.getSubFile("/folder1/folder2/test5").getData());
		
	}
	
}
