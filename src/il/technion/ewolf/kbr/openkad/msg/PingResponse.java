package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

import com.google.inject.name.Named;

public class PingResponse extends KadResponse {

	private static final long serialVersionUID = -5054944878934710372L;

	PingResponse(long id, @Named("openkad.local.node") Node src) {
		super(id, src);
	}

}
