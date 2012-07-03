package il.technion.ewolf.server.handlers;


import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.server.exceptions.NotFound;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class WolfpacksFetcher implements JsonDataHandler {
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	private final UserIDFactory userIDFactory;

	@Inject
	public WolfpacksFetcher(SocialFS socialFS, WolfPackLeader socialGroupsManager, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
		this.userIDFactory = userIDFactory;
	}

	private class JsonReqWolfpacksParams {
//		If userID field wasn't sent with the request then
//			the response list will be for "logged in" user
		String userID;
	}
	/**
	 * @param	jsonReq	serialized object of JsonReqWolfpacksParams class
	 * @return	list of all social groups (wolfpacks) names, the user has access to them
	 * @throws NotFound 
	 */
	@Override
	public Object handleData(JsonElement jsonReq) throws NotFound {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqWolfpacksParams jsonReqParams = gson.fromJson(jsonReq, JsonReqWolfpacksParams.class);
		
		List<WolfPack> wgroups = socialGroupsManager.getAllSocialGroups();
		List<String> groups = new ArrayList<String>();
		
		if (jsonReqParams.userID==null) {
			for (WolfPack w : wgroups) {
				groups.add(w.getName());
			}
		} else {
			UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
			try {
				Profile profile = socialFS.findProfile(uid);
				for (WolfPack w : wgroups) {
					if (w.getMembers().contains(profile)) {
						groups.add(w.getName());
					}
				}
			} catch (ProfileNotFoundException e) {
				throw new NotFound(e);
			}
		}
		return groups;
	}
}
