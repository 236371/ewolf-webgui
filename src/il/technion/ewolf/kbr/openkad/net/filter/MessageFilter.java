package il.technion.ewolf.kbr.openkad.net.filter;

import il.technion.ewolf.kbr.openkad.msg.KadMessage;

public interface MessageFilter {

	boolean shouldHandle(KadMessage m);
	
}
