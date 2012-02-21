package il.technion.ewolf.stash.exception;

public class EncryptionException extends RuntimeException {

	private static final long serialVersionUID = -3449650452706074922L;

	public EncryptionException() {
	}

	public EncryptionException(String message) {
		super(message);
	}

	public EncryptionException(Throwable cause) {
		super(cause);
	}

	public EncryptionException(String message, Throwable cause) {
		super(message, cause);
	}

}
