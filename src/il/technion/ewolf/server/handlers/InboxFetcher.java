package il.technion.ewolf.server.handlers;


import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class InboxFetcher implements JsonDataHandler {
	private static final String SENDER_NOT_FOUND_MESSAGE = "Not found";
	private final SocialMail smail;

	@Inject
	public InboxFetcher(SocialMail smail) {
		this.smail = smail;
	}

	private class JsonReqInboxParams {
		//The max amount of messages to retrieve.
		Integer maxMessages;
		//Time in milliseconds since 1970, to retrieve messages older than this date.
		Long olderThan;
		//Time in milliseconds since 1970, to retrieve messages newer than this date.
		Long newerThan;
		//User ID, to retrieve messages from a specific sender.
		String fromSender;
		
		public boolean isMatchCriteria(InboxMessage msg) {
			return 	(fromSender == null || fromSender.equals(msg.senderID)) &&
					(newerThan == null || newerThan <= msg.timestamp) &&
					(olderThan == null || olderThan >= msg.timestamp);
		}
	}

	class InboxMessage implements Comparable<InboxMessage>{
		String senderID;
		String senderName;
		Long timestamp;
		String message;

		@Override
		public int compareTo(InboxMessage o) {
			return -Long.signum(this.timestamp - o.timestamp); //"-" for ordering from newer messages to older
		}
	}

	@SuppressWarnings("unused")
	class InboxResponse {
		private List<InboxMessage> lst;
		private String result;
		public InboxResponse(List<InboxMessage> lst, String result) {
			this.lst = lst;
			this.result = result;
		}
	}

	/**
	 * @param	jsonReq serialized object of JsonReqInboxParams class
	 * @return	inbox list, each element contains sender ID, sender name,
	 * 			timestamp and message text, sorted from newer date to older
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqInboxParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqInboxParams.class);
		} catch (Exception e) {
			return new InboxResponse(null, RES_BAD_REQUEST);
		}
		List<InboxMessage> lst = new ArrayList<InboxMessage>();			

		List<SocialMessage> messages = smail.readInbox();
		for (SocialMessage m : messages) {
			//TODO make separate thread that accepts PokeMessages
			Class<? extends SocialMessage> messageClass = m.getClass();
			if (messageClass == PokeMessage.class) {
				((PokeMessage)m).accept();
				continue;
			}
				
			InboxMessage msg = new InboxMessage();
			try {
				Profile sender = m.getSender();
				msg.senderID = sender.getUserId().toString();
				msg.senderName = sender.getName();
			} catch (ProfileNotFoundException e) {
				msg.senderID = SENDER_NOT_FOUND_MESSAGE;
				msg.senderName = SENDER_NOT_FOUND_MESSAGE;
				e.printStackTrace();
			}
			msg.timestamp = m.getTimestamp();
			
			if(jsonReqParams.isMatchCriteria(msg)) {
				msg.message = ((ContentMessage)m).getMessage();
				lst.add(msg);
			}
		}
		//sort by timestamp
		Collections.sort(lst);	
		
		if (jsonReqParams.maxMessages != null && lst.size() > jsonReqParams.maxMessages) {
			lst = lst.subList(0, jsonReqParams.maxMessages);
		}
		
		return new InboxResponse(lst, RES_SUCCESS);
	}
}
