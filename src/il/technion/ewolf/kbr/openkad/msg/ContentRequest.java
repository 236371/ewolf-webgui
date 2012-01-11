package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ContentRequest extends KadRequest {

	private static final long serialVersionUID = 918433377540165654L;

	private String tag;
	private byte[] content;
	
	@Inject
	ContentRequest(
			@Named("openkad.rnd.id") long id,
			@Named("openkad.local.node") Node src) {
		
		super(id, src);
	}

	public String getTag() {
		return tag;
	}
	
	public byte[] getContent() {
		return content;
	}
	public ContentRequest setContent(byte[] content) {
		this.content = content;
		return this;
	}
	
	public ContentRequest setTag(String tag) {
		this.tag = tag;
		return this;
	}
	
	
	@Override
	public ContentResponse generateResponse(Node localNode) {
		return new ContentResponse(getId(), localNode);
	}

	
	
}
