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
import com.google.gson.JsonArray;
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
	}

	@SuppressWarnings("unused")
	private class InboxMessage implements Comparable<InboxMessage>{
		private String senderID;
		private String senderName;
		private Long timestamp;
		private String message;
		
		private InboxMessage(String senderID, String senderName, Long timestamp,
				String message, String className) {
			this.senderID = senderID;
			this.senderName = senderName;
			this.timestamp = timestamp;
			this.message = message;
		}

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
	public JsonElement handleData(JsonElement jsonReq) {
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
			
			Profile sender;
			String senderID = null;
			String senderName = null;

			try {
				sender = m.getSender();
				senderID = sender.getUserId().toString();
				senderName = sender.getName();
			} catch (ProfileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Long timestamp = m.getTimestamp();
			if (jsonReqParams.fromSender==null || jsonReqParams.fromSender.equals(senderID)) {
				if (jsonReqParams.newerThan==null || jsonReqParams.newerThan<=timestamp) {
					if (jsonReqParams.olderThan==null || jsonReqParams.olderThan>=timestamp) {
						lst.add(new InboxMessage(senderID, senderName, timestamp,
								((ContentMessage)m).getMessage(), messageClass.getCanonicalName()));
					}
				}
			}
		}
		//sort by timestamp
		Collections.sort(lst);
		
		List<InboxMessage> finalList = lst;
		if (jsonReqParams.maxMessages!=null && jsonReqParams.maxMessages<lst.size()) {
			finalList = getFirstNElements(jsonReqParams.maxMessages, lst);
		}
		return listToJsonArray(finalList);
	}

	private JsonArray listToJsonArray(List<InboxMessage> lst) {
		JsonArray jsonArray = new JsonArray();
		Gson gson = new Gson();
		for (InboxMessage m: lst) {
			jsonArray.add(gson.toJsonTree(m));
		}
		return jsonArray;
	}
	
	private <T> List<T> getFirstNElements(int n, List<T> list) {
		List<T> newList = new ArrayList<T>();
		for (int i=0; i<n; i++) {
			newList.add(list.get(i));
		}
		return newList;		
	}
}
