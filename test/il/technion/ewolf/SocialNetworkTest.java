package il.technion.ewolf;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.ChunKeeperModule;
import il.technion.ewolf.dht.SimpleDHTModule;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;
import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.SocialFSCreatorModule;
import il.technion.ewolf.socialfs.SocialFSModule;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.stash.StashModule;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SocialNetworkTest {

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
	public void itShouldCreateANewAccount() throws Exception {
		Injector injector = Guice.createInjector(
				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "20")
					.setProperty("openkad.bucket.kbuckets.maxsize", "20")
					.setProperty("openkad.net.udp.port", Integer.toString(BASE_PORT)),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", Integer.toString(BASE_PORT)),
				
				new SimpleDHTModule(),
					
				new ChunKeeperModule(),
				
				new StashModule(),
				
				new SocialFSCreatorModule()
					.setProperty("socialfs.user.username", "user")
					.setProperty("socialfs.user.password", "1234"),
				
				new SocialFSModule(),
				
				new EwolfAccountCreatorModule(),
				
				new EwolfModule()
		);
		
		injectors.add(injector);
		
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// bind the http connector
		HttpConnector connector = injector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		EwolfAccountCreator accountCreator = injector.getInstance(EwolfAccountCreator.class);
		
		accountCreator.create();
	}
	
	
	@Test
	public void itShouldSendMessageToAnotherUser() throws Exception {
		
		for (int i=0; i < 5; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "20")
						.setProperty("openkad.bucket.kbuckets.maxsize", "20")
						.setProperty("openkad.net.udp.port", ""+(BASE_PORT+i)),
						
					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(BASE_PORT+i)),
					
					new SimpleDHTModule(),
						
					new ChunKeeperModule(),
					
					new StashModule(),
						
					new SocialFSCreatorModule()
						.setProperty("socialfs.user.username", "user_"+i)
						.setProperty("socialfs.user.password", "1234"),
					
					new SocialFSModule(),
					
					new EwolfAccountCreatorModule(),
					
					new EwolfModule()
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
			EwolfAccountCreator accountCreator = injector.getInstance(EwolfAccountCreator.class);
			accountCreator.create();
			System.out.println("done\n");
			
		}
		/*
		for (Injector injector : injectors) {
			System.out.println("login ...");
			SocialNetwork osn = injector.getInstance(SocialNetwork.class);
			osn.login("1234");
		}
		*/
		Thread.sleep(1000);
		
		SocialFS sfs1 = injectors.get(0).getInstance(SocialFS.class);
		SocialFS sfs2 = injectors.get(1).getInstance(SocialFS.class);
		
		SocialMail sm1 = injectors.get(0).getInstance(SocialMail.class);
		SocialMail sm2 = injectors.get(1).getInstance(SocialMail.class);
		
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		
		ContentMessage msg1 = sm1.createContentMessage()
			.setMessage("hi !");
		Thread.sleep(100);
		ContentMessage msg2 = sm1.createContentMessage()
			.setMessage("hoe !");
		
		
		sm1.send(msg1, profile2);
		sm1.send(msg2, profile2);
		
		
		Thread.sleep(1000);
		
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(2, inbox.size());
		Assert.assertEquals(ContentMessage.class, inbox.get(0).getClass());
		Assert.assertEquals(ContentMessage.class, inbox.get(1).getClass());
		Assert.assertEquals("hi !", ((ContentMessage)inbox.get(0)).getMessage());
		Assert.assertEquals("hoe !", ((ContentMessage)inbox.get(1)).getMessage());
	}
	
	@Test
	public void itShouldSendShareAPostWithAnotherUser() throws Exception {
		
		for (int i=0; i < 2; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "20")
						.setProperty("openkad.bucket.kbuckets.maxsize", "20")
						.setProperty("openkad.net.udp.port", ""+(BASE_PORT+i)),
						
					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(BASE_PORT+i)),
					
					new SimpleDHTModule(),
						
					new ChunKeeperModule(),
					
					new StashModule(),
						
					new SocialFSCreatorModule()
						.setProperty("socialfs.user.username", "user_"+i)
						.setProperty("socialfs.user.password", "1234"),
					
					new SocialFSModule(),
					
					new EwolfAccountCreatorModule(),
					
					new EwolfModule()
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
			EwolfAccountCreator accountCreator = injector.getInstance(EwolfAccountCreator.class);
			accountCreator.create();
			System.out.println("done\n");
			
		}
		/*
		for (Injector injector : injectors) {
			System.out.println("login ...");
			SocialNetwork osn = injector.getInstance(SocialNetwork.class);
			osn.login("1234");
		}
		*/
		Thread.sleep(1000);
		
		SocialNetwork osn1 = injectors.get(0).getInstance(SocialNetwork.class);
		SocialNetwork osn2 = injectors.get(1).getInstance(SocialNetwork.class);
		
		SocialFS sfs1 = injectors.get(0).getInstance(SocialFS.class);
		SocialFS sfs2 = injectors.get(1).getInstance(SocialFS.class);
		
		SocialMail sm2 = injectors.get(1).getInstance(SocialMail.class);
		
		WolfPackLeader sgm1 = injectors.get(0).getInstance(WolfPackLeader.class);
		
		UserID uid1 = sfs1.getCredentials().getProfile().getUserId();
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());

		// osn1 adds osn2 to its friends group
		WolfPack osn1Friends = sgm1
			.findOrCreateSocialGroup("firends")
			.addMember(profile2);
		
		sgm1
			.findSocialGroup("wall-readers")
			.addMember(profile2);
		
		System.out.println(sgm1.getAllSocialGroups());
		
		// osn2 accepts the secret key and saves it
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(2, inbox.size());
		Assert.assertEquals(PokeMessage.class, inbox.get(0).getClass());
		((PokeMessage)inbox.get(0)).accept();
		((PokeMessage)inbox.get(1)).accept();
		
		
		// osn1 creates a post
		Post textPost = injectors.get(0).getInstance(TextPost.class).setText("hi !");
		osn1.getWall().publish(textPost, osn1Friends);
		
		Thread.sleep(1000);
		
		// osn2 searches the post
		Profile profile1 = inbox.get(0).getSender();
		Assert.assertEquals(uid1, profile1.getUserId());
		
		List<Post> posts = osn2.getWall(profile1).getAllPosts();
		Assert.assertEquals(1, posts.size());
		Assert.assertEquals(TextPost.class, posts.get(0).getClass());
		Assert.assertEquals("hi !", ((TextPost)posts.get(0)).getText());
	}
	
}
