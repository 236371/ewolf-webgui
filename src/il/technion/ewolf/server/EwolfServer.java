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
import il.technion.ewolf.server.handlers.AddNewSocialGroupHandler;
import il.technion.ewolf.server.handlers.AddSocialGroupMemberHandler;
import il.technion.ewolf.server.handlers.ViewMessageBoardHandler;
import il.technion.ewolf.server.handlers.ViewProfileHandler;
import il.technion.ewolf.server.handlers.ViewSelfProfileHandler;
import il.technion.ewolf.server.handlers.ViewSocialGroupMembersHandler;
import il.technion.ewolf.socialfs.SocialFSCreatorModule;
import il.technion.ewolf.socialfs.SocialFSModule;
import il.technion.ewolf.stash.StashModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class EwolfServer {
	
	private static final String EWOLF_CONFIG = "ewolf.config.properties";


	public static void main(String[] args) throws Exception {
/*
		try {
			PropertiesConfiguration config = new PropertiesConfiguration(EWOLF_CONFIG);
			config.setProperty("username", "user");
			config.setProperty("password", "1234");
			config.save(EWOLF_CONFIG);
		} catch (ConfigurationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
*/
		String username;
		String password;
		String name;
		List<URI> kbrURIs = new ArrayList<URI>();
		try {
			PropertiesConfiguration config = new PropertiesConfiguration(EWOLF_CONFIG);
			username = config.getString("username");
			password = config.getString("password");
			name = config.getString("name");
			for (Object o: config.getList("kbr.urls")) {
				kbrURIs.add(new URI((String)o));
			}
			if (username == null) {
				//TODO get username/password from user, store to EWOLF_CONFIG and continue
			}
		} catch (ConfigurationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			System.out.println("Cant' read configuration file");
			return;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("String from configuration file could not be parsed as a URI");
			return;
		}

		ServerModule serverModule = new ServerModule();
		
		Injector injector = Guice.createInjector(
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

				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", serverModule.getPort()),

					serverModule,

				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "20")
					.setProperty("openkad.bucket.kbuckets.maxsize", "20")
					.setProperty("openkad.seed", serverModule.getPort())
					.setProperty("openkad.net.udp.port", serverModule.getPort())
		);
		
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		//FIXME port for testing
		kbr.join(kbrURIs);
		
		HttpConnector connector = injector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
		
		EwolfAccountCreator accountCreator = injector.getInstance(EwolfAccountCreator.class);
		accountCreator.create();

		//server resources handlers register
		connector.register("/json*", new JsonHandler() );
		connector.register("/file*", new HttpFileHandler("/",new ServerResourceFactory()));
		//ewolf resources handlers register
		connector.register("/viewSelfProfile", injector.getInstance(ViewSelfProfileHandler.class));
		connector.register("/addSocialGroup", injector.getInstance(AddNewSocialGroupHandler.class));
		connector.register("/viewProfile/*", injector.getInstance(ViewProfileHandler.class));
		connector.register("/viewSocialGroupMembers/*", injector.getInstance(ViewSocialGroupMembersHandler.class));
		connector.register("/addTextPost/*", injector.getInstance(AddMessageBoardPostHandler.class));
		connector.register("/viewMessageBoard/*", injector.getInstance(ViewMessageBoardHandler.class));
		connector.register("/addSocialGroupMember/*", injector.getInstance(AddSocialGroupMemberHandler.class));

		System.out.println("server started");
	}
}