package il.technion.ewolf.server.jsonDataHandlers;

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
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.server.jsonDataHandlers.NewsFeedFetcher;
import il.technion.ewolf.server.jsonDataHandlers.NewsFeedFetcher.NewsFeedResponse;
import il.technion.ewolf.server.jsonDataHandlers.NewsFeedFetcher.PostData;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class NewsFeedFetcherTest {
	private static final int BASE_PORT = 10000;
	private List<Injector> injectors = new LinkedList<Injector>();
	
	static class JsonReqNewsFeedParams {
		String newsOf;
		String wolfpackName;
		String userID;
		Integer maxMessages;
		Long olderThan;
		Long newerThan;
	}
	
	@After
	public void cleanup() {
		for (Injector inj: injectors) {
			inj.getInstance(KeybasedRouting.class).shutdown();
			inj.getInstance(HttpConnector.class).shutdown();
		}
		injectors.clear();
	}
	
	public static JsonElement setNewsFeedParams(String newsOf, String wolfpackName, String userID,
			Integer maxMessages, Long olderThan, Long newerThan) {

		JsonReqNewsFeedParams params = new JsonReqNewsFeedParams();
		params.newsOf = newsOf;
		params.wolfpackName = wolfpackName;
		params.userID = userID;
		params.maxMessages = maxMessages;
		params.olderThan = olderThan;
		params.newerThan = newerThan;
		Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
		JsonElement jElem = gson.toJsonTree(params);
		return jElem;
	}
	
	@Test
	public void getAllPostsForUser() throws Exception {
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
		
		SocialFS sfs1 = injectors.get(0).getInstance(SocialFS.class);
		SocialFS sfs2 = injectors.get(1).getInstance(SocialFS.class);
		
		UserID uid1 = sfs1.getCredentials().getProfile().getUserId();
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		
		SocialMail sm2 = injectors.get(1).getInstance(SocialMail.class);
		
		WolfPackLeader sgm1 = injectors.get(0).getInstance(WolfPackLeader.class);
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		WolfPack osn1Friends = sgm1
				.findOrCreateSocialGroup("friends")
				.addMember(profile2);

		WolfPack osn1Enemies = sgm1
				.findOrCreateSocialGroup("enemies");
		sgm1
		.findSocialGroup("wall-readers")
		.addMember(profile2);
		
		// osn2 accepts the secret key and saves it
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(2, inbox.size());
		Assert.assertEquals(PokeMessage.class, inbox.get(0).getClass());
		((PokeMessage)inbox.get(0)).accept();
		((PokeMessage)inbox.get(1)).accept();
		
		// osn1 creates 10 posts
		Post[] posts = new Post[10];
		for (int i=0; i<4; i++) {
			posts[i] = injectors.get(0).getInstance(TextPost.class).setText("post " + i);
			osn1.getWall().publish(posts[i], osn1Friends);
		}
		Thread.sleep(1000);
		Post[] posts2 = new Post[10];
		for (int i=0; i<4; i++) {
			posts2[i] = injectors.get(0).getInstance(TextPost.class)
					.setText("SHOULDN'T GET: post " + i);
			osn1.getWall().publish(posts2[i], osn1Enemies);
		}
		Thread.sleep(1000);
		
		JsonElement params = setNewsFeedParams("user", null, uid1.toString(), null, null, null);
		NewsFeedResponse obj = ((NewsFeedResponse)injectors.get(1).getInstance(NewsFeedFetcher.class).handleData(params));
		Assert.assertEquals(obj.mailList.size(), 4);
		for (int i=0; i<4; i++) {
			PostData post = obj.mailList.get(i);
			Assert.assertEquals(uid1.toString(), post.senderID);
			Assert.assertEquals("post " + (3-i), post.mail);
		}

	}

}
