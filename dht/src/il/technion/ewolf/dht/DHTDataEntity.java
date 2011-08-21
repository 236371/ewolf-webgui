package il.technion.ewolf.dht;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="DHTDataEntity")
public class DHTDataEntity implements Serializable {

	private static final long serialVersionUID = -8875750465430087840L;

	@ManyToMany
	private Set<DHTKeyEntity> keys = new HashSet<DHTKeyEntity>();
	
	@Column(name="data")
	private String data;
	
	@Column(name="lastInserted")
	private long lastInserted;
	
	@Id
	@GeneratedValue
	private Long id;

	
	public DHTDataEntity() {
		
	}
	
	public DHTDataEntity(String data) {
		this.data = data;
	}
	
	public Set<DHTKeyEntity> getKeys() {
		return keys;
	}

	public void setKeys(Set<DHTKeyEntity> keys) {
		this.keys = keys;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public long getLastInserted() {
		return lastInserted;
	}

	public void setLastInserted(long lastInserted) {
		this.lastInserted = lastInserted;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	
	
}
