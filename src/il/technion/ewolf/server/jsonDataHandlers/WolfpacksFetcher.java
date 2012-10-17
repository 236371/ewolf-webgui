package il.technion.ewolf.server.jsonDataHandlers;


import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_NOT_FOUND;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.server.cache.ICache;
import il.technion.ewolf.server.cache.ICacheWithParameter;
import il.technion.ewolf.socialfs.Profile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class WolfpacksFetcher implements IJsonDataHandler {

	private final ICache<Map<String, WolfPack>> wolfpacksCache;
	private final ICacheWithParameter<Profile, String> profilesCache;
	private final ICache<Map<String,List<Profile>>> wolfpacksMembersCache;

	@Inject
	public WolfpacksFetcher(
			ICache<Map<String, WolfPack>> wolfpacksCache,
			ICacheWithParameter<Profile, String> profilesCache,
			ICache<Map<String,List<Profile>>> wolfpacksMembersCache) {
		this.wolfpacksCache = wolfpacksCache;
		this.profilesCache = profilesCache;
		this.wolfpacksMembersCache = wolfpacksMembersCache;
	}

	private static class JsonReqWolfpacksParams {
		//		If userID field wasn't sent with the request then the response
		//			list of wolfpack names will be for "logged in" user
		String userID;
	}

	static class WolfpacksResponse extends EWolfResponse {
		Set<String> wolfpacksList;

		public WolfpacksResponse(Set<String> set) {
			this.wolfpacksList = set;
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

		Map<String, WolfPack> wolfpacksMap = wolfpacksCache.get();
		Map<String,List<Profile>> wolfpacksMembersMap = wolfpacksMembersCache.get();
		Set<String> groups = new HashSet<String>();

		if (jsonReqParams.userID == null) {
			groups.addAll(wolfpacksMap.keySet());
		} else {
			Profile profile = profilesCache.get(jsonReqParams.userID);
			if (profile == null) {
				return new WolfpacksResponse(RES_NOT_FOUND);
			}

			for (String w : wolfpacksMembersMap.keySet()) {
				List<Profile> wMembers = wolfpacksMembersMap.get(w);
				if (wMembers.contains(profile)) {
					groups.add(w);
				}
			}
		}
		return new WolfpacksResponse(groups);
	}
}
