package il.technion.ewolf.socialfs.exception;

public class ProfileNotFoundException extends Exception {

	private static final long serialVersionUID = -734493552843634378L;

	public ProfileNotFoundException() {
		super();
	}

	public ProfileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProfileNotFoundException(String message) {
		super(message);
	}

	public ProfileNotFoundException(Throwable cause) {
		super(cause);
	}

	
}
