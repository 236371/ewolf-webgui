package il.technion.ewolf.server.jsonDataHandlers;


import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import static il.technion.ewolf.server.EWolfResponse.*;

public class InboxFetcher implements JsonDataHandler {
	private static final String SENDER_NOT_FOUND_MESSAGE = "Not found";
	private final SocialMail smail;

	private List<SocialMessage> messages;
	private Date lastModified;
	private static final long cachedTime = 60000; // 60000ms = 1min

	@Inject
	public InboxFetcher(SocialMail smail) {
		this.smail = smail;
		messages = smail.readInbox();
		lastModified = new Date();
	}

	private static class JsonReqInboxParams {
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

	static class InboxMessage implements Comparable<InboxMessage>{
		String itemID;
		String senderID;
		String senderName;
		Long timestamp;
		String mail;

		@Override
		public int compareTo(InboxMessage o) {
			return -Long.signum(this.timestamp - o.timestamp); //"-" for ordering from newer messages to older
		}
	}

	static class InboxResponse extends EWolfResponse {
		List<InboxMessage> mailList;

		public InboxResponse(List<InboxMessage> lst) {
			this.mailList = lst;
		}

		public InboxResponse(String result) {
			super(result);
		}
	}

	/**
	 * @param	jsonReq serialized object of JsonReqInboxParams class
	 * @return	inbox list, each element contains sender ID, sender name,
	 * 			timestamp and message text, sorted from newer date to older
	 */
	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqInboxParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqInboxParams.class);
		} catch (Exception e) {
			return new InboxResponse(RES_BAD_REQUEST);
		}
		List<InboxMessage> lst = new ArrayList<InboxMessage>();			

		Long curTime = new Date().getTime();
		if (curTime - lastModified.getTime() > cachedTime) {
			messages = smail.readInbox();
			lastModified = new Date();
		}

		for (SocialMessage m : messages) {
			if (m.getClass() != ContentMessage.class) {
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
				msg.mail = ((ContentMessage)m).getMessage();
				lst.add(msg);
			}
		}
		//sort by timestamp
		Collections.sort(lst);	

		if (jsonReqParams.maxMessages != null && lst.size() > jsonReqParams.maxMessages) {
			lst = lst.subList(0, jsonReqParams.maxMessages);
		}

		return new InboxResponse(lst);
	}
}
