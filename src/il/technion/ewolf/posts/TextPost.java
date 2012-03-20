package il.technion.ewolf.posts;

import il.technion.ewolf.PostID;
import il.technion.ewolf.socialfs.SocialFS;

import com.google.gson.annotations.Expose;
import com.google.inject.Inject;
import com.google.inject.name.Named;


public class TextPost extends Post {

	private static final long serialVersionUID = -5112196849869662463L;
	
	@Expose private String text;
	
	@Inject
	TextPost(@Named("ewolf.rnd.postID") PostID postID, SocialFS socialFS) {
		super(postID, socialFS);
	}
	
	public TextPost setText(String text) {
		this.text = text;
		return this;
	}
	
	public String getText() {
		return text;
	}

	/*
	@Override
	public void exportToJson(JsonWriter writer) throws IOException {
		writer.beginObject();
		
		exportPostToJson(writer);
		writer.name("text").value(text);
		
		writer.endObject();
	}
	*/
}
