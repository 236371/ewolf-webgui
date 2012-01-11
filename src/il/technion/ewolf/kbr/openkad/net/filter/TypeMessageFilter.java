package il.technion.ewolf.kbr.openkad.net.filter;

import il.technion.ewolf.kbr.openkad.msg.KadMessage;

public class TypeMessageFilter implements MessageFilter {

	private final Class<? extends KadMessage> clazz;
	
	public TypeMessageFilter(Class<? extends KadMessage> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public boolean shouldHandle(KadMessage m) {
		return m.getClass().equals(clazz);
	}

	
}
