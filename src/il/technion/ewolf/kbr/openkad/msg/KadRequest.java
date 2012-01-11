package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public abstract class KadRequest extends KadMessage {

	private static final long serialVersionUID = 7014729033211615669L;

	@Inject
	KadRequest(long id, @Named("openkad.local.node") Node src) {
		super(id, src);
	}

	public abstract KadResponse generateResponse(Node localNode);
	
}
