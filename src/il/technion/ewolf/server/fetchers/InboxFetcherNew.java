package il.technion.ewolf.server.fetchers;


import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

public class InboxFetcherNew implements JsonDataFetcher {
	private final SocialMail smail;

	@Inject
	public InboxFetcherNew(SocialMail smail) {
		this.smail = smail;
	}
	
	class InboxMessage implements Comparable<InboxMessage>{
		String senderID;
		String senderName;
		Long timestamp;
		String message;
		String className; //TODO field for tests, delete at the end
		
		private InboxMessage(String senderID, String senderName, Long timestamp, String message, String className) {
			this.senderID = senderID;
			this.senderName = senderName;
			this.timestamp = timestamp;
			this.message = message;
			this.className = className;
		}

		@Override
		public int compareTo(InboxMessage o) {
			return -Long.signum(this.timestamp - o.timestamp); //"-" for ordering from newer messages to older
		}
	}

	/**
	 * @param	parameters	The method gets exactly 4 parameters for filtering inbox.
	 * 			[0]:		The amount of messages to retrieve.
	 * 			[1]:		Time in milliseconds since 1970, to retrieve messages older than this date.
	 * 			[2]:		Time in milliseconds since 1970, to retrieve messages newer than this date.
	 * 			[3]:		User ID, to retrieve messages from a specific sender.
	 * @return	inbox list, each element contains sender ID, sender name, timestamp and message text,
	 * sorted from newer date to older
	 */
	@Override
	public Object fetchData(String... parameters) {
		if(parameters.length != 4) {
			return null;
		}
		for (int i=0; i<4; i++) {
			if (parameters[i].equals("null")) {
				parameters[i]=null;
			}
		}
		Integer filterNumOfMessages = (parameters[0]!=null)?Integer.valueOf(parameters[0]):null;
		Long filterToDate = (parameters[1]!=null)?Long.valueOf(parameters[1]):null;
		Long filterFromDate = (parameters[2]!=null)?Long.valueOf(parameters[2]):null;
		String filterByUserId = parameters[3];
		
		List<SocialMessage> messages = smail.readInbox();
		List<InboxMessage> lst = new ArrayList<InboxMessage>();
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
				System.out.println("Sender of social message " + m.toString() + "not found");
				e.printStackTrace();
			}
			Long timestamp = m.getTimestamp();
			if (filterByUserId==null || filterByUserId.equals(senderID)) {
				if (filterFromDate==null || filterFromDate<=timestamp) {
					if (filterToDate==null || filterToDate>=timestamp) {
						lst.add(new InboxMessage(senderID, senderName, timestamp,
								((ContentMessage)m).getMessage(), messageClass.getCanonicalName()));
					}
				}
			}
		}
		//sort by timestamp
		Collections.sort(lst);
		
		if (filterNumOfMessages==null) {
			return lst;
		} else {
			return (filterNumOfMessages<lst.size())?getFirstNElements(filterNumOfMessages, lst):lst;
		}
	}
	
	private <T> List<T> getFirstNElements(int n, List<T> list) {
		List<T> newList = new ArrayList<T>();
		for (int i=0; i<n; i++) {
			newList.add(list.get(i));
		}
		return newList;		
	}
}
