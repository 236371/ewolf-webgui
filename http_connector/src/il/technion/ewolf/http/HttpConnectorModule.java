package il.technion.ewolf.http;

import il.technion.ewolf.kbr.KeybasedRouting;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class HttpConnectorModule extends AbstractModule {
	
	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		// testing params, DONT TOUCH !!!
		// scheme name
		defaultProps.setProperty("httpconnector.scheme", "http");
				
				
		// performance
		// number of server threads
		defaultProps.setProperty("httpconnector.executor.nrthread", "1");
		
		// config, plz touch
		// port for opening the tcp socket on
		defaultProps.setProperty("httpconnector.net.port", "2345");
		
		
		return defaultProps;
	}
	
	public HttpConnectorModule() {
		this(new Properties());
	}
	
	public HttpConnectorModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public HttpConnectorModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(HttpConnector.class).in(Scopes.SINGLETON);
	}
	
	@Provides
	@Singleton
	@Named("httpconnector.net.sock")
	ServerSocket provideServerSocket(
			KeybasedRouting kbr,
			@Named("httpconnector.scheme") String scheme,
			@Named("httpconnector.net.port") int port) throws IOException {
		kbr.getLocalNode().addEndpoint(scheme, port);
		//System.out.println("http binding: "+port);
		return new ServerSocket(port);
	}
	
	
	@Provides
	@Singleton
	ExecutorService provideExecutor(@Named("httpconnector.executor.nrthread") int nrthread) {
		return Executors.newFixedThreadPool(nrthread); 
	}
	
	@Provides
	@Singleton
	HttpRequestHandlerRegistry provideRegistry() {
		return new HttpRequestHandlerRegistry();
	}
	
	@Provides
	@Singleton
	@Named("httpconnector.params.server")
	HttpParams provideHttpServerParams() {
		return new SyncBasicHttpParams()
	        .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
	        .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
	        .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
	        .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
	        .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "MyServer !!");
	}
	
	@Provides
	@Singleton
	@Named("httpconnector.params.client")
	HttpParams provideHttpClientParams() {
		HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "KadNet :)");
        HttpProtocolParams.setUseExpectContinue(params, true);
        return params;
	}
	
	@Provides
	@Singleton
	@Named("httpconnector.proc.server")
	HttpProcessor provideHttpServerProcessor() {
		return  new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
                new ResponseDate(),
                new ResponseServer(),
                new ResponseContent(),
                new ResponseConnControl()
        });
	}
	
	@Provides
	@Singleton
	@Named("httpconnector.proc.client")
	HttpProcessor provideHttpClientProcessor() {
		return  new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
				// Required protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()
        });
	}
	
	
	@Provides
	@Singleton
	HttpService provideHttpService(
			HttpRequestHandlerRegistry registry,
			@Named("httpconnector.params.server") HttpParams serverParams,
			@Named("httpconnector.proc.server") HttpProcessor serverProc) {
		
		return  new HttpService(
				serverProc, 
                new NoConnectionReuseStrategy(),
                new DefaultHttpResponseFactory(),
                registry,
                serverParams);
	}
	
	@Provides
	HttpContext provideHttpContext() {
		return new BasicHttpContext();
	}
	
	@Provides
	@Singleton
	HttpRequestExecutor provideHttpRequestExecutor() {
		return new HttpRequestExecutor();
	}
	
	@Provides
	DefaultHttpClientConnection provideDefaultHttpClientConnection() {
		return new DefaultHttpClientConnection();
	}
	
}
