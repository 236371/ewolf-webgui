package il.technion.ewolf.stash.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;

public class SignatureOutputStream extends OutputStream {

	private final Signature sig;
	
	public SignatureOutputStream(Signature sig) {
		this.sig = sig;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		try {
			sig.update(b, off, len);
		} catch (SignatureException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}
	
	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte)b});
	}

}
