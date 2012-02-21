package il.technion.ewolf;

import il.technion.ewolf.posts.Post;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

public class Wall {

	// dependencies
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	
	// state
	private SFSFile rootFolder;
	private Profile profile = null;
	
	@Inject
	Wall(SocialFS socialFS,
			WolfPackLeader socialGroupsManager) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
	}
	
	Wall setRootFolder(SFSFile rootFolder) {
		this.rootFolder = rootFolder;
		return this;
	}
	
	Wall setProfile(Profile profile) {
		this.profile = profile;
		return this;
	}
	
	public WolfPack getSocialGroup() {
		return socialGroupsManager.findSocialGroup("wall-readers");
	}
	
	public List<Post> getAllPosts() throws FileNotFoundException {
		List<SFSFile> postsFiles = rootFolder.getSubFile("/wall/posts/").list();
		List<Post> posts = new ArrayList<Post>();
		for (SFSFile f : postsFiles) {
			posts.add(((Post)f.getData())
					.setTransientParams(socialFS));
		}
		return posts;
	}
	
	public void publish(Post p, WolfPack g) throws FileNotFoundException, GroupNotFoundException {
		if (profile.getPrvSigKey() == null)
			throw new IllegalStateException("cannot publish on other people's wall");
		
		SFSFile postFile = socialFS.getSFSFileFactory().createNewFolder()
			.setName(p.getPostId().toString())
			.setData(p);
		
		p.setOwner(profile);
		
		//p.setPrvSigKey(postFile.getPrvSigKey());
		//p.setPubSigKey(postFile.getPubSigKey());
		
		rootFolder.getSubFile("/wall/posts/")
			.append(postFile, g.getGroup());
	}
}
