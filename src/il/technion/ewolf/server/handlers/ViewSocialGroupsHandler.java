package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.server.HttpStringExtractor;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class ViewSocialGroupsHandler implements HttpRequestHandler {
	private static final String HANDLER_REGISTER_PATTERN = "/viewSocialGroups";
	private final WolfPackLeader socialGroupsManager;
	private final UserIDFactory userIDFactory;
	private final SocialFS socialFS;

	private class JsonSocialGroups {
		private final List<String> groups = new ArrayList<String>();

		void addGroup(String groupName) {
			groups.add(groupName);
		}
	}
	
	@Inject
	public ViewSocialGroupsHandler(WolfPackLeader socialGroupsManager, UserIDFactory userIDFactory, SocialFS socialFS) {
		this.socialGroupsManager = socialGroupsManager;
		this.userIDFactory = userIDFactory;
		this.socialFS = socialFS;
	}

	//XXX req of type GET with "/viewSocialGroups/{userID}" or "/viewSocialGroups/my" URI
	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		String strUid = HttpStringExtractor.fromURIAfterLastSlash(req);
		
		JsonSocialGroups jsonObj = new JsonSocialGroups();
		List<WolfPack> groups = socialGroupsManager.getAllSocialGroups();
		
		if (strUid.equals("my")) {
			for (WolfPack w : groups) {
				jsonObj.addGroup(w.getName());
			}
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			Profile profile;
			try {
				profile = socialFS.findProfile(uid);			
			} catch (ProfileNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("Profile with UserID" + uid + "not found");
				res.setStatusCode(HttpStatus.SC_NOT_FOUND);
				res.setEntity(new FileEntity(new File("404.html"),"text/html"));
				return;
			}
			for (WolfPack w : groups) {
				if (w.getMembers().contains(profile)) {
					jsonObj.addGroup(w.getName());
				}				
			}
		}
		
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		String json = gson.toJson(jsonObj);
		res.setEntity(new StringEntity(json));
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}

}
