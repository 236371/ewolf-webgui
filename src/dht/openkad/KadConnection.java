package dht.openkad;

import java.io.IOException;

import dht.openkad.validator.KadMsgValidator;

public abstract class KadConnection {
	
	public abstract void sendMessage(KadMsg msg) throws IOException;
	
	public abstract KadMsg recvMessage(KadMsgValidator ... validators) throws IOException;
	
	public abstract void close();
}
