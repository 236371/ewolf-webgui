package il.technion.ewolf.socialfs.exception;

public class CredentialsNotFoundException extends Exception {

	private static final long serialVersionUID = -734493552843634378L;

	public CredentialsNotFoundException() {
		super();
	}

	public CredentialsNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public CredentialsNotFoundException(String message) {
		super(message);
	}

	public CredentialsNotFoundException(Throwable cause) {
		super(cause);
	}

	
}
