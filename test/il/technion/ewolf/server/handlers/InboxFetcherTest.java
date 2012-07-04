package il.technion.ewolf.server.handlers;

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
import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.server.handlers.InboxFetcher;
import il.technion.ewolf.server.handlers.InboxFetcher.InboxMessage;
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

@SuppressWarnings("unchecked")
public class InboxFetcherTest {
	private static final int BASE_PORT = 10000;
	private List<Injector> injectors = new LinkedList<Injector>();

	class JsonReqInboxParams {
		//The max amount of messages to retrieve.
		Integer maxMessages;
		//Time in milliseconds since 1970, to retrieve messages older than this date.
		Long olderThan;
		//Time in milliseconds since 1970, to retrieve messages newer than this date.
		Long newerThan;
		//User ID, to retrieve messages from a specific sender.
		String fromSender;
	}
	
	@After
	public void cleanup() {
		for (Injector inj: injectors) {
			inj.getInstance(KeybasedRouting.class).shutdown();
			inj.getInstance(HttpConnector.class).shutdown();
		}
		injectors.clear();
	}
	
	@Test
	public void getFullSortedInbox() throws Exception {
		for (int i=0; i < 3; ++i) {
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
		SocialFS sfs3 = injectors.get(2).getInstance(SocialFS.class);
		
		
		SocialMail sm1 = injectors.get(0).getInstance(SocialMail.class);
		SocialMail sm2 = injectors.get(1).getInstance(SocialMail.class);
		SocialMail sm3 = injectors.get(2).getInstance(SocialMail.class);
		
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		UserID uid3 = sfs3.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		ContentMessage[] messages = new ContentMessage[10];
		for (int i=0; i<10; i++) {
			messages[i] = sm1.createContentMessage().setMessage("msg " + i + " from user1");
			sm1.send(messages[i], profile2);
		}
		for (int i=0; i<10; i++) {
			messages[i] = sm3.createContentMessage().setMessage("msg " + i + " from user3");
			sm3.send(messages[i], profile2);
		}

		Thread.sleep(1000);
		
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(20, inbox.size());

		JsonElement params = setInboxParams(null, null, null, null);
		List<InboxMessage> lst = ((List<InboxMessage>)injectors.get(1).getInstance(InboxFetcher.class).handleData(params));
		for (int i=0; i<10; i++) {
			InboxMessage im = lst.get(i);
			Assert.assertEquals(im.message,"msg " + (9-i) + " from user3");
			Assert.assertEquals(im.senderID, uid3.toString());
		}
		
	}

	private JsonElement setInboxParams(Integer maxMessages, Long olderThan,
			Long newerThan, String fromSender) {

		JsonReqInboxParams params = new JsonReqInboxParams();
		params.fromSender = fromSender;
		params.maxMessages = maxMessages;
		params.newerThan = newerThan;
		params.olderThan = olderThan;
		Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
		JsonElement jElem = gson.toJsonTree(params);
		return jElem;
	}

	@Test
	public void getSortedMessagesFromSpecificUser() throws Exception {
		for (int i=0; i < 3; ++i) {
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
		SocialMail sm3 = injectors.get(2).getInstance(SocialMail.class);
		
		UserID uid1 = sfs1.getCredentials().getProfile().getUserId();
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		ContentMessage[] messages = new ContentMessage[10];
		for (int i=0; i<10; i++) {
			messages[i] = sm1.createContentMessage().setMessage("msg " + i + " from user1");
			sm1.send(messages[i], profile2);
		}
		for (int i=0; i<10; i++) {
			messages[i] = sm3.createContentMessage().setMessage("msg " + i + " from user3");
			sm3.send(messages[i], profile2);
		}
		
		
		Thread.sleep(1000);
		
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(20, inbox.size());
		JsonElement params = setInboxParams(null, null, null, uid1.toString());
		List<InboxMessage> lst = ((List<InboxMessage>)injectors.get(1).getInstance(InboxFetcher.class).handleData(params));
		for (int i=0; i<10; i++) {
			InboxMessage im = lst.get(i);
			Assert.assertEquals(im.message,"msg " + (9-i) + " from user1");
			Assert.assertEquals(im.senderID, uid1.toString());
		}
		
	}
	
	@Test
	public void getSortedInboxBetweenTwoTimestamps() throws Exception {
		for (int i=0; i < 3; ++i) {
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
		SocialFS sfs3 = injectors.get(2).getInstance(SocialFS.class);
		
		
		SocialMail sm1 = injectors.get(0).getInstance(SocialMail.class);
		SocialMail sm2 = injectors.get(1).getInstance(SocialMail.class);
		SocialMail sm3 = injectors.get(2).getInstance(SocialMail.class);
		
		UserID uid1 = sfs1.getCredentials().getProfile().getUserId();
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		UserID uid3 = sfs3.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		ContentMessage[] messages = new ContentMessage[10];
		Long[] timestamps = new Long[21];
		for (int i=0; i<10; i++) {
			timestamps[i]=System.currentTimeMillis();
			messages[i] = sm1.createContentMessage().setMessage("msg " + i + " from user1");
			sm1.send(messages[i], profile2);
		}
		for (int i=0; i<10; i++) {
			timestamps[10+i]=System.currentTimeMillis();
			messages[i] = sm3.createContentMessage().setMessage("msg " + i + " from user3");
			sm3.send(messages[i], profile2);
		}
		timestamps[20]=System.currentTimeMillis();
		
		
		Thread.sleep(1000);
		
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(20, inbox.size());
		JsonElement params = setInboxParams(null, timestamps[20], timestamps[0], null);
		List<InboxMessage> lst = ((List<InboxMessage>)injectors.get(1).getInstance(InboxFetcher.class)
				.handleData(params));
		for (int i=0; i<10; i++) {
			InboxMessage im = lst.get(i);
			Assert.assertEquals(im.message,"msg " + (9-i) + " from user3");
			Assert.assertEquals(im.senderID, uid3.toString());
		}
		for (int i=10; i<20; i++) {
			InboxMessage im = lst.get(i);
			Assert.assertEquals(im.message,"msg " + (19-i) + " from user1");
			Assert.assertEquals(im.senderID, uid1.toString());
		}
		
	}
	
	@Test
	public void get5InboxMessagesFromSpecificUser() throws Exception {
		for (int i=0; i < 3; ++i) {
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
		SocialMail sm3 = injectors.get(2).getInstance(SocialMail.class);
		
		UserID uid1 = sfs1.getCredentials().getProfile().getUserId();
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		ContentMessage[] messages = new ContentMessage[10];
		for (int i=0; i<10; i++) {
			messages[i] = sm1.createContentMessage().setMessage("msg " + i + " from user1");
			sm1.send(messages[i], profile2);
		}
		for (int i=0; i<10; i++) {
			messages[i] = sm3.createContentMessage().setMessage("msg " + i + " from user3");
			sm3.send(messages[i], profile2);
		}
		
		
		Thread.sleep(1000);
		
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(20, inbox.size());
		JsonElement params = setInboxParams(5, null, null, uid1.toString());
		List<InboxMessage> lst = ((List<InboxMessage>)injectors.get(1).getInstance(InboxFetcher.class).handleData(params));
		Assert.assertEquals(lst.size(), 5);
		for (int i=0; i<5; i++) {
			InboxMessage im = lst.get(i);
			Assert.assertEquals(im.message,"msg " + (9-i) + " from user1");
			Assert.assertEquals(im.senderID, uid1.toString());
		}	
	}
	
	@Test
	public void get5messagesBetweenTwoTimestamps() throws Exception {
		for (int i=0; i < 3; ++i) {
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
		SocialFS sfs3 = injectors.get(2).getInstance(SocialFS.class);
		
		
		SocialMail sm1 = injectors.get(0).getInstance(SocialMail.class);
		SocialMail sm2 = injectors.get(1).getInstance(SocialMail.class);
		SocialMail sm3 = injectors.get(2).getInstance(SocialMail.class);
		
		UserID uid2 = sfs2.getCredentials().getProfile().getUserId();
		UserID uid3 = sfs3.getCredentials().getProfile().getUserId();
		
		// osn1 finds osn2
		Profile profile2 = sfs1.findProfile(uid2);
		Assert.assertEquals(uid2, profile2.getUserId());
		
		ContentMessage[] messages = new ContentMessage[10];
		Long[] timestamps = new Long[21];
		for (int i=0; i<10; i++) {
			timestamps[i]=System.currentTimeMillis();
			messages[i] = sm1.createContentMessage().setMessage("msg " + i + " from user1");
			sm1.send(messages[i], profile2);
		}
		for (int i=0; i<10; i++) {
			timestamps[10+i]=System.currentTimeMillis();
			messages[i] = sm3.createContentMessage().setMessage("msg " + i + " from user3");
			sm3.send(messages[i], profile2);
		}
		timestamps[20]=System.currentTimeMillis();
		
		
		Thread.sleep(1000);
		
		List<SocialMessage> inbox = sm2.readInbox();
		Assert.assertEquals(20, inbox.size());
		JsonElement params = setInboxParams(5, timestamps[20], timestamps[18], null);
		List<InboxMessage> lst = ((List<InboxMessage>)injectors.get(1).getInstance(InboxFetcher.class)
				.handleData(params));
		Assert.assertEquals(lst.size(), 2);
		for (int i=0; i<2; i++) {
			InboxMessage im = lst.get(i);
			Assert.assertEquals(im.message,"msg " + (9-i) + " from user3");
			Assert.assertEquals(im.senderID, uid3.toString());
		}
	}
}
