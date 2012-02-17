package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.msg.KadMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;

/**
 * Serialize a message into a gun-ziped json message
 * @author eyal.kibbar@gmail.com
 *
 */
public class JsonKadSerializer extends KadSerializer implements JsonSerializer<Serializable>, JsonDeserializer<Serializable> {

	private final Gson gson;
	
	private static final String classPackage = KadMessage.class.getPackage().getName()+".";
	
	@Inject
	JsonKadSerializer() {
		this.gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(Serializable.class, this)
			.create();
	}
	
	@Override
	public KadMessage read(InputStream in) throws IOException, ClassCastException, ClassNotFoundException {
		GZIPInputStream din = null;
		Reader utf8Reader = null;
		JsonReader reader = null;
		
		KadMessage msg = null;
		try {
			din = new GZIPInputStream(in);
			utf8Reader = new InputStreamReader(din, "UTF-8");
			reader = new JsonReader(utf8Reader);
		
			reader.beginArray();
			String clazzName = gson.fromJson(reader, String.class);
			msg = gson.fromJson(reader, Class.forName(classPackage+clazzName));
			reader.endArray();
			
		} finally {
			reader.close();
			utf8Reader.close();
			din.close();
		}
		
		return msg;
	}

	@Override
	public void write(KadMessage msg, OutputStream out) throws IOException {
		
		//System.out.println(KadMessage.class.getPackage().getName());
		GZIPOutputStream dout = null;
		Writer utf8Writer = null;
		JsonWriter writer = null;
		
		try {
			dout = new GZIPOutputStream(out);
			utf8Writer = new OutputStreamWriter(dout, "UTF-8");
			writer = new JsonWriter(utf8Writer);
			
			writer.beginArray();
			Class<?> clazz = msg.getClass();
			gson.toJson(clazz.getSimpleName(), String.class, writer);
			//System.out.println("writing class: "+clazz);
			gson.toJson(msg, clazz, writer);
			writer.endArray();
			
		} finally {
			writer.close();
			utf8Writer.close();
			dout.close();
		}
	}

	@Override
	public Serializable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		byte[] src = Base64.decodeBase64(json.getAsJsonPrimitive().getAsString());
		try {
			return JsonKadSerializer.this.deserialize(src);
		} catch (Exception e) {
			throw new JsonParseException(e);
		}
	}

	public JsonElement serialize(Serializable src, Type typeOfSrc, JsonSerializationContext context) {
		byte[] serialized = JsonKadSerializer.this.serialize(src);
		String s = Base64.encodeBase64String(serialized);
		//System.out.println("sending: "+s);
		return new JsonPrimitive(s);
	}

}
