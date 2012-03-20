package il.technion.ewolf;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.Group;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class WolfPack {

	
	@Inject SocialFS socialFS;
	@Inject @Named("ewolf.fs.social_groups") Provider<SFSFile> socialGroupsFolderProvider;
	@Inject SocialMail socialMail;
	
	private SFSFile file;
	
	
	@Inject
	WolfPack() {
		
	}
	
	WolfPack setFile(SFSFile file) {
		this.file = file;
		return this;
	}
	
	
	WolfPack create(String name, Group group) {
		if (name == null)
			throw new IllegalStateException("missing name");
		if (file != null)
			throw new IllegalStateException("already exists");
		
		file = socialFS.getSFSFileFactory().createNewFolder()
			.setName(name);
		
		socialGroupsFolderProvider.get()
			.append(file, group);
		
		return this;
	}
	
	
	public List<Profile> getMembers() {
		
		List<SFSFile> membersFiles = file.list();
		
		if (membersFiles.isEmpty())
			return Collections.emptyList();
		
		List<Profile> $ = new ArrayList<Profile>();
		for (SFSFile f : membersFiles) {
			$.add((Profile) f.getData());
		}
		
		return $;
	}
	
	public WolfPack addMember(Profile p) throws GroupNotFoundException {
		SFSFile memberFile = socialFS.getSFSFileFactory().createNewFolder()
			.setName(p.getUserId().getKey().toBase64())
			.setData(p);
		
		file.append(memberFile, getGroup());
		
		pokeMember(p);
		
		return this;
	}
	
	public void pokeMember(Profile member) throws GroupNotFoundException {
		PokeMessage msg = socialMail.createPokeMessage()
			.addGroup(getGroup());
		
		socialMail.send(msg, member);
	}

	public Group getGroup() throws GroupNotFoundException {
		Key groupId = file.getGroupId();
		System.out.println("groupId: "+groupId);
		return socialFS.getStash().getGroupFromId(groupId);
	}

	public String getName() {
		return file.getName();
	}
	
	@Override
	public String toString() {
		return "SocialGroup "+getName()+" members: "+getMembers();
	}

}
