package il.technion.ewolf.chunkeeper.handlers;

import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.chunkeeper.ChunkId;
import il.technion.ewolf.chunkeeper.net.ChunkeeperSerializer;
import il.technion.ewolf.chunkeeper.storage.ChunkStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GetHandler extends AbstractHandler {

	private final String getHandlerPath;
	private final ChunkStore chunkStore;
	private final ChunkeeperSerializer serializer;
	
	@Inject
	GetHandler(
			@Named("chunkeeper.handlers.get.path") String getHandlerPath,
			ChunkStore chunkStore,
			ChunkeeperSerializer serializer) {
		
		this.getHandlerPath = getHandlerPath;
		this.chunkStore = chunkStore;
		this.serializer = serializer;
	}
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx) throws HttpException, IOException {
		Map<String, List<String>> q = parseQuery(req);
		
		if (q.get("chunkid") == null || q.get("chunkid").size() != 1)
			throw new IllegalArgumentException("expecting exactly 1 chunk id");
		
		String serializedChunkId = q.get("chunkid").get(0);
		
		
		ChunkId chunkId = serializer.fromString(serializedChunkId);
		Chunk chunk = chunkStore.get(chunkId);
		if (chunk == null)
			res.setStatusCode(HttpStatus.SC_NOT_FOUND);
		
		res.setEntity(new ByteArrayEntity(chunk.downloadRaw()));
	}

	@Override
	protected String getPath() {
		return getHandlerPath;
	}

}
