package il.technion.ewolf.server.jsonDataHandlers;

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
		String userID;
		//message text
		String message;
	}

	static class SendMessageResponse extends EWolfResponse {
		public SendMessageResponse(String result) {
			super(result);
		}

		public SendMessageResponse(String result, String errorMessage) {
			super(result, errorMessage);
		}

		public SendMessageResponse(){
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

		if (jsonReqParams.message == null || jsonReqParams.userID == null) {
			return new SendMessageResponse(RES_BAD_REQUEST,
					"Must specify both user ID and message body.");
		}

		Profile profile;
		try {
			UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
			profile = socialFS.findProfile(uid);
		} catch (ProfileNotFoundException e) {
			e.printStackTrace();
			return new SendMessageResponse(RES_NOT_FOUND, "User with given ID wasn't found.");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return new SendMessageResponse(RES_BAD_REQUEST, "Illegal user ID.");
		}

		ContentMessage msg = smail.createContentMessage().setMessage(jsonReqParams.message);
		smail.send(msg, profile);
		return new SendMessageResponse();
	}

}
