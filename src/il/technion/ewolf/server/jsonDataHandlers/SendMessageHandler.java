package il.technion.ewolf.server.jsonDataHandlers;

import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_GENERIC_ERROR;
import static il.technion.ewolf.server.EWolfResponse.RES_NOT_FOUND;
import static il.technion.ewolf.server.EWolfResponse.RES_SUCCESS;
import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.server.cache.ICacheWithParameter;
import il.technion.ewolf.socialfs.Profile;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class SendMessageHandler implements IJsonDataHandler {
	private final SocialMail smail;

	private final ICacheWithParameter<Profile, String> profilesCache;

	@Inject
	public SendMessageHandler(SocialMail smail,
			ICacheWithParameter<Profile, String> profilesCache) {
		this.smail = smail;
		this.profilesCache = profilesCache;
	}

	private static class JsonReqSendMessageParams {
		List<String> userIDs;
		//message text
		String message;
	}

	static class SendMessageResponse extends EWolfResponse {
		List<EWolfResponse> userIDsResult;
		public SendMessageResponse(String result) {
			super(result);
		}

		public SendMessageResponse(String result, String errorMessage) {
			super(result, errorMessage);
		}

		public SendMessageResponse(){
		}

		public SendMessageResponse(String result, List<EWolfResponse> userIDsResult) {
			super(result);
			this.userIDsResult = userIDsResult;
		}
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqSendMessageParams class
	 * @return	"success" or error message
	 */
	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqSendMessageParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqSendMessageParams.class);
		} catch (Exception e) {
			return new SendMessageResponse(RES_BAD_REQUEST);
		}

		if (jsonReqParams.message == null || jsonReqParams.userIDs == null
				|| jsonReqParams.userIDs.isEmpty()) {
			return new SendMessageResponse(RES_BAD_REQUEST,
					"Must specify both user IDs and message body.");
		}

		List<EWolfResponse> userIDsResult = new ArrayList<EWolfResponse>();

		for (String userID : jsonReqParams.userIDs) {
			if (userID == null) continue;

			Profile profile = profilesCache.get(userID);
			if (profile == null) {
				userIDsResult.add(new EWolfResponse(RES_NOT_FOUND,
						"User with given ID wasn't found."));
				continue;
			}

			ContentMessage msg = smail.createContentMessage().setMessage(jsonReqParams.message);
			smail.send(msg, profile);
			userIDsResult.add(new EWolfResponse());
		}

		for (EWolfResponse res : userIDsResult) {
			if (res.getResult() != RES_SUCCESS) {
				return new SendMessageResponse(RES_GENERIC_ERROR, userIDsResult);
			}
		}
		return new SendMessageResponse();
	}

}
