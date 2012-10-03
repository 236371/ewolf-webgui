package il.technion.ewolf.server.jsonDataHandlers;

import java.util.ArrayList;
import java.util.List;

import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import static il.technion.ewolf.server.EWolfResponse.*;

public class SendMessageHandler implements JsonDataHandler {
	private final SocialMail smail;
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;

	@Inject
	public SendMessageHandler(SocialMail smail, SocialFS socialFS, UserIDFactory userIDFactory) {
		this.smail = smail;
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
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
			Profile profile;
			try {
				UserID uid = userIDFactory.getFromBase64(userID);
				profile = socialFS.findProfile(uid);
			} catch (ProfileNotFoundException e) {
				e.printStackTrace();
				userIDsResult.add(new EWolfResponse(RES_NOT_FOUND,
						"User with given ID wasn't found."));
				continue;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				userIDsResult.add(new EWolfResponse(RES_BAD_REQUEST, "Illegal user ID."));
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
