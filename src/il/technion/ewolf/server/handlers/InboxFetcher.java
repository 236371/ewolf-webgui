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

	@SuppressWarnings("unused")
	private class InboxMessage implements Comparable<InboxMessage>{
		public String senderID;
		public String senderName;
		public Long timestamp;
		public String message;

		@Override
		public int compareTo(InboxMessage o) {
			return -Long.signum(this.timestamp - o.timestamp); //"-" for ordering from newer messages to older
		}
	}

	/**
	 * @param	jsonReq serialized object of JsonReqInboxParams class
	 * @return	array of JsonElements, each one is serialized object of InboxMessage class
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqInboxParams jsonReqParams = gson.fromJson(jsonReq, JsonReqInboxParams.class);
				
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
				// TODO What should we do here?
				// XXX Why should this happen anyway?
				e.printStackTrace();
			}

			msg.timestamp = m.getTimestamp();
			
			if(jsonReqParams.isMatchCriteria(msg)) {
				msg.message = ((ContentMessage)m).getMessage();
				// msg.className = messageClass.getCanonicalName();
				lst.add(msg);
			}
		}
		//sort by timestamp
		Collections.sort(lst);	
		
		if (jsonReqParams.maxMessages != null && lst.size() > jsonReqParams.maxMessages) {
			lst = lst.subList(0, jsonReqParams.maxMessages);
		}
		
		return lst;
		//return listToJsonArray(lst);
	}

//	private JsonArray listToJsonArray(List<InboxMessage> lst) {
//		JsonArray jsonArray = new JsonArray();
//		Gson gson = new Gson();
//
//		for (InboxMessage m: lst) {
//			jsonArray.add(gson.toJsonTree(m));
//		}
//		return jsonArray;
//	}
}
