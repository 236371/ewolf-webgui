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
import il.technion.ewolf.server.fetchers.InboxFetcher;
import il.technion.ewolf.server.fetchers.ProfileFetcher;
import il.technion.ewolf.server.fetchers.WolfpackMembersFetcher;
import il.technion.ewolf.server.fetchers.WolfpacksFetcher;
import il.technion.ewolf.server.handlers.AddMessageBoardPostHandler;
import il.technion.ewolf.server.handlers.AddSocialGroupHandler;
import il.technion.ewolf.server.handlers.AddSocialGroupMemberHandler;
import il.technion.ewolf.server.handlers.ViewInboxHandler;
import il.technion.ewolf.server.handlers.ViewMessageBoardHandler;
import il.technion.ewolf.server.handlers.ViewProfileHandler;
import il.technion.ewolf.server.handlers.ViewSelfProfileHandler;
import il.technion.ewolf.server.handlers.ViewSocialGroupMembersHandler;
import il.technion.ewolf.server.handlers.ViewSocialGroupsHandler;
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
		ServerResources.EwolfConfigurations configurations = 
				ServerResources.getConfigurations();
		
		Injector injector = createInjector(configurations.username,
				configurations.password, configurations.name);
		
		HttpConnector connector = initEwolf(configurations.kbrURIs, injector);

		registerConnectorHandlers(injector, connector);
		
		System.out.println("server started");
	}

	private static void registerConnectorHandlers(Injector injector, HttpConnector connector) {
		//ewolf resources handlers register
		connector.register(ViewSelfProfileHandler.getRegisterPattern(), injector.getInstance(ViewSelfProfileHandler.class));
		connector.register(ViewProfileHandler.getRegisterPattern(), injector.getInstance(ViewProfileHandler.class));
		connector.register(ViewSocialGroupsHandler.getRegisterPattern(), injector.getInstance(ViewSocialGroupsHandler.class));
		connector.register(AddSocialGroupHandler.getRegisterPattern(), injector.getInstance(AddSocialGroupHandler.class));
		connector.register(ViewSocialGroupMembersHandler.getRegisterPattern(), injector.getInstance(ViewSocialGroupMembersHandler.class));
		connector.register(AddSocialGroupMemberHandler.getRegisterPattern(), injector.getInstance(AddSocialGroupMemberHandler.class));
		connector.register(AddMessageBoardPostHandler.getRegisterPattern(), injector.getInstance(AddMessageBoardPostHandler.class));
		connector.register(ViewMessageBoardHandler.getRegisterPattern(), injector.getInstance(ViewMessageBoardHandler.class));
		connector.register(ViewInboxHandler.getRegisterPattern(), injector.getInstance(ViewInboxHandler.class));
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
		.addFetcher("profile", injector.getInstance(ProfileFetcher.class))
		.addFetcher("wolfpacks", injector.getInstance(WolfpacksFetcher.class))
		.addFetcher("wolfpackMembers", injector.getInstance(WolfpackMembersFetcher.class))
		.addFetcher("inbox", new InboxFetcher());
	}
}