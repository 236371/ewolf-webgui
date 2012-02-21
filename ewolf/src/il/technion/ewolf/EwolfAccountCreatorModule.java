package il.technion.ewolf;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class EwolfAccountCreatorModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("ewolf.fs.social_groups.path", "/");
		defaultProps.setProperty("ewolf.fs.social_groups.name", "social-groups");
		
		defaultProps.setProperty("ewolf.fs.wall.path", "/");
		defaultProps.setProperty("ewolf.fs.wall.name", "wall");
		
		return defaultProps;
	}
	
	public EwolfAccountCreatorModule() {
		this(new Properties());
	}
	
	public EwolfAccountCreatorModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public EwolfAccountCreatorModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		
		bind(EwolfAccountCreator.class).in(Scopes.SINGLETON);
	}

}
