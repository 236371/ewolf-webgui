package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.server.HttpStringExtractor;
import il.technion.ewolf.socialfs.Profile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

public class ViewSocialGroupMembersHandler implements HttpRequestHandler {
	private static final String HANDLER_REGISTER_PATTERN = "/viewSocialGroupMembers/*";
	private final WolfPackLeader socialGroupsManager;
	
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
	public ViewSocialGroupMembersHandler(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}

	//XXX req of type GET with "/viewSocialGroupMembers/{GroupName}" URI
	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		String socialGroupName = HttpStringExtractor.fromURIAfterLastSlash(req);
		List<Profile> profiles = socialGroupsManager.findSocialGroup(socialGroupName).getMembers();
		
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		List<ViewSocialGroupMembersHandler.JsonProfile> lst = new ArrayList<ViewSocialGroupMembersHandler.JsonProfile>();
		for (Profile profile: profiles) {
			lst.add(new JsonProfile(profile.getName(), profile.getUserId().toString()));
		}
		String json = gson.toJson(lst, lst.getClass());
		res.setEntity(new StringEntity(json));
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}
}
