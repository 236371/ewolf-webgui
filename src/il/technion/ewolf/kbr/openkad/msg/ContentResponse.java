package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

public class ContentResponse extends KadResponse {

	private static final long serialVersionUID = -4479208136049358778L;

	private byte[] content;
	
	ContentResponse(long id, Node src) {
		super(id, src);
	}

	public byte[] getContent() {
		return content;
	}
	
	public ContentResponse setContent(byte[] content) {
		this.content = content;
		return this;
	}

}
