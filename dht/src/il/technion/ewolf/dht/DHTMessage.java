package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import encoding.Base64;

class DHTMessage {


	enum RPC {
		STORE,
		FIND_VALUE, FIND_VALUE_RESPONSE,
	}
	
	private final RPC rpc;
	private final Key key;
	private final String[] data;
	private final String responseTag;
	
	
	DHTMessage(RPC rpc, Key key, Set<byte[]> data, String responseTag) {
		this.rpc = rpc;
		this.key = key;
		this.responseTag = responseTag;
		this.data = new String[data.size()];
		int i=0;
		for (byte[] d : data) {
			this.data[i++] = Base64.encodeBytes(d);
		}
	}
	
	public String toJson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String msgString = gson.toJson(this);
		return msgString;
	}
	
	public Set<byte[]> getData() throws IOException {
		Set<byte[]> $ = new HashSet<byte[]>();
		for (String d : data) {
			$.add(Base64.decode(d));
		}
		return $;
	}
	
	public String getResponseTag() {
		return responseTag;
	}
	
	public RPC getRpc() {
		return rpc;
	}
	
	public Key getKey() {
		return key;
	}
	
}
