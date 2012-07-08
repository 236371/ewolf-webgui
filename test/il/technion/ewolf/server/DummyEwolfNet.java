package il.technion.ewolf.server;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.ChunKeeperModule;
import il.technion.ewolf.dht.SimpleDHTModule;
import il.technion.ewolf.ewolf.EwolfAccountCreator;
import il.technion.ewolf.ewolf.EwolfAccountCreatorModule;
import il.technion.ewolf.ewolf.EwolfModule;
import il.technion.ewolf.ewolf.SocialNetwork;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;
import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.server.ServerResources.EwolfConfigurations;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.SocialFSCreatorModule;
import il.technion.ewolf.socialfs.SocialFSModule;
import il.technion.ewolf.stash.StashModule;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DummyEwolfNet {
	private static final int BASE_PORT = 10100;
	
	private static final String EWOLF_CONFIG_1 = "/ewolf.config.properties";
	
	private static final String EWOLF_CONFIG_2 = "/ewolf2.config.properties";

	public static void main(String[] args) throws Exception {
		List<Injector> injectors = new LinkedList<Injector>();

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

		EwolfConfigurations configurations1 = 
				ServerResources.getConfigurations(EWOLF_CONFIG_1);
		
		EwolfServer server1 = new EwolfServer(configurations1);
		server1.initEwolf();
		
		EwolfConfigurations configurations2 = 
				ServerResources.getConfigurations(EWOLF_CONFIG_2);
		
		EwolfServer server2 = new EwolfServer(configurations2);
		server2.initEwolf();
		
		//user2
		Injector inj2 = server2.itsInjector;
		SocialMail sm2 = inj2.getInstance(SocialMail.class);
		SocialFS sfs2 = inj2.getInstance(SocialFS.class);
		Profile profile2 = sfs2.getCredentials().getProfile();
		String strUid2 = profile2.getUserId().toString();
		WolfPackLeader sgm2 = inj2.getInstance(WolfPackLeader.class);
		SocialNetwork sn2 = inj2.getInstance(SocialNetwork.class);
		TextPost textPost2 = inj2.getInstance(TextPost.class);
		
		//user1
		Injector inj1 = server1.itsInjector;
		SocialFS sfs1 = inj1.getInstance(SocialFS.class);
		Profile profile1 = sfs1.getCredentials().getProfile();
		
		//send messages from user2 to user1
		ContentMessage[] messages = new ContentMessage[5];
		for (int j=0; j<5; j++) {
			String msg = "{\"text\":\""+
					strUid2 + ": msg " + j +
					"\",\"attachment\":[{\"filename\":\"testfile.doc\",\"contentType\":\"document\",\"path\":\"http://www.google.com\"},{\"filename\":\"israel.jpg\",\"contentType\":\"image/jpeg\",\"path\":\"https://www.cia.gov/library/publications/the-world-factbook/graphics/flags/large/is-lgflag.gif\"},{\"filename\":\"spain.jpg\",\"contentType\":\"image/jpeg\",\"path\":\"https://www.cia.gov/library/publications/the-world-factbook/graphics/flags/large/sp-lgflag.gif\"}]}";
			messages[j] = sm2.createContentMessage().setMessage(msg);
			sm2.send(messages[j], profile1);
		}
		
		//post messages to user1
		WolfPack friends = sgm2.findOrCreateSocialGroup("friends").addMember(profile1);
		//user1 SHOULDN't get posts to enemies 
		WolfPack enemies = sgm2.findOrCreateSocialGroup("enemies");

		sgm2.findSocialGroup("wall-readers").addMember(profile1);
		
		Post[] posts = new Post[5];
		//send posts to friends
		for (int j=0; j<5; j++) {				 
			posts[j] = textPost2.setText("Post to friends: post " + j + "from " + strUid2);
			sn2.getWall().publish(posts[j], friends);
		}
		//send posts to enemies (main user SHOULDN't get them)
		for (int j=0; j<5; j++) {				 
			posts[j] = textPost2.setText("Post to enemies: post " + j + "from " + strUid2);
			sn2.getWall().publish(posts[j], enemies);
		}
		
		System.out.println("Server test is ready...");
	}
}
