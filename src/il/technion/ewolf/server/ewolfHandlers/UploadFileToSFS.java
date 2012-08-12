package il.technion.ewolf.server.ewolfHandlers;

import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_INTERNAL_SERVER_ERROR;
import static il.technion.ewolf.server.EWolfResponse.RES_SUCCESS;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.Group;

import java.io.FileNotFoundException;
import java.net.URLEncoder;

import org.apache.commons.lang.RandomStringUtils;

import com.google.inject.Inject;

public class UploadFileToSFS {
	class UploadFileResponse extends EWolfResponse {
		String path;
		public UploadFileResponse(String result, String path) {
			super(result);
			this.path = path;
		}

		public UploadFileResponse(String result, String errorMessage, String path) {
			super(result, errorMessage);
			this.path = path;
		}
	}
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;

	private static final int FILENAME_LENGTH = 10;

	@Inject
	public UploadFileToSFS(SocialFS socialFS, WolfPackLeader socialGroupsManager) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
	}
	
	public UploadFileResponse handleData(String wolfpackName, String ext, byte[] fileData) {
		Profile profile = socialFS.getCredentials().getProfile();
		String resFileName;
		try {
			WolfPack sharedSocialGroup = socialGroupsManager.findSocialGroup(wolfpackName);
			if (sharedSocialGroup == null) {
				return new UploadFileResponse(RES_BAD_REQUEST, "Given wolfpack wasn't found.", null);
			}
			Group group = sharedSocialGroup.getGroup();

			SFSFile sharedFolder = profile.getRootFile().getSubFile("sharedFolder");

			//create unique file name
			while (true) {
				resFileName = RandomStringUtils.randomAlphanumeric(FILENAME_LENGTH) + "." + ext;
				try {
					sharedFolder.getSubFile(resFileName);
				} catch (FileNotFoundException e) {
					break;
				}
			}
			
			SFSFile file = socialFS.getSFSFileFactory().createNewFile()
					.setName(resFileName)
					.setData(fileData);

			sharedFolder.append(file, group);

			String encodedID = URLEncoder.encode(profile.getUserId().toString(), "UTF-8");
			String path = "/sfs?userID=" + encodedID + "&fileName=" + resFileName;
			return new UploadFileResponse(RES_SUCCESS, path);
		} catch (Exception e) {
			e.printStackTrace();
			return new UploadFileResponse(RES_INTERNAL_SERVER_ERROR, null);
		}
	}

}
