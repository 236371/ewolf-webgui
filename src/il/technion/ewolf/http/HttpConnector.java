package il.technion.ewolf.http;

import il.technion.ewolf.kbr.Node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.util.EntityUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Provides a simple http interface over a key based routing network.
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class HttpConnector {

	
	private final Provider<ServerSocket> srvScok;
	private final HttpParams serverParams;
	private final HttpParams clientParams;
	private final HttpService httpService;
	private final ExecutorService executor;
	private final Provider<HttpContext> httpContextProvider;
	private final HttpRequestHandlerRegistry registry;
	private final String scheme;
	private final HttpRequestExecutor clientExecutor;
	private final HttpProcessor clientProcessor;
	private final Provider<DefaultHttpClientConnection> connProvider;
	
	@Inject
	HttpConnector(
			ExecutorService executor,
			HttpService httpService,
			Provider<HttpContext> httpContextProvider,
			HttpRequestHandlerRegistry registry,
			HttpRequestExecutor clientExecutor,
			Provider<DefaultHttpClientConnection> connProvider,
			@Named("httpconnector.proc.client") HttpProcessor clientProcessor,
			@Named("httpconnector.params.client") HttpParams clientParams,
			@Named("httpconnector.params.server") HttpParams serverParams,
			@Named("httpconnector.scheme") String scheme,
			@Named("httpconnector.net.sock") Provider<ServerSocket> srvScok) {
		
		this.registry = registry;
		this.srvScok = srvScok;
		this.clientProcessor = clientProcessor;
		this.scheme = scheme;
		this.clientParams = clientParams;
		this.serverParams = serverParams;
		this.clientExecutor = clientExecutor;
		this.httpService = httpService;
		this.executor = executor;
		this.httpContextProvider = httpContextProvider;
		this.connProvider = connProvider;
	}
	
	/**
	 * Binds the tcp socket
	 */
	public void bind() {
		srvScok.get();
	}
	
	
	/**
	 * Starts the server
	 */
	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				while (true) {
					try {
						Socket socket = srvScok.get().accept();
						System.out.println("incoming http request from "+socket.getRemoteSocketAddress());
						final DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
						conn.bind(socket, serverParams);
						
						executor.execute(new Runnable() {
							@Override
							public void run() {
								try {
									httpService.handleRequest(conn, httpContextProvider.get());
								} catch (Exception e) {
									// nothing 2 do
									e.printStackTrace();
								}
							}
						});
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	/**
	 * Registers a handler
	 * @param pattern http request handler (can be *)
	 * @param handler the provided handler for incoming requests
	 */
	public void register(String pattern, HttpRequestHandler handler) {
		//System.out.println("registering pattern "+pattern);
		registry.register(pattern, handler);
	}
	
	/**
	 * Sends a http request to a node in the key based routing network
	 * @param to the destination node
	 * @param req the http request
	 * @return the destination node's response
	 * @throws IOException in case of connection error or some other tcp or http error
	 */
	public HttpResponse send(Node to, HttpRequest req) throws IOException {
		HttpHost host = new HttpHost(to.getInetAddress().getHostAddress(), to.getPort(scheme));
		
		HttpContext context = httpContextProvider.get();
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
		
		DefaultHttpClientConnection conn = connProvider.get();
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		Socket socket = new Socket(host.getHostName(), host.getPort());
		HttpResponse response = null;
		try {
	        conn.bind(socket, clientParams);
	        req.setParams(clientParams);
	        clientExecutor.preProcess(req, clientProcessor, context);
			response = clientExecutor.execute(req, conn, context);
	        response.setParams(clientParams);
			clientExecutor.postProcess(response, clientProcessor, context);
			
			if (response.getEntity() != null) {
				ByteArrayEntity entity = new ByteArrayEntity(EntityUtils.toByteArray(response.getEntity()));
				response.setEntity(entity);
			}
			
		} catch (HttpException e) {
			throw new IOException(e);
		} finally {
			conn.close();
		}
		
		return response;
	}
	
}

