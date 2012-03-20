package il.technion.ewolf.http;

import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class HttpConnectorTest {

	
	@Test
	public void itShouldSendAndRecvGetRequests() throws Exception{
		int basePort = 10000;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		List<HttpConnector> connectors = new ArrayList<HttpConnector>();
		for (int i=0; i < 2; ++i) {
			Injector injector = Guice.createInjector(
					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(i+basePort)),
					
					new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "1")
					.setProperty("openkad.bucket.kbuckets.maxsize", "3")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			
			HttpConnector connector = injector.getInstance(HttpConnector.class);
			connector.bind();
			connector.start();
			connectors.add(connector);
			
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		System.out.println("finished joining");
		
		HttpRequest req = new BasicHttpRequest("GET", "/testing");
		connectors.get(0).register("/testing", new HttpRequestHandler() {
			
			@Override
			public void handle(HttpRequest req, HttpResponse res, HttpContext ctx) throws HttpException, IOException {
				Assert.assertEquals("/testing", req.getRequestLine().getUri());
				res.setEntity(new StringEntity("blah !!"));
			}
		});
		HttpResponse res = connectors.get(1).send(kbrs.get(0).getLocalNode(), req);
		
		Assert.assertEquals("blah !!", EntityUtils.toString(res.getEntity()));
	}
	
	
	@Test
	public void itShouldSendAndRecvPostRequests() throws Exception{
		int basePort = 10100;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		List<HttpConnector> connectors = new ArrayList<HttpConnector>();
		for (int i=0; i < 2; ++i) {
			Injector injector = Guice.createInjector(
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", ""+(i+basePort)),
				
				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "1")
					.setProperty("openkad.bucket.kbuckets.maxsize", "3")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			
			HttpConnector connector = injector.getInstance(HttpConnector.class);
			connector.bind();
			connector.start();
			connectors.add(connector);
			
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		System.out.println("finished joining");
		
		BasicHttpEntityEnclosingRequest req = new BasicHttpEntityEnclosingRequest("POST", "/testing");
		req.setEntity(new StringEntity("this is a post msg !"));
		connectors.get(0).register("*", new HttpRequestHandler() {
			
			@Override
			public void handle(HttpRequest req, HttpResponse res, HttpContext ctx) throws HttpException, IOException {
				Assert.assertEquals("/testing", req.getRequestLine().getUri());
				Assert.assertEquals("this is a post msg !",
						EntityUtils.toString(((HttpEntityEnclosingRequest)req).getEntity()));
				res.setEntity(new StringEntity("blah !!"));
			}
		});
		HttpResponse res = connectors.get(1).send(kbrs.get(0).getLocalNode(), req);
		
		Assert.assertEquals("blah !!", EntityUtils.toString(res.getEntity()));
	}
	
}
