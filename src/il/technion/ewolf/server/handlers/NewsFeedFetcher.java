package il.technion.ewolf.server.handlers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import il.technion.ewolf.SocialNetwork;
import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.server.exceptions.BadRequestException;
import il.technion.ewolf.server.exceptions.InternalEwolfErrorException;
import il.technion.ewolf.server.exceptions.NotFoundException;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

public class NewsFeedFetcher implements JsonDataHandler {
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	private final UserIDFactory userIDFactory;
	private final SocialNetwork snet;

	@Inject
	public NewsFeedFetcher(SocialFS socialFS, WolfPackLeader socialGroupsManager, UserIDFactory userIDFactory, SocialNetwork snet) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
		this.userIDFactory = userIDFactory;
		this.snet = snet;
	}

	private class JsonReqNewsFeedParams {
		//Request type: "user" or "wolfpack"
		String newsOf;
		/* The wolfpackName field will be ignored if "newsOf == user",
		 * otherwise
		 * 		if "wolfpackName" field is present then the response will contain
		 * 			all posts made by all the members of the given wolfpack,
		 * 		else ("wolfpackName" field isn't present) the response will contain
		 * 			all the posts readable for the "logged in" user (made by all
		 * 			members from all the “logged in” user wolfpacks).
		 */
		String wolfpackName;
		/* The userID field will be ignored if "newsOf == wolfpack",
		 * otherwise
		 * 		if "userID" field is present then the response will contain
		 * 			all the posts of the given user,
		 * 		else ("userID" field isn't present) the response will contain
		 * 			all the posts made by the “logged in” user.
		 */
		String userID;
		//The max amount of posts to retrieve.
		Integer maxMessages;
		//Time in milliseconds since 1970, to retrieve posts older than this date.
		Long olderThan;
		//Time in milliseconds since 1970, to retrieve posts newer than this date.
		Long newerThan;
	}

	class PostData implements Comparable<PostData>{
		String postID;
		String senderID;
		String senderName;
		Long timestamp;
		String post;
		
		PostData(String postID, String senderID, String senderName, Long timestamp, String post) {
			this.postID = postID;
			this.senderID = senderID;
			this.senderName = senderName;
			this.timestamp = timestamp;
			this.post = post;
		}

		@Override
		public int compareTo(PostData o) {
			return -Long.signum(this.timestamp - o.timestamp); //"-" for ordering from newer posts to older
		}
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqNewsFeedParams class  
	 * @return	list of posts, each contains post ID, sender ID, sender name, timestamp and post text
	 * @throws InternalEwolfErrorException 
	 * @throws NotFoundException 
	 * @throws BadRequestException 
	 */
	@Override
	public Object handleData(JsonElement jsonReq) throws InternalEwolfErrorException, NotFoundException, BadRequestException {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqNewsFeedParams jsonReqParams = gson.fromJson(jsonReq, JsonReqNewsFeedParams.class);
		
		List<Post> posts = null;
		if (jsonReqParams.newsOf == null) {
			throw new BadRequestException("Must specify whose news feed to fetch.");
		}
		if (jsonReqParams.newsOf.equals("user")) {
			posts = fetchPostsForUser(jsonReqParams.userID);
		} else if (jsonReqParams.newsOf.equals("wolfpack")) {
			posts = fetchPostsForWolfpack(jsonReqParams.wolfpackName);
		} else {
			//TODO throw Bad Request?
//			throw new IllegalArgumentException(NewsFeedFetcher.class.getCanonicalName() + 
//					": request type should be either \"userID\" or \"wolfpack\"");
		}

		return filterPosts(posts, jsonReqParams.maxMessages, jsonReqParams.newerThan,
				jsonReqParams.olderThan);
	}

	private Object filterPosts(List<Post> posts, Integer filterNumOfPosts,
			Long filterFromDate, Long filterToDate) {
		List<PostData> lst = new ArrayList<PostData>();
		for (Post post: posts) {
			Profile postOwner;
			try {
				postOwner = post.getOwner();
			} catch (ProfileNotFoundException e) {
				postOwner = null;
			}
			Long timestamp = post.getTimestamp();
			if (filterFromDate==null || filterFromDate<=timestamp) {
				if (filterToDate==null || filterToDate>=timestamp) {
					lst.add(new PostData(post.getPostId().toString(), postOwner.getUserId().toString(),
							postOwner.getName(), post.getTimestamp(), ((TextPost)post).getText()));
				}
			}
		}
		//sort by timestamp
		Collections.sort(lst);
		
		if (filterNumOfPosts != null && lst.size() > filterNumOfPosts) {
			lst = lst.subList(0, filterNumOfPosts);
		}
		return lst;
	}

	private List<Post> fetchPostsForWolfpack(String socialGroupName) throws InternalEwolfErrorException {
		List<WolfPack> wolfpacks = new ArrayList<WolfPack>();
		if (socialGroupName==null) {
			wolfpacks = socialGroupsManager.getAllSocialGroups();
		} else {
			wolfpacks.add(socialGroupsManager.findSocialGroup(socialGroupName));
		}
		
		Set<Profile> profiles = new HashSet<Profile>();
		for (WolfPack w : wolfpacks) {
			profiles.addAll(w.getMembers());
		}
		
		return fetchPostsForProfiles(profiles);
	}

	private List<Post> fetchPostsForUser(String strUid) throws InternalEwolfErrorException, NotFoundException {
		Profile profile;
		if (strUid==null) {
			profile = socialFS.getCredentials().getProfile();
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			try {
				profile = socialFS.findProfile(uid);
			} catch (ProfileNotFoundException e) {
				throw new NotFoundException(e);
			}			
		}

		Set<Profile> profiles = new HashSet<Profile>();
		profiles.add(profile);
		return fetchPostsForProfiles(profiles);
	}

	private List<Post> fetchPostsForProfiles(Set<Profile> profiles) throws InternalEwolfErrorException {
		List<Post> posts = new ArrayList<Post>();
		for (Profile profile: profiles) {
			try {
				posts.addAll(snet.getWall(profile).getAllPosts());
			} catch (FileNotFoundException e) {
				throw new InternalEwolfErrorException(e);
			} catch (WallNotFound e) {
				throw new InternalEwolfErrorException(e);
			}
		}		
		return posts;
	}
}
//TODO set instead of list for posts