package il.technion.ewolf.dht;

import il.technion.ewolf.dht.DHTMessage.RPC;
import il.technion.ewolf.kbr.Key;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

class DHTMessageBuilder {

	private RPC rpc = null;
	private Set<byte[]> data = new HashSet<byte[]>();
	private Key key = null;
	private String responseTag;
	
	DHTMessageBuilder() {
		
	}
	
	DHTMessageBuilder(InputStream in) throws JsonIOException, JsonSyntaxException, IOException {
		this((DHTMessage) new GsonBuilder().create().fromJson(
				new JsonReader(new InputStreamReader(in, "UTF-8")),
				new TypeToken<DHTMessage>() {}.getType()));
	}
	
	DHTMessageBuilder(DHTMessage msg) throws IOException {
		this.rpc = msg.getRpc();
		this.data.addAll(msg.getData());
		this.key = msg.getKey();
		this.responseTag = msg.getResponseTag();
	}
	
	public DHTMessageBuilder setRpc(RPC rpc) {
		this.rpc = rpc;
		return this;
	}
	
	public DHTMessageBuilder setResponseTag(String responseTag) {
		this.responseTag = responseTag;
		return this;
	}
	
	public DHTMessageBuilder addData(byte[] data) {
		this.data.add(data);
		return this;
	}
	
	public DHTMessageBuilder addAllData(Collection<byte[]> data) {
		this.data.addAll(data);
		return this;
	}
	
	public DHTMessageBuilder setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public DHTMessage build() {
		if (key == null)
			throw new IllegalArgumentException("missing key");
		
		if (rpc == RPC.STORE && data.isEmpty())
			throw new IllegalArgumentException("missing data");
		
		if (rpc == RPC.FIND_VALUE && responseTag == null)
			throw new IllegalArgumentException("missing reponse tag");
		
		return new DHTMessage(rpc, key, data, responseTag);
	}
	
}
