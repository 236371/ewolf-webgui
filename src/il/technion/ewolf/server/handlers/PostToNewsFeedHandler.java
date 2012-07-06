package il.technion.ewolf.server.handlers;

import il.technion.ewolf.ewolf.SocialNetwork;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.FileNotFoundException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class PostToNewsFeedHandler implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	private final SocialNetwork snet;
	private final TextPost textPost;

	@Inject
	public PostToNewsFeedHandler(SocialNetwork snet, WolfPackLeader socialGroupsManager,
			TextPost textPost) {
		this.snet = snet;
		this.socialGroupsManager = socialGroupsManager;
		this.textPost = textPost;
	}
	
	private class JsonReqPostToNewsFeedParams {
		String wolfpackName;
		//post text
		String post;
	}

	//response error messages
	private static final String INTERNAL_ERROR_MESSAGE = "internal error";
	private static final String WOLFPACK_NOT_FOUND_MESSAGE = "wolfpack not found";

	/**
	 * @param	jsonReq	serialized object of JsonReqCreateWolfpackParams class
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqPostToNewsFeedParams jsonReqParams =
				gson.fromJson(jsonReq, JsonReqPostToNewsFeedParams.class);
		if (jsonReqParams.wolfpackName == null || jsonReqParams.post == null) {
			return new EWolfResponse("Must specify both wolfpack name and post text");
		}

		WolfPack socialGroup = socialGroupsManager.findSocialGroup(jsonReqParams.wolfpackName);
		if (socialGroup == null) {
			return new EWolfResponse(WOLFPACK_NOT_FOUND_MESSAGE);
		}

		try {
			snet.getWall().publish(textPost.setText(jsonReqParams.post), socialGroup);
		} catch (GroupNotFoundException e) {
			System.out.println("Wolfpack " + socialGroup + " not found");
			e.printStackTrace();
			return new EWolfResponse(WOLFPACK_NOT_FOUND_MESSAGE);
		} catch (WallNotFound e) {
			System.out.println("Wall not found");
			e.printStackTrace();
			return new EWolfResponse(INTERNAL_ERROR_MESSAGE);
		} catch (FileNotFoundException e) {
			System.out.println("File /wall/posts/ not found");
			e.printStackTrace();
			return new EWolfResponse(INTERNAL_ERROR_MESSAGE);
		}
		return new EWolfResponse();
	}

}
