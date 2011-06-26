package dht.openkad.validator;

import dht.openkad.KadMsg;
import dht.openkad.KadNode;

public class KadSourceValidator implements KadMsgValidator {

	private final KadNode src;
	
	public KadSourceValidator(KadNode src) {
		this.src = src;
	}
	
	@Override
	public void validate(KadMsg msg) {
		if (!src.equals(msg.getSrc()))
			throw new IllegalArgumentException("wrong src, expecting "+src+" but got "+msg.getSrc());
	}

}
