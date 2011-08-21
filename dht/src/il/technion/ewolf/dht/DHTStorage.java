package il.technion.ewolf.dht;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import il.technion.ewolf.kbr.Key;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;

import com.google.inject.Inject;

class DHTStorage {

	private final SessionFactory sessionFactory;
	
	private final long maxLifetime;
	private final int maxEntrySize;
	private final int maxEntries;
	
	@Inject
	DHTStorage(
			@Named("dht.storage.maxlifetime") long maxLifetime,
			@Named("dht.storage.maxentrysize") int maxEntrySize,
			@Named("dht.storage.maxentries") int maxEntries,
			SessionFactory sessionFactory) {
		
		this.sessionFactory = new Configuration().configure(new File("hibernate.cfg.xml")).buildSessionFactory();
		this.maxLifetime = maxLifetime;
		this.maxEntrySize = maxEntrySize;
		this.maxEntries = maxEntries;
			
	}
	
	public synchronized void store(Key key, Collection<String> vals) {
		Session session = sessionFactory.openSession();
		
		session.beginTransaction();
		try {
			@SuppressWarnings("unchecked")
			List<DHTKeyEntity> keys = session.createQuery("from DHTKeyEntity where base64Key = :key")
				.setParameter("key", key.toBase64())
				.list();
			
			DHTKeyEntity keyEntity = keys.isEmpty() ? new DHTKeyEntity(key) : keys.get(0);
			session.save(keyEntity);
			
			for (String data : vals) {
				if (vals.size() > maxEntrySize)
					continue;
				
				@SuppressWarnings("unchecked")
				List<DHTDataEntity> d = session.createQuery("from DHTDataEntity where data = :data")
					.setParameter("data", data)
					.list();
				
				DHTDataEntity dataEntity = d.isEmpty() ? new DHTDataEntity(data) : d.get(0);
				
				dataEntity.setLastInserted(System.currentTimeMillis());
				dataEntity.getKeys().add(keyEntity);
				
				session.save(dataEntity);
			}
			
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		} finally {
			session.close();
		}
	}
	
	public synchronized Collection<String> get(Key key) {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		try {
			@SuppressWarnings("unchecked")
			List<DHTDataEntity> data = session.createQuery(
					"select d from DHTDataEntity d " +
					"join d.keys k " +
					"where k.base64Key = :key")
					.setParameter("key", key.toBase64())
				.list();
	
			if (data.isEmpty())
				return Collections.emptyList();
			
			return extract(data, on(DHTDataEntity.class).getData());
			
		} catch (Exception e) {
			e.printStackTrace();
			session.getTransaction().rollback();
			return Collections.emptyList();
		} finally {
			session.close();
		}
		
	}
	
	
	
}
