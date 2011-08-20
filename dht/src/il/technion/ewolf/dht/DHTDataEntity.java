package il.technion.ewolf.dht;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

public class DHTDataEntity {

	@OneToMany
	@JoinColumn(name="DHTDataEntity_ID")
	private Set<String> keys;
	
	@Column(name="DATA")
	private String data;
	
	@Id
	@GeneratedValue
	private Long id;
	
}
