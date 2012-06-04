package il.technion.ewolf.server.handlers;

import il.technion.ewolf.SocialNetwork;
import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.server.HttpStringExtractor;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class AddMessageBoardPostHandler implements HttpRequestHandler {
	private static final String HANDLER_REGISTER_PATTERN = "/addTextPost/*";
	private final SocialNetwork snet;
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	private final TextPost textPost;
	private final String port;
	private final String hostName;
	
	@Inject
	public AddMessageBoardPostHandler(SocialNetwork snet, WolfPackLeader socialGroupsManager,
			SocialFS socialFS, TextPost textPost,
			 @Named("server.port") String port, @Named("server.host.name") String hostName) {
		this.snet = snet;
		this.socialGroupsManager = socialGroupsManager;
		this.socialFS = socialFS;
		this.textPost = textPost;
		this.hostName = hostName;
		this.port = port;
	}
	
	//XXX req of type POST with "/addTextPost/{groupName}" URI and body containing text=post text
	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		//get post text from body
		String text = HttpStringExtractor.fromBodyAfterFirstEqualsSign(req);
		
		//get social group name from URI
		String groupName = HttpStringExtractor.fromURIAfterLastSlash(req);
		
		WolfPack socialGroup = socialGroupsManager.findSocialGroup(groupName);
		if (socialGroup == null) {
			res.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		
		try {
			snet.getWall().publish(textPost.setText(text), socialGroup);
		} catch (GroupNotFoundException e) {
			System.out.println("Social group" + socialGroup + "not found");
			res.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (WallNotFound e) {
			System.out.println("Wall not found");
			res.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (FileNotFoundException e) {
			System.out.println("File /wall/posts/ not found");
			res.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		res.setStatusCode(HttpStatus.SC_SEE_OTHER);
		//FIXME where to redirect?
		res.setHeader("Location", hostName + ":" + port +
				"/viewMessageBoard/" + socialFS.getCredentials().getProfile().getUserId().toString());
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}
}
