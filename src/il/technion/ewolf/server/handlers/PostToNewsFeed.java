package il.technion.ewolf.server.handlers;

import il.technion.ewolf.SocialNetwork;
import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.FileNotFoundException;

import com.google.inject.Inject;

public class PostToNewsFeed implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	private final SocialNetwork snet;
	private final TextPost textPost;

	@Inject
	public PostToNewsFeed(SocialNetwork snet, WolfPackLeader socialGroupsManager,
			TextPost textPost) {
		this.snet = snet;
		this.socialGroupsManager = socialGroupsManager;
		this.textPost = textPost;
	}

	/**
	 * @param	parameters	The method gets exactly 2 parameters.
	 * 						[0]:		wolfpack name
	 * 						[1]:		post text
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(String... parameters)
			throws ProfileNotFoundException, FileNotFoundException,
			WallNotFound {
		if(parameters.length != 2) {
			return null;
		}
		
		WolfPack socialGroup = socialGroupsManager.findSocialGroup(parameters[0]);
		if (socialGroup == null) {
			return "wolfpack not found";
		}
		
		String text = parameters[1];
		try {
			snet.getWall().publish(textPost.setText(text), socialGroup);
		} catch (GroupNotFoundException e) {
			System.out.println("Wolfpack" + socialGroup + "not found");
			e.printStackTrace();
			return "wolfpack not found";
		} catch (WallNotFound e) {
			System.out.println("Wall not found");
			e.printStackTrace();
			return "wall not found";
		} catch (FileNotFoundException e) {
			System.out.println("File /wall/posts/ not found");
			e.printStackTrace();
			return "file system error";
		}
		return "success";
	}

}
