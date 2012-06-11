package il.technion.ewolf.msg;

import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.Group;

import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PokeMessage extends SocialMessage {

	private static final long serialVersionUID = -5734904136374212221L;

	private List<Group> groups = new ArrayList<Group>();
	
	
	@Inject
	PokeMessage(SocialFS socialFS,
			@Named("ewolf.groups.root") Group rootGroup) {
		super(socialFS);
		groups.add(rootGroup);
	}

	@Override
	protected void updateSignature(Signature sig) throws SignatureException {
		super.updateSignature(sig);
		for (Group group : groups) {
			sig.update(group.getGroupId().toBase64().getBytes());
			sig.update(group.getGroupSecretKey().getEncoded());
		}
	}
	
	
	public void accept() {
		for (Group group : groups) {
			//System.out.println("adding group "+group);
			socialFS.getStash().addGroup(group);
		}
	}
	
	public void ignore() {
		
	}
	

	public PokeMessage addGroup(Group group) {
		this.groups.add(group);
		return this;
	}

	@Override
	public String toString() {
		return "Poke: "+groups;
	}

	
}
