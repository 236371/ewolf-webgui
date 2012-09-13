package il.technion.ewolf.server;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;

public class HttpSessionStore {
	private static final int SESSION_ID_LENGTH = 10;
	Set<String> keys = new HashSet<String>();

	public String createSession() {
		String key = generateKey();
		keys.add(key);
		return key;
	}
	 
	public void deleteSession(String key) {
		keys.remove(key);
	}
	 
	public boolean isValid(String key) {
		return keys.contains(key);
	}

	private String generateKey() {
		String newKey;
		while (true) {
			newKey = RandomStringUtils.randomAlphanumeric(SESSION_ID_LENGTH);
			if (!keys.contains(newKey)) {
				break;
			}
		}
		return newKey;
	}
}
