package il.technion.ewolf.server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.Lock;
import org.apache.commons.configuration.PropertiesConfiguration;

public class ServerResources {	
	private static final String MIME_TYPES = "/mime.types";
	/** A lock object for protecting read/write operations to a configuration file. */
	private static Object configLock = new Lock("ConfigurationFileLock");

	public static class EwolfConfigurations {
		public String username;
		public String password;
		public String name;
		List<URI> kbrURIs = new ArrayList<URI>();
		int serverPort;
		int ewolfPort;
	}

	public static URL getResource(String name) {
		return ServerResources.class.getResource(name);
	}

	public static PropertiesConfiguration getProperties(String fileName) throws ConfigurationException {
		File f = new File("." + fileName);
		if (f.canRead())
			return new PropertiesConfiguration(f);

		URL resourceUrl = getResource(fileName);
		return new PropertiesConfiguration(resourceUrl);
	}
	public static void setUserConfigurations(String configurationFile, String username,
			String name, String password) throws ConfigurationException {
		try {
			synchronized (configLock) {
				PropertiesConfiguration config = getProperties(configurationFile);
				config.setProperty("username", username);
				config.setProperty("name", name);
				config.setProperty("password", password);
				config.save();
				configLock.notifyAll();
			}
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.out.println("Can't read configuration file.");
			throw e;
		}
	}

	public static EwolfConfigurations getConfigurations(String configurationFile)
			throws ConfigurationException {
		EwolfConfigurations configurations = new EwolfConfigurations();

		try {
			synchronized (configLock) {
				PropertiesConfiguration config = getProperties(configurationFile);
				configurations.username = config.getString("username");
				configurations.password = config.getString("password");
				configurations.name = config.getString("name");
				configurations.serverPort = config.getInt("serverPort", 10000);
				configurations.ewolfPort = config.getInt("ewolfPort", 10300);

				for (Object o: config.getList("kbr.urls")) {
					configurations.kbrURIs.add(new URI((String)o));
				}
			}
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.out.println("Can't read configuration file.");
			throw e;
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.out.println("Can't parse kbr URI string as valid URI in configuration file.");
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

	public static void waitForSignup(String config) {
		try {
			EwolfConfigurations c = getConfigurations(config);
			synchronized (configLock) {
				while (c.username == null || c.password == null
						|| c.name == null) {
					System.out.println("waiting for signup");
					configLock.wait();
					c = getConfigurations(config);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
