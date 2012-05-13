package il.technion.ewolf.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

public class ServerResourceFactory implements ServerFileFactory {
	
	@Override
	public ServerFile newInstance() {
		return new ServerFile() {
			
			URL url = null;
			
			@Override
			public void read(String path) throws FileNotFoundException {
				if(path.isEmpty()) {
					path = "home.html";
				}
				
				url = getClass().getResource(path);
				
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
				
				if(path.endsWith(".ico") || path.endsWith(".jpg")
						|| path.endsWith(".gif")) {					
					return "image/gif";
				} else if(path.endsWith(".html") || path.endsWith(".htm") ) {
					return "text/html";
				} else if(path.endsWith(".js")) {
					return "text/javascript";
				} else if(path.endsWith(".css")) {
					return "text/css";
				} else {
					return "application/unknown";
				}
			}
		};
	}

	

}
