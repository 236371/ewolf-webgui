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
import il.technion.ewolf.server.handlers.AddSocialGroupHandler;
import il.technion.ewolf.server.handlers.InboxFetcher;
import il.technion.ewolf.server.handlers.NewsFeedFetcher;
import il.technion.ewolf.server.handlers.ProfileFetcher;
import il.technion.ewolf.server.handlers.WolfpackMembersFetcher;
import il.technion.ewolf.server.handlers.WolfpacksFetcher;
import il.technion.ewolf.server.handlersOld.AddMessageBoardPostHandler;
import il.technion.ewolf.server.handlersOld.AddSocialGroupMemberHandler;
import il.technion.ewolf.socialfs.SocialFSCreatorModule;
import il.technion.ewolf.socialfs.SocialFSModule;
import il.technion.ewolf.stash.StashModule;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;

@SuppressWarnings("deprecation")
public class EwolfServer {

	public static void main(String[] args) throws Exception {
		EwolfConfigurations configurations = 
				ServerResources.getConfigurations();
		
		Injector injector = createInjector(configurations.username,
				configurations.password, configurations.name);
		
		HttpConnector connector = initEwolf(configurations.kbrURIs, injector);

		registerConnectorHandlers(injector, connector);
		
		System.out.println("server started");
	}

	private static void registerConnectorHandlers(Injector injector, HttpConnector connector) {
		//ewolf resources handlers register
		connector.register(AddSocialGroupMemberHandler.getRegisterPattern(), injector.getInstance(AddSocialGroupMemberHandler.class));
		connector.register(AddMessageBoardPostHandler.getRegisterPattern(), injector.getInstance(AddMessageBoardPostHandler.class));

		//server resources handlers register
		JsonHandler serverJsonHandler = createJsonHandler(injector);
		connector.register("/json*", serverJsonHandler );
		connector.register("*", new HttpFileHandler("/",
				new ServerResourceFactory(ServerResources.getFileTypeMap())));
	}


	private static HttpConnector initEwolf(List<URI> kbrURIs, Injector injector)
			throws IOException, Exception {
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		HttpConnector connector = injector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
		
		EwolfAccountCreator accountCreator = injector.getInstance(EwolfAccountCreator.class);
		accountCreator.create();
		
		//FIXME port for testing
		kbr.join(kbrURIs);
		return connector;
	}


	private static Injector createInjector(String username, String password,
			String name) {		
		ServerModule serverModule = new ServerModule();
		
		Injector injector = Guice.createInjector(

				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "20")
					.setProperty("openkad.bucket.kbuckets.maxsize", "20")
					.setProperty("openkad.seed", serverModule.getPort())
					.setProperty("openkad.net.udp.port", serverModule.getPort()),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", serverModule.getPort()),

				new SimpleDHTModule(),
					
				new ChunKeeperModule(),
				
				new StashModule(),
				
				new SocialFSCreatorModule()
					.setProperty("socialfs.user.username", username)
					.setProperty("socialfs.user.password", password)
					.setProperty("socialfs.user.name", name),

				new SocialFSModule(),
				
				new EwolfAccountCreatorModule(),

				new EwolfModule(),

					serverModule
		);
		return injector;
	}
	
	private static JsonHandler createJsonHandler(Injector injector) {
		return new JsonHandler()
		.addHandler("profile", injector.getInstance(ProfileFetcher.class))
		.addHandler("wolfpacks", injector.getInstance(WolfpacksFetcher.class))
		.addHandler("wolfpackMembers", injector.getInstance(WolfpackMembersFetcher.class))
		.addHandler("inbox", injector.getInstance(InboxFetcher.class))
		.addHandler("newsFeed", injector.getInstance(NewsFeedFetcher.class))
		.addHandler("createWolfpack", injector.getInstance(AddSocialGroupHandler.class));
	}
}
