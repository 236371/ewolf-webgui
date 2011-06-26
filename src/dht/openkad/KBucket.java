package dht.openkad;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import dht.openkad.KadMsg.RPC;
import dht.openkad.validator.KadBasicMsgValidator;
import dht.openkad.validator.KadMsgValidator;
import dht.openkad.validator.KadSourceValidator;


class KBucket {

	private final List<KadNode> nodes = new LinkedList<KadNode>();
	private final int maxSize;
	private final KadMsg pingMsg;
	private final KadMsgValidator basicValidator;
	
	KBucket(int maxSize, KadNode localNode, KadBasicMsgValidator basicValidator) {
		this.maxSize = maxSize;
		
		pingMsg = new KadMsgBuilder()
			.setSrc(localNode)
			.setRpc(RPC.PING)
			.buildMessage();
		
		this.basicValidator = basicValidator;
	}
	
	public synchronized List<KadNode> getNodes() {
		return new ArrayList<KadNode>(nodes);
	}
	
	public synchronized void insertNodeIfNotFull(KadNode s) {
		if (nodes.size() < maxSize && !nodes.contains(s))
			nodes.add(s);
	}
	
	public void insertNode(KadNode s) {
		synchronized(this) {
			if (nodes.remove(s)) {
				nodes.add(s);
				return;
			}
		
			if (nodes.size() < maxSize) {
				nodes.add(s);
				return;
			}
		}
		
		KadConnection conn = null;
		KadNode x = null;
		try {
			
			synchronized(this) {
				if (nodes.isEmpty())
					return;
				x = nodes.get(0);
			}
			
			conn = x.openConnection();
			conn.sendMessage(pingMsg);
			conn.recvMessage(basicValidator, new KadSourceValidator(x));
			
			// PING WAS RECVED
			synchronized(this) {
				if (nodes.remove(x))
					nodes.add(x);
			}
			
		} catch (Exception e) {
			// PING WAS NOT RECVED
			synchronized(this) {
				if (nodes.remove(x))
					nodes.add(s);
			}
			
		} finally {
			if (conn != null)
				conn.close();
		}
	}
	
	public String toString() {
		return getNodes().toString();
	}
}
