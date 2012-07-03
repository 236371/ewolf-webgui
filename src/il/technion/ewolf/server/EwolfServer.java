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
import il.technion.ewolf.server.handlers.AddWolfpackMemberHandler;
import il.technion.ewolf.server.handlers.CreateWolfpackHandler;
import il.technion.ewolf.server.handlers.InboxFetcher;
import il.technion.ewolf.server.handlers.NewsFeedFetcher;
import il.technion.ewolf.server.handlers.PostToNewsFeedHandler;
import il.technion.ewolf.server.handlers.ProfileFetcher;
import il.technion.ewolf.server.handlers.SendMessageHandler;
import il.technion.ewolf.server.handlers.WolfpackMembersFetcher;
import il.technion.ewolf.server.handlers.WolfpacksFetcher;
import il.technion.ewolf.server.handlers.JarResourceHandler;
import il.technion.ewolf.socialfs.SocialFSCreatorModule;
import il.technion.ewolf.socialfs.SocialFSModule;
import il.technion.ewolf.stash.StashModule;

import java.io.IOException;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class EwolfServer {
	
	private static final String EWOLF_CONFIG = "/ewolf.config.properties";
	
	EwolfConfigurations configurations;
	
	Injector itsInjector;
	
	HttpConnector connector;
	
	public EwolfServer(EwolfConfigurations configurations) {
		if(configurations == null) {
			throw new IllegalArgumentException();
		}
		
		this.configurations = configurations;
		
		this.itsInjector = createInjector();
	}

	public static void main(String[] args) throws Exception {		
		EwolfConfigurations myServerConfigurations = 
				ServerResources.getConfigurations(EWOLF_CONFIG);
		
		EwolfServer server = new EwolfServer(myServerConfigurations);
		server.initEwolf();
	}

	public void initEwolf() throws IOException, Exception {
		KeybasedRouting kbr = itsInjector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = itsInjector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		connector = itsInjector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
		
		EwolfAccountCreator accountCreator = 
				itsInjector.getInstance(EwolfAccountCreator.class);
		accountCreator.create();
		
		//FIXME port for testing
		kbr.join(configurations.kbrURIs);
		
		registerConnectorHandlers();
		
		System.out.println("server started");
	}
	
	private void registerConnectorHandlers() {
		//ewolf resources handlers register
//		connector.register("/json*", createJsonHandler() );
		connector.register("/json*", createJsonHandlerNew());

		//server resources handlers register
		connector.register("*", new JarResourceHandler());
//		connector.register("*", new HttpFileHandler("/",
//				new ServerResourceFactory(ServerResources.getFileTypeMap())));
	}
	
	private JsonHandlerNew createJsonHandlerNew() {
		return new JsonHandlerNew()
		.addHandler("inbox", itsInjector.getInstance(InboxFetcher.class))
		.addHandler("wolfpacks", itsInjector.getInstance(WolfpacksFetcher.class))
		.addHandler("profile", itsInjector.getInstance(ProfileFetcher.class))
		.addHandler("wolfpackMembers", itsInjector.getInstance(WolfpackMembersFetcher.class))
		.addHandler("newsFeed", itsInjector.getInstance(NewsFeedFetcher.class))
		.addHandler("createWolfpack", itsInjector.getInstance(CreateWolfpackHandler.class));
	}

	private JsonHandler createJsonHandler() {
		return new JsonHandler()
		.addHandler("profile", itsInjector.getInstance(ProfileFetcher.class))
		.addHandler("wolfpacks", itsInjector.getInstance(WolfpacksFetcher.class))
		.addHandler("wolfpackMembers", itsInjector.getInstance(WolfpackMembersFetcher.class))
		.addHandler("inbox", itsInjector.getInstance(InboxFetcher.class))
		.addHandler("newsFeed", itsInjector.getInstance(NewsFeedFetcher.class))
		.addHandler("createWolfpack", itsInjector.getInstance(CreateWolfpackHandler.class))
		.addHandler("addWolfpackMember", itsInjector.getInstance(AddWolfpackMemberHandler.class))
		.addHandler("post", itsInjector.getInstance(PostToNewsFeedHandler.class))
		.addHandler("sendMessage", itsInjector.getInstance(SendMessageHandler.class));
	}

	private Injector createInjector() {
		String port = String.valueOf(configurations.port);
		
		return Guice.createInjector(

				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "20")
					.setProperty("openkad.bucket.kbuckets.maxsize", "20")
					.setProperty("openkad.seed", port)
					.setProperty("openkad.net.udp.port", port),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", port),

				new SimpleDHTModule(),
					
				new ChunKeeperModule(),
				
				new StashModule(),
				
				new SocialFSCreatorModule()
					.setProperty("socialfs.user.username", 
							configurations.username)
					.setProperty("socialfs.user.password", 
							configurations.password)
					.setProperty("socialfs.user.name", 
							configurations.name),

				new SocialFSModule(),
				
				new EwolfAccountCreatorModule(),

				new EwolfModule()
		);
	}
}
