package il.technion.ewolf;

import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.SocialFSCreator;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class EwolfAccountCreator {

	
	private final SocialFSCreator socialFSCreator;
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	
	@Inject @Named("ewolf.fs.social_groups.name") String socialGroupsFolderName;
	
	// state
	private SFSFile rootFolder;
	
	@Inject
	EwolfAccountCreator(
			SocialFSCreator socialFSCreator,
			SocialFS socialFS,
			WolfPackLeader socialGroupsManager) {
		
		this.socialFSCreator = socialFSCreator;
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
	}
	
	
	private void createWall() throws Exception {
		WolfPack wallReadersSocialGroup = socialGroupsManager
			.createSocialGroup("wall-readers");
		
		SFSFile wallFolder = socialFS.getSFSFileFactory()
			.createNewFolder()
			.setName("wall");
			
		rootFolder.append(wallFolder, wallReadersSocialGroup.getGroup());
		
		SFSFile wallPostsFolder = socialFS.getSFSFileFactory()
				.createNewFolder()
				.setName("posts");

		wallFolder.append(wallPostsFolder, wallReadersSocialGroup.getGroup());
	}
	
	public void create() throws Exception {
		socialFSCreator.create();
		
		socialFS.login(socialFSCreator.getPassword());
		
		Profile myProfile = socialFS.getCredentials().getProfile();
		rootFolder = myProfile.getRootFile();
		
		SFSFile socialGroupsFolder = socialFS.getSFSFileFactory()
			.createNewFolder()
			.setName(socialGroupsFolderName);
		
		rootFolder.append(socialGroupsFolder, socialFS.getStash().createGroup());
		
		createWall();
		
		System.out.println(socialFS);
	}
	
	
}
