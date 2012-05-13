package il.technion.ewolf.server;

import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.server.ServerFileFactory.ServerFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.InputStreamEntity;

import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class HttpFileHandler implements HttpRequestHandler {
	
	ServerFileFactory fileFactory;
	final String prefix;
	
	public HttpFileHandler(
			String					inputPrefix,
			String					inputRegEx,
			ServerFileFactory		inputFileFactory, 
			HttpConnector			inputConnector) {
		super();
		
		fileFactory = inputFileFactory;
		prefix = inputPrefix;
		inputConnector.register(inputRegEx, this);
	}
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx)
			throws HttpException, IOException {
		String resName = req.getRequestLine().getUri();
		
		System.out.println("\t[HttpFileHandler] requesting: " + resName);
		
		addGeneralHeaders(res);
		
		ServerFile file = fileFactory.newInstance();
		if(!loadFile(file,resName)) {
			res.setStatusCode(404);
			System.out.println("\t[HttpFileHandler] file not found");
			// Do not exit. Need to send a 404 page.
		}
		
		res.addHeader("Content-Type", file.contentType());
		
		if(!isModified(file,req,res)) {
			res.setStatusCode(304);
			System.out.println("\t[HttpFileHandler] resource not modified.");
			return;
		}
		
	    res.setEntity(new InputStreamEntity(file.openStream(), -1));
	}	
	
	private boolean isHeaderMatch(Header[] h, String tag) {
		if(h.length != 0) {
			for (Header header : h) {
				if(header.getValue().equals(tag)) {
					return true;
				}
			}			
		}
		
		return false;
	}
	
	private boolean isModified(ServerFile file, HttpRequest req,
			HttpResponse res) {
		try {
			String lastModified = file.lastModified().toString();
			
			if(isHeaderMatch(req.getHeaders("IF-MODIFIED-SINCE"), lastModified)) {
				return false;
			} else {
				res.addHeader("Last-Modified", lastModified);
				return true;
			}
		} catch (UnsupportedOperationException lastModifiedException) {
			try {
				String ETag = file.getTag();
				
				if(isHeaderMatch(req.getHeaders("IF-NONE-MATCH"), ETag)) {
					return false;
				} else {
					res.addHeader("ETag", ETag);
					return true;
				}	
			} catch (UnsupportedOperationException tagException) {
				// nothing to do.
			} catch (FileNotFoundException fileException) {
				// nothing to do.
			}
		} catch (FileNotFoundException fileException) {
			// nothing to do.
		}
		
		return true;
	}
	
	private void addGeneralHeaders(HttpResponse res) {
		res.addHeader("Server", "e-WolfNode");
		res.addHeader("Date",Calendar.getInstance().getTime().toString());
	}
	
	private boolean loadFile(ServerFile file, String path)
			throws FileNotFoundException {
		try {
			file.read(path.substring(prefix.length()));
			return true;
		} catch (FileNotFoundException e) {
			file.read("404.html");
			return false;
		}
	}
}
