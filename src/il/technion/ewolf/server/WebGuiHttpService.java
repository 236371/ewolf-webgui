package il.technion.ewolf.server;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerResolver;
import org.apache.http.protocol.HttpService;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

public class WebGuiHttpService extends HttpService {
	HttpSessionStore sessionStore;

	@Inject
	public WebGuiHttpService(HttpProcessor processor,
			ConnectionReuseStrategy connStrategy,
			HttpResponseFactory responseFactory,
			HttpRequestHandlerResolver handlerResolver, HttpParams params, HttpSessionStore sessionStore) {
		super(processor, connStrategy, responseFactory, handlerResolver, params);
		this.sessionStore = sessionStore;
		// TODO Auto-generated constructor stub
	}

	public WebGuiHttpService(HttpProcessor processor,
			ConnectionReuseStrategy connStrategy,
			HttpResponseFactory responseFactory,
			HttpRequestHandlerResolver handlerResolver,
			HttpExpectationVerifier expectationVerifier, HttpParams params) {
		super(processor, connStrategy, responseFactory, handlerResolver,
				expectationVerifier, params);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doService(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		boolean authorized = false;
		if (req.containsHeader("Cookie")) {
			Header[] headers = req.getHeaders("Cookie");
			for (Header h : headers) {
				String cookie = h.getValue();
				if (sessionStore.isValid(cookie)) {
					authorized = true;
					break;
				}
			}
		}
		
		if (!authorized) {
			String uri = req.getRequestLine().getUri();
			if (!uri.equals("/json")){
				res.setStatusCode(HttpStatus.SC_FORBIDDEN);
				return;
			} else {
				context.setAttribute("authorized", false);
			}
		}

//		if (needAuthorization(req)) {
//			boolean authorized = false;
//			if (req.containsHeader("Cookie")) {
//				Header[] headers = req.getHeaders("Cookie");
//				for (Header h : headers) {
//					String cookie = h.getValue();
//					if (sessionStore.isValid(cookie)) {
//						authorized = true;
//						break;
//					}
//				}
//			}
//			if (!authorized) {
//				res.setStatusCode(HttpStatus.SC_FORBIDDEN);
//				return;
//			}
//		}
		// if request needs authorization
		//		if authorization is invalid (=check cookies)
		//			reply: "forbidden" + redirect to login page
		// else
		super.doService(req, res, context);
	}
//
//	private boolean needAuthorization(HttpRequest req) throws ParseException, IOException {
//		String uri = req.getRequestLine().getUri();
//		if (uri.equals("/json")) {
//			String jsonReqAsString = EntityUtils.toString(((HttpEntityEnclosingRequest)req).getEntity());
//			JsonParser parser = new JsonParser();
//			JsonObject jsonReq = parser.parse(jsonReqAsString).getAsJsonObject();
//			Set<Entry<String, JsonElement>> jsonReqAsSet = jsonReq.entrySet();
//			if (jsonReqAsSet.size() == 1) {
//				for (Entry<String, JsonElement> obj : jsonReqAsSet) {
//					String key = obj.getKey();
//					if (key.equals("login") || key.equals("createAccount")) {
//						return false;
//					}
//				}
//			}
//		}
//		return true;
//	}
}
