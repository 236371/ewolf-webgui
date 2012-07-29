package il.technion.ewolf.server;

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

public class ServerResources {	
	private static final String MIME_TYPES = "/mime.types";
	
	static class EwolfConfigurations {
		String username;
		String password;
		String name;
		List<URI> kbrURIs = new ArrayList<URI>();
		int serverPort;
		int ewolfPort;
	}
	
	public static URL getResource(String name) {
		return ServerResources.class.getResource(name);
	}
	
	public static EwolfConfigurations getConfigurations(String configurationFile) 
			throws ConfigurationException {
		EwolfConfigurations configurations = new EwolfConfigurations();
		
		try {
			URL configFile = getResource(configurationFile);
			PropertiesConfiguration config = new PropertiesConfiguration(configFile);
			configurations.username = config.getString("username");
			configurations.password = config.getString("password");
			configurations.name = config.getString("name");
			configurations.serverPort = config.getInt("serverPort", 10000);
			configurations.ewolfPort = config.getInt("ewolfPort", 10300);

			for (Object o: config.getList("kbr.urls")) {
				configurations.kbrURIs.add(new URI((String)o));
			}
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.out.println("Cant' read configuration file");
			throw e;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.out.println("String from configuration file could not be parsed as a URI");
			throw new ConfigurationException(e);
		}
		
		return configurations;
	}
	
	public static FileTypeMap getFileTypeMap() {
		MimetypesFileTypeMap map;
		try {
			URL mime = getResource(MIME_TYPES);
			map = new MimetypesFileTypeMap(mime.openStream());
		} catch (IOException e1) {
			map = new MimetypesFileTypeMap();
		}
		return map;
	}
}
