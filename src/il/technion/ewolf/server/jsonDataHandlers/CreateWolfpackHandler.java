package il.technion.ewolf.server.jsonDataHandlers;

import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_GENERIC_ERROR;
import static il.technion.ewolf.server.EWolfResponse.RES_INTERNAL_SERVER_ERROR;
import static il.technion.ewolf.server.EWolfResponse.RES_SUCCESS;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.server.cache.ICache;
import il.technion.ewolf.socialfs.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class CreateWolfpackHandler implements IJsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	private final ICache<Map<String, WolfPack>> wolfpacksCache;
	private final ICache<Map<String,List<Profile>>> wolfpacksMembersCache;

	@Inject
	public CreateWolfpackHandler(WolfPackLeader socialGroupsManager,
			ICache<Map<String, WolfPack>> wolfpacksCache,
			ICache<Map<String,List<Profile>>> wolfpacksMembersCache) {
		this.socialGroupsManager = socialGroupsManager;
		this.wolfpacksCache = wolfpacksCache;
		this.wolfpacksMembersCache = wolfpacksMembersCache;
	}

	private static class JsonReqCreateWolfpackParams {
		List<String> wolfpackNames;
	}

	static class CreateWolfpackResponse extends EWolfResponse {
		List<EWolfResponse> wolfpacksResult;
		public CreateWolfpackResponse(String result) {
			super(result);
		}

		public CreateWolfpackResponse(String result, String errorMessage) {
			super(result, errorMessage);
		}

		public CreateWolfpackResponse(String result, List<EWolfResponse> wolfpacksResult) {
			super(result);
			this.wolfpacksResult = wolfpacksResult;
		}
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqCreateWolfpackParams class
	 * @return	"success" or error message
	 */
	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqCreateWolfpackParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqCreateWolfpackParams.class);
		} catch (Exception e) {
			return new CreateWolfpackResponse(RES_BAD_REQUEST);
		}

		if (jsonReqParams.wolfpackNames == null || jsonReqParams.wolfpackNames.isEmpty()) {
			return new CreateWolfpackResponse(RES_BAD_REQUEST, "Must specify wolfpack name/s.");
		}

		List<EWolfResponse> wolfpacksResult = new ArrayList<EWolfResponse>();
		Map<String, WolfPack> wolfpacksMap = wolfpacksCache.get();

		for (String wolfpackName : jsonReqParams.wolfpackNames) {
			if (wolfpacksMap.containsKey(wolfpackName)) {
				wolfpacksResult.add(new EWolfResponse(RES_BAD_REQUEST, "Wolfpack already exists"));
				continue;
			}
			try {
				socialGroupsManager.findOrCreateSocialGroup(wolfpackName);
			} catch (Exception e) {
				wolfpacksResult.add(new EWolfResponse(RES_INTERNAL_SERVER_ERROR));
				continue;
			}
			wolfpacksResult.add(new EWolfResponse());
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				//updates also wolfpacksCache
				wolfpacksMembersCache.update();
			}
		}).start();

		for (EWolfResponse res : wolfpacksResult) {
			if (res.getResult() != RES_SUCCESS) {
				return new CreateWolfpackResponse(RES_GENERIC_ERROR, wolfpacksResult);
			}
		}
		return new CreateWolfpackResponse(RES_SUCCESS);
	}

}
