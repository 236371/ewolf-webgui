package il.technion.ewolf.server;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;

public interface ServerFileFactory {
	public interface ServerFile {
		public void read(String path) throws FileNotFoundException;
		public InputStream openStream() throws FileNotFoundException;
		public Date lastModified() throws FileNotFoundException,UnsupportedOperationException;
		public String getTag() throws FileNotFoundException,UnsupportedOperationException;
		public String contentType() throws FileNotFoundException;
	}
	
	public ServerFile newInstance();
}
