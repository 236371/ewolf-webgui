package il.technion.ewolf.dht.msg;

import java.io.Serializable;
import java.util.Set;

import com.google.inject.Inject;

public class FindValueResponse extends DHTMessage {

	private static final long serialVersionUID = 334499637013726422L;
	
	private Set<Serializable> values;
	
	@Inject
	FindValueResponse() {
	}
	
	public FindValueResponse setValues(Set<Serializable> values) {
		this.values = values;
		return this;
	}
	
	public Set<Serializable> getValues() {
		return values;
	}
	
}
