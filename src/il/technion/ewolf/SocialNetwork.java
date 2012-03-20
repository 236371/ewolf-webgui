package il.technion.ewolf;

import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.exception.CredentialsNotFoundException;

public interface SocialNetwork {

	public void login(String password) throws CredentialsNotFoundException;
	
	public Wall getWall() throws WallNotFound;
	public Wall getWall(Profile profile) throws WallNotFound;
	
}
