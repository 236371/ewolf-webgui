package il.technion.ewolf.server.handlers;

import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.Group;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntityEnclosingRequest;

import com.google.inject.Inject;

public class SFSUploadHandler implements HttpRequestHandler {
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;

	@Inject
	public SFSUploadHandler(SocialFS socialFS, WolfPackLeader socialGroupsManager) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding server header to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		String uri = req.getRequestLine().getUri();

		Profile profile;
		String filename=null;
		String contentType;
		try {
			List<NameValuePair> parameters = 
					URLEncodedUtils.parse(new URI(uri).getQuery(),Consts.UTF_8);
			if (parameters.size() != 2) {
				//TODO how to handle errors? failed share file -> need report to user
				return;
			}
			for (NameValuePair v : parameters) {
				System.out.println(v.getName() + ": "+v.getValue());
				String name = v.getName();

				if (name.equals("filename")) {
					filename = v.getValue();
				} else if (name.equals("contentType")) {
					contentType = v.getValue();
				} else {
					//TODO how to handle errors? failed share file -> need report to user
					return;
				}
			}

			String fileData = EntityUtils.toString(((HttpEntityEnclosingRequest)req).getEntity());

			profile = socialFS.getCredentials().getProfile();
			SFSFile sharedFolder = profile.getRootFile().getSubFile("sharedFolder");

			SFSFile file = socialFS.getSFSFileFactory().createNewFile()
					.setName(filename)
					.setData(fileData);
			WolfPack sharedSocialGroup = socialGroupsManager.findSocialGroup("wall-readers");
			Group group = sharedSocialGroup.getGroup();
			sharedFolder.append(file, group);
			System.out.println(sharedFolder.getSubFile(filename).getData());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (GroupNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		String path = "/sfs/" + profile.getUserId().toString() + "/" + filename;
		res.setEntity(new StringEntity(path, ContentType.TEXT_PLAIN));
	}
}
