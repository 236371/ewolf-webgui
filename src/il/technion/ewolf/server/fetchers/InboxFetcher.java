package il.technion.ewolf.server.fetchers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class InboxFetcher implements JsonDataFetcher {
	
	final DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	private int year = 2000;
	private int globalKey = 1;
	
	public class InboxMessage implements Comparable<InboxMessage> {
		
		public InboxMessage(String sender, String timestamp, String message){
			this.sender = sender;
			this.timestamp = timestamp;
			this.message = message;
			
			try {
				itsDate = df.parse(timestamp);
			} catch (ParseException e) {
				itsDate = null;
				e.printStackTrace();
			}
		}
		
		public String sender;
		public String timestamp;
		public String message;
		
		transient private Date itsDate;

		@Override
		public int compareTo(InboxMessage o) {			
			int res = -itsDate.compareTo(o.itsDate);
			
			if(res == 0) {
				if(o.getClass() == DummyInboxItem.class) {
					return -o.compareTo(this);
				} else {
					return sender.compareTo(o.sender);
				}				
			} else {
				return res;
			}
		}
	}
	
	public class DummyInboxItem extends InboxMessage {
		
		public DummyInboxItem(String timestamp, boolean alwaysErlier) {
			super(new String(), timestamp, new String());
			this.alwaysErlier = alwaysErlier;
		}
		
		public String timestamp;
		
		transient private boolean alwaysErlier;

		@Override
		public int compareTo(InboxMessage o) {			
			int res = -super.itsDate.compareTo(o.itsDate);
			
			if(res == 0) {
				if(alwaysErlier) {
					return 1;
				} else {
					return -1;
				}
			} else {
				return res;
			}
		}
	}

	@Override
	public Object fetchData(String... parameters) {
		/*!
		 * The parameters should be:
		 * 	0:	The amount of messages to retrieve.
		 * 	1:	Retrieve messages older than this date
		 * 	2:	Retrieve messages newer than this date
		 * 	3:	Retrieve messages from a specific sender (id)
		 */
		
		if(parameters.length != 4) {
			return null;
		}

		int maxCount = Integer.parseInt(parameters[0]);
		
		String olderThen,newerThen;
		if(parameters[1].equals("null")) {
			Date today = Calendar.getInstance().getTime();
			today.setYear(today.getYear()+1);
			olderThen = df.format(today);
		} else {
			olderThen = parameters[1];
		}
		
		if(parameters[2].equals("null")) {
			newerThen = "00/00/00";
		} else {
			newerThen = parameters[2];
		}
		
		InboxMessage olderThenElement = new DummyInboxItem(olderThen,true);				
		InboxMessage newerThenElement = new DummyInboxItem(newerThen,false);
		
		SortedSet<InboxFetcher.InboxMessage> inbox = getInbox().
				subSet(olderThenElement,newerThenElement);
		
		List<InboxFetcher.InboxMessage> smallInbox = new ArrayList<InboxFetcher.InboxMessage>();
		
		for (InboxMessage item : inbox) {
			if(smallInbox.size() >= maxCount) {
				break;
			}
			
			if(parameters[3].equals("null") || item.sender.equals(parameters[3])) {
				smallInbox.add(item);
			}
		}
		
		if(smallInbox.isEmpty()) {
			year++;
		}
		
		return smallInbox;
	}
	
	private SortedSet<InboxFetcher.InboxMessage> getInbox() {
		SortedSet<InboxFetcher.InboxMessage> inbox =
				new TreeSet<InboxFetcher.InboxMessage>();	
		inbox.add(new InboxMessage("Liran","01/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Anna","02/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Moshe","03/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Moshe","04/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("David","05/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Haim","06/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("לירן","07/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("אנה","08/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("גיל","09/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("דודו","10/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("חנה","12/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Craig","13/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("שלמה","14/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Facebook","15/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Gmail","16/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Google+","17/02/"+year,""+globalKey++));
		inbox.add(new InboxMessage("eWolf System","20/02/"+year,""+globalKey++));
		
		inbox.add(new InboxMessage("Liran","21/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","22/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","23/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","24/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","25/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","26/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","27/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","29/01/"+year,""+globalKey++));
		inbox.add(new InboxMessage("Liran","30/01/"+year,""+globalKey++));
		
		return inbox;
	}

}
