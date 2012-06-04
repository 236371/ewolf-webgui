package il.technion.ewolf.server.handlers;

import il.technion.ewolf.SocialNetwork;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.posts.TextPost;
import il.technion.ewolf.server.HttpStringExtractor;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

public class ViewMessageBoardHandler implements HttpRequestHandler {
	private static final String HANDLER_REGISTER_PATTERN = "/viewMessageBoard/*";
	private final SocialNetwork snet;
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;
	
	@SuppressWarnings("unused")
	private class JsonPost {
		private String postID;
		private Long timestamp;
		private String text;
		
		private JsonPost(String postID, Long timestamp, String text) {
			this.postID = postID;
			this.timestamp = timestamp;
			this.text = text;
		}
	}
	
	@Inject
	public ViewMessageBoardHandler(SocialNetwork snet, SocialFS socialFS, UserIDFactory userIDFactory) {
		this.snet = snet;
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}

	//XXX req of type GET with "/viewMessageBoard/{UserID}" URI
	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		//get user ID from URI
		String strUid = HttpStringExtractor.fromURIAfterLastSlash(req);
		UserID uid = userIDFactory.getFromBase64(strUid);

		Profile profile;
		try {
			profile = socialFS.findProfile(uid);			
		} catch (ProfileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Profile with UserID" + uid + "not found");
			res.setStatusCode(HttpStatus.SC_NOT_FOUND);
			res.setEntity(new FileEntity(new File("404.html"),"text/html"));
			e.printStackTrace();
			return;
		}
		
		List<Post> posts;
		try {
			posts = snet.getWall(profile).getAllPosts();
		} catch (WallNotFound e) {
			System.out.println("Wall not found");
			res.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		List<ViewMessageBoardHandler.JsonPost> lst = new ArrayList<ViewMessageBoardHandler.JsonPost>();
		for (Post post: posts) {
			lst.add(new JsonPost(post.getPostId().toString(), post.getTimestamp(), ((TextPost)post).getText()));
		}
		String json = gson.toJson(lst, lst.getClass());
		res.setEntity(new StringEntity(json));
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}
}
