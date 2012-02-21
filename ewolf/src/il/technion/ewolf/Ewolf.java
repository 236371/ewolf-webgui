package il.technion.ewolf;

import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.exception.CredentialsNotFoundException;

import java.io.FileNotFoundException;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class Ewolf implements SocialNetwork {

	private final SocialFS socialFS;
	private final Provider<Wall> wallProvider;
	
	@Inject
	Ewolf(SocialFS socialFS,
			Provider<Wall> wallProvider) {
		
		this.socialFS = socialFS;
		this.wallProvider = wallProvider;
	}
	
	public void login(String password) throws CredentialsNotFoundException {
		socialFS.login(password);
	}

	@Override
	public Wall getWall() throws WallNotFound {
		return getWall(socialFS.getCredentials().getProfile());
	}

	@Override
	public Wall getWall(Profile profile) throws WallNotFound {
		try {
			return wallProvider.get()
				.setRootFolder(profile.getRootFile())
				.setProfile(profile);
		} catch (FileNotFoundException e) {
			throw new WallNotFound(e);
		}
	}



}
