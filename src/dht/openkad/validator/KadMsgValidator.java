package dht.openkad.validator;

import dht.openkad.KadMsg;

public interface KadMsgValidator {

	/**
	 * validate a kad message
	 * @param msg
	 * @return true if message is valid
	 */
	public void validate(KadMsg msg) throws Exception;
}
