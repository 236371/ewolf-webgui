package il.technion.ewolf.server;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class ServerModule extends AbstractModule {
	private String SERVER_PORT;

	public ServerModule(String port) {
		SERVER_PORT = port;
	}
	
	@Override
	protected void configure() {
		bind(String.class).annotatedWith(Names.named("server.host.name")).toInstance("http://localhost");
		bind(String.class).annotatedWith(Names.named("server.port")).toInstance(SERVER_PORT);
	}

	public String getPort() {
		return SERVER_PORT;
	}

}
