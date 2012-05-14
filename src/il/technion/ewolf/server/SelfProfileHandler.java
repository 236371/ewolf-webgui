package il.technion.ewolf.server;

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
import com.google.inject.Inject;

public class SelfProfileHandler implements HttpRequestHandler{
	@SuppressWarnings("unused")
	private String name;
	@SuppressWarnings("unused")
	private String id;
	private List<String> groups = new ArrayList<String>();
	
	@Inject
	public SelfProfileHandler(SocialFS socialFS, WolfPackLeader socialGroupsManager) {
		
		Profile profile = socialFS.getCredentials().getProfile();
		name = profile.getName();
		id = profile.getUserId().toString();
		
		for (WolfPack w : socialGroupsManager.getAllSocialGroups()) {
			groups.add(w.toString());
		}
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		res.addHeader("Server", "e-WolfNode");
		res.addHeader("Content-Type", "application/json");
		
		Gson gson = new Gson();
		String json = gson.toJson(this);
		
		res.setEntity(new StringEntity(json));				
	}

}
