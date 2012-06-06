package il.technion.ewolf.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class ServerResourceFactory implements ServerFileFactory {
	private static final String MIME_TYPES = "mime.properties";
	private final String CWD = System.getProperty("user.dir") + "/";
	
	@Override
	public ServerFile newInstance() {
		return new ServerFile() {
			
			URL url = null;
			
			@Override
			public void read(String path) throws FileNotFoundException {
				if(path.isEmpty()) {
					path = "home.html";
				}
				
				//url = getClass().getResource(path);
				try {
					url = new URL("file", "", CWD + path);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(url == null) {
					throw new FileNotFoundException();
				}
			}

			@Override
			public InputStream openStream() throws FileNotFoundException {
				try {
					return url.openStream();
				} catch (IOException e) {
					throw new FileNotFoundException();
				}
			}

			@Override
			public Date lastModified() throws FileNotFoundException {
				if(url == null) {
					throw new FileNotFoundException();
				}
				
				try {
					return new Date(url.openConnection().getLastModified());
				} catch (IOException e) {
					throw new FileNotFoundException();
				}
			}

			@Override
			public String getTag() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public String contentType() throws FileNotFoundException {
				if(url == null) {
					throw new FileNotFoundException();
				}

				String path = url.getPath();
				String contentType = null;

				try {
					PropertiesConfiguration config = new PropertiesConfiguration(MIME_TYPES);
					String extension = path.substring( path.lastIndexOf('.'));
					contentType = config.getString(extension);
					System.out.println(contentType);
				} catch (ConfigurationException e2) {
					// TODO Auto-generated catch block
					System.out.println("Can't read configuration file:" + MIME_TYPES);
					e2.printStackTrace();
				}
				
				if (contentType == null) {
					return "application/unknown";
				}
				else {
					return contentType;
				}

//				if(path.endsWith(".ico") || path.endsWith(".gif")) {
//					return "image/gif";
//				} else if(path.endsWith(".jpg") || path.endsWith(".jpeg")) {
//					return "image/jpeg";
//				} else if(path.endsWith(".svg")) {
//					return "image/svg+xml";
//				} else if(path.endsWith(".html") || path.endsWith(".htm") ) {
//					return "text/html";
//				} else if(path.endsWith(".js")) {
//					return "text/javascript";
//				} else if(path.endsWith(".css")) {
//					return "text/css";
//				} else {
//					return "application/unknown";
//				}
			}
		};
	}

	

}
