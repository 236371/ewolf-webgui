package il.technion.ewolf.server.fetchers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;

import il.technion.ewolf.SocialNetwork;
import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

public class NewsFeedFetcher implements JsonDataFetcher {
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
	
	class PostData implements Comparable<PostData>{
		String postID;
		String senderID;
		String senderName;
		Long timestamp;
		String text;
		
		PostData(String postID, String senderID, String senderName, Long timestamp, String text) {
			this.postID = postID;
			this.senderID = senderID;
			this.senderName = senderName;
			this.timestamp = timestamp;
			this.text = text;
		}

		@Override
		public int compareTo(PostData o) {
			return -Long.signum(this.timestamp - o.timestamp); //"-" for ordering from newer posts to older
		}
	}

	//FIXME wolfpack name can be "all"!!!
	/**
	 * @param	parameters	The method gets exactly 5 parameters for filtering news-feed.
	 * 			[0]:		Request type: "userID" or "wolfpack".
	 * 			[1]:		For request type "userID": user ID or "my" 
	 * 							(to retrieve posts from a specific user), 
	 * 						for request type "wolfpack": wolfpack name or "all" 
	 * 							(to retrieve posts from specific wolfpack or from all wolfpacks).
	 * 			[2]:		The amount of posts to retrieve.
	 * 			[3]:		Time in milliseconds since 1970, to retrieve posts older than this date.
	 * 			[4]:		Time in milliseconds since 1970, to retrieve posts newer than this date.  
	 * @return	list of posts, each contains post ID, sender ID, sender name, timestamp and post text
	 */
	@Override
	public Object fetchData(String... parameters) throws ProfileNotFoundException, FileNotFoundException, WallNotFound {
		if(parameters.length != 5) {
			return null;
		}

		Integer filterNumOfPosts = (parameters[2].equals("null"))?null:Integer.valueOf(parameters[0]);
		Long filterToDate = (parameters[3].equals("null"))?null:Long.valueOf(parameters[1]);
		Long filterFromDate = (parameters[4].equals("null"))?null:Long.valueOf(parameters[2]);
		String requestInfo = parameters[1];		
		String requestType = parameters[0];
		
		List<Post> posts;
		if (requestType.equals("userID")) {
			posts = fetchPostsForUser(requestInfo);
		} else if (requestType.equals("wolfpack")) {
			posts = fetchPostsForWolfpack(requestInfo);
		} else {
			throw new IllegalArgumentException(NewsFeedFetcher.class.getCanonicalName() + 
					": request type should be either \"userID\" or \"wolfpack\"");
		}
		return filterPosts(posts, filterNumOfPosts, filterFromDate, filterToDate);
	}

	private Object filterPosts(List<Post> posts, Integer filterNumOfPosts,
			Long filterFromDate, Long filterToDate) throws ProfileNotFoundException {
		List<PostData> lst = new ArrayList<PostData>();
		for (Post post: posts) {
			Profile postOwner = post.getOwner();
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
		
		if (filterNumOfPosts==null) {
			return lst;
		} else {
			return (filterNumOfPosts<lst.size())?getFirstNElements(filterNumOfPosts, lst):lst;
		}
	}
	
	private <T> List<T> getFirstNElements(int n, List<T> list) {
		List<T> newList = new ArrayList<T>();
		for (int i=0; i<n; i++) {
			newList.add(list.get(i));
		}
		return newList;		
	}

	private List<Post> fetchPostsForWolfpack(String socialGroupName) throws FileNotFoundException, WallNotFound, ProfileNotFoundException {
		List<WolfPack> wolfpacks = new ArrayList<WolfPack>();
		if (socialGroupName.equals("all")) {
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

	private List<Post> fetchPostsForUser(String strUid) throws ProfileNotFoundException, FileNotFoundException, WallNotFound {
		Profile profile;
		if (strUid.equals("my")) {
			profile = socialFS.getCredentials().getProfile();
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			profile = socialFS.findProfile(uid);			
		}

		Set<Profile> profiles = new HashSet<Profile>();
		profiles.add(profile);
		return fetchPostsForProfiles(profiles);
	}

	private List<Post> fetchPostsForProfiles(Set<Profile> profiles) throws FileNotFoundException, WallNotFound, ProfileNotFoundException {
		List<Post> posts = new ArrayList<Post>();
		for (Profile profile: profiles) {
			posts.addAll(snet.getWall(profile).getAllPosts());
		}		
		return posts;
	}
}
//TODO set instead of list for posts