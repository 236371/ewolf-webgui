package il.technion.ewolf;

import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class WolfPackLeader {

	// dependencies
	private final SocialFS socialFS;
	private final Provider<WolfPack> socialGroupProvider;
	private final Provider<SFSFile> socialGroupsFolderProvider;
	
	// optimization
	private final Map<String, WolfPack> socialGroupsCache = new ConcurrentHashMap<String, WolfPack>();
	
	@Inject
	WolfPackLeader(SocialFS socialFS,
			Provider<WolfPack> socialGroupProvider,
			@Named("ewolf.fs.social_groups") Provider<SFSFile> socialGroupsFolderProvider) {
		
		this.socialFS = socialFS;
		this.socialGroupProvider = socialGroupProvider;
		this.socialGroupsFolderProvider = socialGroupsFolderProvider;
	}
	
	public WolfPack createSocialGroup(String name) {
		Group newGroup = socialFS.getStash().createGroup();
		WolfPack socialGroup = socialGroupProvider.get()
				.create(name, newGroup);
		
		return socialGroup;
	}
	
	public List<WolfPack> getAllSocialGroups() {
		List<WolfPack> $ = new ArrayList<WolfPack>();
		
		List<SFSFile> groupsFiles = socialGroupsFolderProvider.get().list();
		
		for (SFSFile f : groupsFiles) {
			WolfPack sg = socialGroupProvider.get()
					.setFile(f);
			$.add(sg);
			socialGroupsCache.put(sg.getName(), sg);
		}
		
		return $;
	}
	
	
	public WolfPack findSocialGroup(String name) {
		WolfPack $ = socialGroupsCache.get(name);
		if ($ != null)
			return $;
		
		List<SFSFile> groupsFiles = socialGroupsFolderProvider.get().list();
		
		for (SFSFile f : groupsFiles) {
			if (!name.equals(f.getName()))
				continue;
			
			$ = socialGroupProvider.get()
				.setFile(f);
			
			socialGroupsCache.put(name, $);
			
			return $;
		}
		return null;
	}
	
	public WolfPack findOrCreateSocialGroup(String name) {
		WolfPack $ = findSocialGroup(name);
		if ($ == null)
			$ = createSocialGroup(name);
		return $;
	}
}
