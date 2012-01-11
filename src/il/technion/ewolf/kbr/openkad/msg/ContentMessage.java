package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ContentMessage extends KadMessage {

	private static final long serialVersionUID = -57547778613163861L;
	
	private String tag;
	private byte[] content;

	
	@Inject
	ContentMessage(
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
	public ContentMessage setContent(byte[] content) {
		this.content = content;
		return this;
	}
	
	public ContentMessage setTag(String tag) {
		this.tag = tag;
		return this;
	}

}
