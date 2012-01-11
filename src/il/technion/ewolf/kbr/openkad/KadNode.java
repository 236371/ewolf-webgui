package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Node;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;

public class KadNode {

	private final AtomicLong lastContactTimestamp = new AtomicLong(0);
	private final AtomicBoolean beingPinged = new AtomicBoolean(false);
	private Node node;
	
	
	@Inject
	KadNode() {
	}
	
	@Override
	public int hashCode() {
		return getNode().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		return getNode().equals(((KadNode)obj).getNode());
	}
	
	public KadNode setNode(Node node) {
		this.node = node;
		return this;
	}

	public boolean lockForPing() {
		return beingPinged.compareAndSet(false, true);
	}
	
	public void releasePingLock() {
		beingPinged.set(false);
	}
	
	public KadNode setNodeWasContacted() {
		lastContactTimestamp.set(System.currentTimeMillis());
		return this;
	}
	
	
	public long getLastContact() {
		return lastContactTimestamp.get();
	}
	
	public boolean hasNeverContacted() {
		return lastContactTimestamp.get() == 0;
	}

	public boolean isPingStillValid(long validTimespan) {
		return lastContactTimestamp.get() + validTimespan > System.currentTimeMillis();
	}

	public Node getNode() {
		return node;
	}

	public void setNodeWasContacted(long lastContact) {
		lastContactTimestamp.set(lastContact);
	}
	
	@Override
	public String toString() {
		return getNode().toString();
	}

	public boolean hasContacted() {
		return !hasNeverContacted();
	}
}
