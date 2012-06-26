package il.technion.ewolf.server;

import il.technion.ewolf.EwolfAccountCreator;
import il.technion.ewolf.EwolfAccountCreatorModule;
import il.technion.ewolf.EwolfModule;
import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.ChunKeeperModule;
import il.technion.ewolf.dht.SimpleDHTModule;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;
import il.technion.ewolf.server.ServerResources.EwolfConfigurations;
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
	private static final String SERVER_PORT_1 = "10000";
	
	private static final String EWOLF_CONFIG_2 = "/ewolf2.config.properties";
	private static final String SERVER_PORT_2 = "10200";

	public static void main(String[] args) throws Exception {
		List<Injector> injectors = new LinkedList<Injector>();

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

		EwolfConfigurations configurations1 = 
				ServerResources.getConfigurations(EWOLF_CONFIG_1);
		ServerModule serverModule1 = new ServerModule(SERVER_PORT_1);
		
		EwolfServer server1 = new EwolfServer(configurations1, serverModule1);
		server1.initEwolf();
		
		EwolfConfigurations configurations2 = 
				ServerResources.getConfigurations(EWOLF_CONFIG_2);
		ServerModule serverModule2 = new ServerModule(SERVER_PORT_2);
		
		EwolfServer server2 = new EwolfServer(configurations2, serverModule2);
		server2.initEwolf();
		
		System.out.println("Server test is ready...");

//		List<SocialFS> sfsList = new ArrayList<SocialFS>();
//		List<UserID> userIDList = new ArrayList<UserID>();
//		List<String> strUidList = new ArrayList<String>();
//		List<Profile> profileList = new ArrayList<Profile>();
//		List<SocialMail> smailsList = new ArrayList<SocialMail>();
//		List<WolfPackLeader> wolfpackLeadersList = new ArrayList<WolfPackLeader>();
//		List<SocialNetwork> socNetsList = new ArrayList<SocialNetwork>();
//		List<TextPost> textPostsList = new ArrayList<TextPost>();
//		
//		for (int i=0; i<injectors.size(); i++) {
//			sfsList.add(i, injectors.get(i).getInstance(SocialFS.class));
//			profileList.add(i, sfsList.get(i).getCredentials().getProfile());
//			userIDList.add(i, profileList.get(i).getUserId());
//			strUidList.add(i, userIDList.get(i).toString());
//			smailsList.add(i, injectors.get(i).getInstance(SocialMail.class));
//			wolfpackLeadersList.add(i, injectors.get(i).getInstance(WolfPackLeader.class));
//			socNetsList.add(i, injectors.get(i).getInstance(SocialNetwork.class));
//			textPostsList.add(i, injectors.get(i).getInstance(TextPost.class));
//		}
//		
//		UserIDFactory userIDFactory = injectors.get(0).getInstance(UserIDFactory.class);
//		UserID uid = userIDFactory.getFromBase64(EwolfServer.userID);
//		Profile profile = sfsList.get(0).findProfile(uid);
//		
//		//send messages to main user
//		ContentMessage[] messages = new ContentMessage[5];
//		for (int i=0; i<injectors.size(); i++) {
//			SocialMail sm = smailsList.get(i);
//			String strUid = strUidList.get(i);
//			for (int j=0; j<5; j++) {
//				messages[i] = sm.createContentMessage().setMessage(strUid + ": msg " + j);
//				sm.send(messages[i], profile);
//			}
//		}
//		//post messages to main user
//		for (int i=0; i<injectors.size(); i++) {
//			WolfPackLeader sgm = wolfpackLeadersList.get(i);
//			WolfPack friends = sgm.findOrCreateSocialGroup("friends").addMember(profile);
//			//main user SHOULDN't get posts to enemies 
//			WolfPack enemies = sgm.findOrCreateSocialGroup("enemies");
//			sgm.findSocialGroup("wall-readers").addMember(profile);
//			
//			SocialNetwork sn = socNetsList.get(i);
//			String strUid = strUidList.get(i);
//			Post[] posts = new Post[5];
//			//send posts to friends
//			for (int j=0; j<5; j++) {				 
//				posts[i] = textPostsList.get(i)
//						.setText("Post to friends: post " + j + "from " + strUid);
//				sn.getWall().publish(posts[i], friends);
//			}
//			//send posts to enemies (main user SHOULDN't get them)
//			for (int j=0; j<5; j++) {				 
//				posts[i] = textPostsList.get(i)
//						.setText("Post to enemies: post " + j + "from " + strUid);
//				sn.getWall().publish(posts[i], enemies);
//			}
//		}
	}
}
