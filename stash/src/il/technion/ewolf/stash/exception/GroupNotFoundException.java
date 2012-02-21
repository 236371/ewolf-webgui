package il.technion.ewolf.stash.exception;

public class GroupNotFoundException extends Exception {

	private static final long serialVersionUID = -4379603031961112595L;

	public GroupNotFoundException() {
		super();
	}

	public GroupNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public GroupNotFoundException(String message) {
		super(message);
	}

	public GroupNotFoundException(Throwable cause) {
		super(cause);
	}

	
}
