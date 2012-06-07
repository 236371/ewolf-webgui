package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.HttpStringExtractor;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.File;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

public class ViewProfileHandler implements HttpRequestHandler {
	private static final String HANDLER_REGISTER_PATTERN = "/viewProfile/*";
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;
	
	private class JsonProfile {
		@SuppressWarnings("unused")
		private String name;
		@SuppressWarnings("unused")
		private String id;
	
		private JsonProfile(String name, String id) {
			this.name = name;
			this.id = id;
		}
	}
	
	@Inject
	public ViewProfileHandler(SocialFS socialFS, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}

	//XXX req of type GET with "/viewProfile/{UserID}" or "/viewProfile/my" URI
	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		String strUid = HttpStringExtractor.fromURIAfterLastSlash(req);
		
		Profile profile;
		if (strUid.equals("my")) {
			profile = socialFS.getCredentials().getProfile();
			strUid = profile.getUserId().toString();
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			try {
				profile = socialFS.findProfile(uid);			
			} catch (ProfileNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("Profile with UserID" + uid + "not found");
				res.setStatusCode(HttpStatus.SC_NOT_FOUND);
				res.setHeader(HTTP.CONTENT_TYPE, "text/html");
				res.setEntity(new FileEntity(new File("404.html"),"text/html"));
				return;
			}
		}

		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		JsonProfile jsonObj = new JsonProfile(profile.getName(), strUid);
		String json = gson.toJson(jsonObj);
		res.setEntity(new StringEntity(json));
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}
}