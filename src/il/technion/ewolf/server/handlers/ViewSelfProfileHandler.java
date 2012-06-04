package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

public class ViewSelfProfileHandler implements HttpRequestHandler{
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;

	private class JsonProfile {
		@SuppressWarnings("unused")
		private String name;
		@SuppressWarnings("unused")
		private String id;
		private List<String> groups = new ArrayList<String>();

		private JsonProfile(String name, String id) {
			this.name = name;
			this.id = id;
		}
		void addGroup(String groupName) {
			groups.add(groupName);
		}
	}
	
	@Inject
	public ViewSelfProfileHandler(SocialFS socialFS, WolfPackLeader socialGroupsManager) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding headers to response intercepter
		res.addHeader("Server", "e-WolfNode");
		res.addHeader("Content-Type", "application/json");

		Profile profile = socialFS.getCredentials().getProfile();
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		JsonProfile jsonObj = new JsonProfile(profile.getName(), profile.getUserId().toString());
		for (WolfPack w : socialGroupsManager.getAllSocialGroups()) {
			jsonObj.addGroup(w.getName());
		}
		
		String json = gson.toJson(jsonObj);
		res.setEntity(new StringEntity(json));				
	}

}
