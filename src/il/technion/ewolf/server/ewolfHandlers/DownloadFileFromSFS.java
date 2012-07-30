package il.technion.ewolf.server.ewolfHandlers;

import java.io.FileNotFoundException;

import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import com.google.inject.Inject;

public class DownloadFileFromSFS {
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;

	@Inject
	public DownloadFileFromSFS(SocialFS socialFS, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}
	
	public String handleData(String userID, String fileName) {
		Profile profile;
		String strEntity;
		try {
			UserID uid = userIDFactory.getFromBase64(userID);
			profile = socialFS.findProfile(uid);
			SFSFile sharedFolder = profile.getRootFile().getSubFile("sharedFolder");
			strEntity = sharedFolder.getSubFile(fileName).getData().toString();
		} catch (ProfileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}  catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return strEntity;
	}

}
