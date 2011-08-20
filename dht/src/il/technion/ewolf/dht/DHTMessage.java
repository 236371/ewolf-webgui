package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.util.Collection;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class DHTMessage {


	enum RPC {
		STORE,
		FIND_VALUE, FIND_VALUE_RESPONSE,
	}
	
	private final RPC rpc;
	private final Key key;
	private final Set<String> data;
	private final String responseTag;
	
	
	DHTMessage(RPC rpc, Key key, Set<String> data, String responseTag) {
		this.rpc = rpc;
		this.key = key;
		this.responseTag = responseTag;
		
		this.data = data;
		/*
		this.data = convert(dhtItems, new Converter<DHTItem, String>() {
			@Override
			public String convert(DHTItem item) {
				return Base64.encodeBytes(item.getBytes());
			}
		});
		*/
	}
	
	public String toJson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String msgString = gson.toJson(this);
		return msgString;
	}
	
	public Collection<String> getData() {
		return data;
	}
	
	/*
	public Collection<DHTItem> getData() throws IOException {
		return convert(data, new Converter<String, DHTItem>() {
			@Override
			public DHTItem convert(String d) {
				try {
					return new DHTItem(Base64.decode(d));
				} catch (Exception e) {
					throw new RuntimeException("error decoding data", e);
				}
			}
		});
	}
	*/
	
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
