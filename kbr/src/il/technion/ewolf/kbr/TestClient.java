package il.technion.ewolf.kbr;

import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import com.google.inject.Guice;

public class TestClient implements HttpRequestHandler {

	private KeybasedRouting kbr;
	private HttpParams params;
	private BufferingHttpServiceHandler handler;
	
	public TestClient(Properties props) throws IOException {
		kbr = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
		
		kbr.create();
		
		params = new SyncBasicHttpParams()
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
                new ResponseDate(),
                new ResponseServer(),
                new ResponseContent(),
                new ResponseConnControl()
        });
        
        handler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);

        HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
        reqistry.register("*", this);

        handler.setHandlerResolver(reqistry);
        
        
	}
	
	public void join(URI bootstrap) throws Exception {
		kbr.join(bootstrap).get();
	}
	
	public List<String> findNodes(String keyString, int n) throws Exception {
		List<String> $ = new ArrayList<String>();
		List<Node> nodes = kbr.findNodes(kbr.getKeyFactory().getFromKey(keyString), n).get();
		for (int i=0; i < nodes.size(); ++i) {
			$.add(nodes.get(i).getKey().toBase64());
		}
		return $;
	}
	
	public void startTestServer(int testport) throws Exception {
		IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(handler, params);
		ListeningIOReactor ioReactor = new DefaultListeningIOReactor(1, params);
		ioReactor.listen(new InetSocketAddress(testport));
		ioReactor.execute(ioEventDispatch);
		System.out.println("Shutdown");
	}
	
	private Map<String, String> getHttpRequestParams(HttpRequest req) throws Exception {
		Map<String, String> $ = new HashMap<String, String>();
		for (String pair : new URI(req.getRequestLine().getUri()).getRawQuery().split("&")) {
			$.put(URLDecoder.decode(pair.split("=")[0], "UTF8"),
					URLDecoder.decode(pair.split("=")[1], "UTF8"));
		}
		return $;
	}
	
	private Set<String> getNeighbors() {
		Set<String> $ = new HashSet<String>();
		for (Node n : kbr.getNeighbors())
			$.add(n.getKey().toBase64());
		return $;
	}
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx) {
		try {
			Map<String, String> reqParams = getHttpRequestParams(req);
			
			String op = reqParams.get("op");
			if ("testing".equals(op)) {
				res.setEntity(new StringEntity("OK !!"));
			}
			
			else if ("join".equals(op)) {
				join(new URI(reqParams.get("bootstrap")));
			}
			
			else if ("findNodes".equals(op)) {
				List<String> l = findNodes(reqParams.get("key"), Integer.parseInt(reqParams.get("n")));
				//Gson gson = new Gson();
				//res.setEntity(new StringEntity(gson.toJson(l)));
				res.setEntity(new StringEntity(l.toString()));
			}
			
			else if ("getNeighbors".equals(op)) {
				Set<String> l = getNeighbors();
				//Gson gson = new Gson();
				//res.setEntity(new StringEntity(gson.toJson(l)));
				res.setEntity(new StringEntity(l.toString()));
			}
			
			else {
				res.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				return;
			}
			
			res.setStatusCode(HttpStatus.SC_OK);
			
		} catch (Exception e) {
			e.printStackTrace();
			res.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	
	/**
	 * @param args
	 * args[0] - porps file
	 * args[1] - testing port
	 */
	public static void main(String[] args) throws Exception {
		String propsFilename = args[0];
		int testingport = Integer.parseInt(args[1]);
		
		Properties props = new Properties();
		props.load(new FileInputStream(propsFilename));
		
		TestClient tc = new TestClient(props);
		tc.startTestServer(testingport);
	}



}
