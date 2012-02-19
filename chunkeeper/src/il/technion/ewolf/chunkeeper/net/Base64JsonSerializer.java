package il.technion.ewolf.chunkeeper.net;

import il.technion.ewolf.chunkeeper.ChunkId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

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

public class Base64JsonSerializer implements ChunkeeperSerializer {

	private final Gson gson;
	
	@Inject
	Base64JsonSerializer() {
		gson = new GsonBuilder()
			.registerTypeAdapter(byte[].class, new JsonSerializer<byte[]>() {
				@Override
				public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
					return new JsonPrimitive(Base64.encodeBase64String(src));
				}
				
			})
			.registerTypeAdapter(byte[].class, new JsonDeserializer<byte[]>() {
	
				@Override
				public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
					return Base64.decodeBase64(json.getAsJsonPrimitive().getAsString());
				}
				
			})
			.create();
	}
	
	@Override
	public ChunkId fromString(String str) {
		InputStream bin = null;
		//GZIPInputStream din = null;
		Reader utf8Reader = null;
		JsonReader reader = null;
		try {
			bin = new ByteArrayInputStream(Base64.decodeBase64(str));
			//din = new GZIPInputStream(bin);
			utf8Reader = new InputStreamReader(bin, "UTF-8");
			reader = new JsonReader(utf8Reader);
		
			return gson.fromJson(reader, ChunkId.class);
			
		} catch (Exception e) {
			throw new IllegalArgumentException("could not deserialize chunk id", e);
		} finally {
			try { reader.close(); } catch (Exception e) {}
			try { utf8Reader.close(); } catch (Exception e) {}
			//try { din.close(); } catch (Exception e) {}
			try { bin.close(); } catch (Exception e) {}
		}
		
	}

	@Override
	public String toUrlSafeString(ChunkId chunkId) {
		ByteArrayOutputStream bout = null;
		//GZIPOutputStream dout = null;
		Writer utf8Writer = null;
		JsonWriter writer = null;
		
		try {
			bout = new ByteArrayOutputStream();
			//dout = new GZIPOutputStream(bout);
			utf8Writer = new OutputStreamWriter(bout, "UTF-8");
			writer = new JsonWriter(utf8Writer);
			
			gson.toJson(chunkId, ChunkId.class, writer);
			
		} catch (Exception e) {
			throw new RuntimeException("cannot serialize", e);
		} finally {
			try { writer.close(); } catch (Exception e) {}
			try { utf8Writer.close(); } catch (Exception e) {}
			//try { dout.close(); } catch (Exception e) {}
			try { bout.close(); } catch (Exception e) {}
		}
		
		
		
		return Base64.encodeBase64URLSafeString(bout.toByteArray());
	}

}
