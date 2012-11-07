package il.technion.ewolf.server.sfsHandlers;

import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.FileNotFoundException;
import java.io.Serializable;

import com.google.inject.Inject;

public class DownloadFileFromSFS {
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;

	@Inject
	public DownloadFileFromSFS(SocialFS socialFS, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}

	public Serializable handleData(String userID, String fileName)
			throws ProfileNotFoundException, FileNotFoundException {
		Profile profile;
		Serializable fileData;
		UserID uid = userIDFactory.getFromBase64(userID);
		profile = socialFS.findProfile(uid);
		SFSFile sharedFolder = profile.getRootFile().getSubFile("sharedFolder");
		fileData = sharedFolder.getSubFile(fileName).getData();
		return fileData;
	}

}
