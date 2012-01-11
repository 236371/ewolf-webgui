package il.technion.ewolf.kbr;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.mockito.Mockito;

import com.google.inject.AbstractModule;

public class KadNetMockModule extends AbstractModule {

	private KeybasedRouting mockedKbr;
	
	
	public KadNetMockModule() {
		mockedKbr = Mockito.mock(KeybasedRouting.class);
		
		try {
			Mockito.when(mockedKbr.getKeyFactory())
				.thenReturn(new RandomKeyFactory(10, new Random(), "SHA-1"));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void configure() {
		bind(KeybasedRouting.class)
			.toInstance(mockedKbr);
	}
	
	public KeybasedRouting getMockedKbr() {
		return mockedKbr;
	}

}
