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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.inject.Guice;
import com.google.inject.Injector;

@SuppressWarnings("deprecation")
public class EwolfServer {
	
	private static final String EWOLF_CONFIG = "ewolf.config.properties";
	private static final String MIME_TYPES = "mime.types";
	
	private static class EwolfConfigurations {
		public String username;
		public String password;
		public String name;
		public List<URI> kbrURIs = new ArrayList<URI>();
	}


	public static void main(String[] args) throws Exception {
		EwolfConfigurations configurations = getConfigurations();
		
		Injector injector = createInjector(configurations.username,
				configurations.password, configurations.name);
		
		HttpConnector connector = initEwolf(configurations.kbrURIs, injector);

		registerConnectorHandlers(injector, connector);
		
		System.out.println("server started");
	}


	private static EwolfConfigurations getConfigurations() {
		EwolfConfigurations configurations = new EwolfConfigurations();
		
		try {
			PropertiesConfiguration config = new PropertiesConfiguration(EWOLF_CONFIG);
			configurations.username = config.getString("username");
			configurations.password = config.getString("password");
			configurations.name = config.getString("name");
			for (Object o: config.getList("kbr.urls")) {
				configurations.kbrURIs.add(new URI((String)o));
			}
			if (configurations.username == null) {
				//TODO get username/password from user, store to EWOLF_CONFIG and continue
			}
		} catch (ConfigurationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			System.out.println("Cant' read configuration file");
			return null;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("String from configuration file could not be parsed as a URI");
			return null;
		}
		
		return configurations;
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
		
		JsonHandler serverJsonHandler = createJsonHandler();
		
		connector.register("/json*", serverJsonHandler );
		connector.register("*", new HttpFileHandler("/",
				new ServerResourceFactory(getFileTypeMap())));
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
	
	private static FileTypeMap getFileTypeMap() {
		MimetypesFileTypeMap map;
		try {
			URL mime = EwolfServer.class.getResource(MIME_TYPES);			
			map = new MimetypesFileTypeMap(mime.openStream());
		} catch (IOException e1) {
			map = new MimetypesFileTypeMap();
		}
		return map;
	}
	
	private static JsonHandler createJsonHandler() {
		JsonHandler serverJsonHandler = new JsonHandler();
		serverJsonHandler.addFetcher("wolfpacks", new WolfpacksFetcher());
		serverJsonHandler.addFetcher("inbox", new InboxFetcher());
		serverJsonHandler.addFetcher("message", new MessageFetcher());
		return serverJsonHandler;
	}
}