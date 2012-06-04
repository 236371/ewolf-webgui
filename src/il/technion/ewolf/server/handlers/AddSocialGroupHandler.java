package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.server.HttpStringExtractor;

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

public class AddSocialGroupHandler  implements HttpRequestHandler{
	private static final String HANDLER_REGISTER_PATTERN = "/addSocialGroup";
	private final WolfPackLeader socialGroupsManager;
	private final String hostName;
	private final String port;
	
	@Inject
	public AddSocialGroupHandler(WolfPackLeader socialGroupsManager, @Named("server.port") String port,
			@Named("server.host.name") String hostName) {
		this.socialGroupsManager = socialGroupsManager;
		this.port = port;
		this.hostName = hostName;
	}

	//XXX req of type POST with "/addSocialGroup" URI and body containing groupName=name
	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");

		//get group name from the body
		String groupName = HttpStringExtractor.fromBodyAfterFirstEqualsSign(req);

		socialGroupsManager.findOrCreateSocialGroup(groupName);

		res.setStatusCode(HttpStatus.SC_SEE_OTHER);
		res.setHeader("Location", hostName + ":" + port + "/viewSocialGroupMembers/" + groupName);
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}
}
