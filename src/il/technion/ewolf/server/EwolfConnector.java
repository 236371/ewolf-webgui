package il.technion.ewolf.server;


import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import il.technion.ewolf.SocialNetwork;
import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

public class EwolfConnector {
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	private final UserIDFactory userIDFactory;
	private final SocialNetwork snet;
	private final SocialMail smail;
	private final TextPost textPost;
	
	@SuppressWarnings("unused")
	private class ProfileObj {
		private String name;
		private String id;
	
		private ProfileObj(String name, String id) {
			this.name = name;
			this.id = id;
		}
	}
	
	@Inject
	public EwolfConnector(SocialFS socialFS, WolfPackLeader socialGroupsManager, UserIDFactory userIDFactory,
			SocialNetwork snet, SocialMail smail, TextPost textPost) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
		this.userIDFactory = userIDFactory;
		this.snet = snet;
		this.smail = smail;
		this.textPost = textPost;
	}
	
	public Object getProfile(String strUid) throws ProfileNotFoundException {
		
		Profile profile;
		if (strUid.equals("my")) {
			profile = socialFS.getCredentials().getProfile();
			strUid = profile.getUserId().toString();
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			profile = socialFS.findProfile(uid);			
		}
		return new ProfileObj(profile.getName(), strUid);
	}
	
	public Object getSocialGroups(String strUid) throws ProfileNotFoundException {
		List<WolfPack> wgroups = socialGroupsManager.getAllSocialGroups();
		List<String> groups = new ArrayList<String>();
		
		if (strUid.equals("my")) {
			for (WolfPack w : wgroups) {
				groups.add(w.getName());
			}
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			Profile profile;
			profile = socialFS.findProfile(uid);
			for (WolfPack w : wgroups) {
				if (w.getMembers().contains(profile)) {
					groups.add(w.getName());
				}				
			}
		}
		return groups;
	}

	public Object getSocialGroupMembers(String socialGroupName) {
		List<Profile> profiles = socialGroupsManager.findSocialGroup(socialGroupName).getMembers();
		List<EwolfConnector.ProfileObj> lst = new ArrayList<EwolfConnector.ProfileObj>();
		for (Profile profile: profiles) {
			lst.add(new ProfileObj(profile.getName(), profile.getUserId().toString()));
		}
		return lst;
	}

	public Object getMessageBoard(String strUid) throws ProfileNotFoundException, FileNotFoundException, WallNotFound {
		@SuppressWarnings("unused")
		class PostObj {
			private String postID;
			private Long timestamp;
			private String text;
			
			private PostObj(String postID, Long timestamp, String text) {
				this.postID = postID;
				this.timestamp = timestamp;
				this.text = text;
			}
		}
		
		Profile profile;
		if (strUid.equals("my")) {
			profile = socialFS.getCredentials().getProfile();
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			profile = socialFS.findProfile(uid);			
		}
		
		List<Post> posts;
		posts = snet.getWall(profile).getAllPosts();
		List<PostObj> lst = new ArrayList<PostObj>();
		for (Post post: posts) {
			lst.add(new PostObj(post.getPostId().toString(), post.getTimestamp(), ((TextPost)post).getText()));
		}
		return lst;
	}

	public Object getInbox() {
		@SuppressWarnings("unused")
		class MessageObj implements Comparable<MessageObj>{
			private String sender;
			private Long timestamp;
			private String className;
			
			private MessageObj(String sender, Long timestamp, String className) {
				this.sender = sender;
				this.timestamp = timestamp;
				this.className = className;
			}

			@Override
			public int compareTo(MessageObj o) {
				return Long.signum(this.timestamp - o.timestamp);
			}
		}
		
		List<SocialMessage> messages = smail.readInbox();
		List<MessageObj> lst = new ArrayList<MessageObj>();
		for (SocialMessage m : messages) {
			try {
				//XXX also accepts PokeMessages
				Class<? extends SocialMessage> messageClass = m.getClass();
				if (messageClass == PokeMessage.class) {
					((PokeMessage)m).accept();
					continue;
				}
				lst.add(new MessageObj(m.getSender().getUserId().toString(), m.getTimestamp(),
						messageClass.getCanonicalName()));
			} catch (ProfileNotFoundException e) {
				System.out.println("Sender of social message" + m.toString() + "not found");
				e.printStackTrace();
				//TODO what to rethrow?
			}
		}
		//sort by timestamp
		Collections.sort(lst);
		return lst;
	}

	public void addSocialGroup(String groupName) {
		socialGroupsManager.findOrCreateSocialGroup(groupName);
	}
	
	public void addSocialGroupMember(String groupName, String strUid)
			throws ProfileNotFoundException, GroupNotFoundException {
		UserID uid = userIDFactory.getFromBase64(strUid);
		Profile profile = socialFS.findProfile(uid);
		WolfPack socialGroup = socialGroupsManager.findSocialGroup(groupName);
		if (socialGroup == null) {
			//TODO throw "group not found" exception("group name: " + groupName)
		}
		socialGroup.addMember(profile);
	}
	
	public void addMessageBoardPost(String groupName, String text)
			throws FileNotFoundException, GroupNotFoundException, WallNotFound {
		WolfPack socialGroup = socialGroupsManager.findSocialGroup(groupName);
		if (socialGroup == null) {
			//TODO throw "group not found" exception("group name: " + groupName)
		}
		snet.getWall().publish(textPost.setText(text), socialGroup);
	}
}
