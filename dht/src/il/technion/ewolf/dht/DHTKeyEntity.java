package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "DHTKeyEntity")
public class DHTKeyEntity implements Serializable {

	private static final long serialVersionUID = -5119225395520660307L;

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	Long id;
	
	@Column(name="base64Key")
	String base64Key;
	
	DHTKeyEntity() {
		
	}
	
	public DHTKeyEntity(Key key) {
		this.base64Key = key.toBase64();
	}
	
	public String getBase64Key() {
		return base64Key;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setBase64Key(String base64Key) {
		this.base64Key = base64Key;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
}
