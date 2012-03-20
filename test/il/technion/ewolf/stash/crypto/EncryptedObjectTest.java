package il.technion.ewolf.stash.crypto;


import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Test;

public class EncryptedObjectTest {

	@Test
	public void itShouldEncryptWithSymmetricKey() throws Exception {
		
		KeyGenerator gen = KeyGenerator.getInstance("AES");
		SecretKey secretKey = gen.generateKey();
		
		EncryptedObject<String> encrypted = new EncryptedObject<String>().encrypt("abc", secretKey);
		
		Assert.assertNotSame("abc", new String(encrypted.getBytes()));
		
		String decrypted = encrypted.decrypt(secretKey);
		
		Assert.assertEquals("abc", decrypted);
	}
	
	@Test
	public void itShouldEncryptWithAsymmetricKey() throws Exception {
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048);
		KeyPair keyPair = gen.generateKeyPair();
		
		EncryptedObject<String> encrypted = new EncryptedObject<String>().encrypt("abc", keyPair.getPublic());
		
		Assert.assertNotSame("abc", new String(encrypted.getBytes()));
		
		String decrypted = encrypted.decrypt(keyPair.getPrivate());
		
		Assert.assertEquals("abc", decrypted);
	}
	
	@Test
	public void itShouldEncryptLargeObjectWithAsymmetricKey() throws Exception {
		KeyPairGenerator pairGen = KeyPairGenerator.getInstance("RSA");
		pairGen.initialize(2048);
		KeyPair keyPair = pairGen.generateKeyPair();
		
		KeyGenerator gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		SecretKey secretKey = gen.generateKey();
		
		String obj = "a";
		for (int i=0; i < 20; ++i) {
			obj += obj;
		}
		
		AsymmetricEncryptedObject<String> encrypted = new AsymmetricEncryptedObject<String>(secretKey)
			.encrypt(obj, keyPair.getPublic());
		
		
		String decrypted = encrypted.decrypt(keyPair.getPrivate());
		
		Assert.assertEquals(obj, decrypted);
	}
	
}
