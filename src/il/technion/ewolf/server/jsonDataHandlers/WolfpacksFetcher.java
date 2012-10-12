package il.technion.ewolf.server.jsonDataHandlers;


import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_NOT_FOUND;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.server.cache.ICache;
import il.technion.ewolf.server.cache.ICacheWithParameter;
import il.technion.ewolf.socialfs.Profile;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class WolfpacksFetcher implements IJsonDataHandler {

	private final ICache<List<WolfPack>> wolfpacksCache;
	private final ICacheWithParameter<Profile, String> profilesCache;

	@Inject
	public WolfpacksFetcher(
			ICache<List<WolfPack>> wolfpacksCache,
			ICacheWithParameter<Profile, String> profilesCache) {
		this.wolfpacksCache = wolfpacksCache;
		this.profilesCache = profilesCache;
	}

	private static class JsonReqWolfpacksParams {
		//		If userID field wasn't sent with the request then the response
		//			list of wolfpack names will be for "logged in" user
		String userID;
	}

	static class WolfpacksResponse extends EWolfResponse {
		List<String> wolfpacksList;

		public WolfpacksResponse(List<String> lst) {
			this.wolfpacksList = lst;
		}

		public WolfpacksResponse(String result) {
			super(result);
		}
	}
	/**
	 * @param	jsonReq	serialized object of JsonReqWolfpacksParams class
	 * @return	list of all social groups' (wolfpacks') names, the user has access to them
	 */
	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqWolfpacksParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqWolfpacksParams.class);
		} catch (Exception e) {
			return new WolfpacksResponse(RES_BAD_REQUEST);
		}

		List<WolfPack> wgroups = wolfpacksCache.get();
		List<String> groups = new ArrayList<String>();

		if (jsonReqParams.userID==null) {
			for (WolfPack w : wgroups) {
				groups.add(w.getName());
			}
		} else {
			Profile profile = profilesCache.get(jsonReqParams.userID);
			if (profile == null) {
				return new WolfpacksResponse(RES_NOT_FOUND);
			}

			for (WolfPack w : wgroups) {
				if (w.getMembers().contains(profile)) {
					groups.add(w.getName());
				}
			}
		}
		return new WolfpacksResponse(groups);
	}
}
