package il.technion.ewolf.server.handlers;

import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

public class ViewInboxHandler implements HttpRequestHandler {
	private static final String HANDLER_REGISTER_PATTERN = "/viewInbox";
	private final SocialMail mail;
	
	@SuppressWarnings("unused")
	private class JsonMessage {
		private String sender;
		private Long timestamp;
		private String className;
		
		private JsonMessage(String sender, Long timestamp, String className) {
			this.sender = sender;
			this.timestamp = timestamp;
			this.className = className;
		}
	}

	@Inject
	public ViewInboxHandler(SocialMail mail) {
		this.mail = mail;
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding general headers to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		List<SocialMessage> messages = mail.readInbox();
		List<ViewInboxHandler.JsonMessage> lst = new ArrayList<JsonMessage>();
		for (SocialMessage m : messages) {
			try {
				lst.add(new JsonMessage(m.getSender().getUserId().toString(), m.getTimestamp(),
						m.getClass().getCanonicalName()));
			} catch (ProfileNotFoundException e) {
				System.out.println("Sender of social message" + m.toString() + "not found");
				res.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}

		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		String json = gson.toJson(lst, lst.getClass());
		res.setEntity(new StringEntity(json));
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
	}
	
	public static String getRegisterPattern() {
		return HANDLER_REGISTER_PATTERN;
	}
}
