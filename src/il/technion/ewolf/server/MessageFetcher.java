package il.technion.ewolf.server;

public class MessageFetcher implements JsonDataFetcher {

	@Override
	public Object fetchData(String... parameters) {
		/*!
		 * The parameter should be the key of the message.
		 */
		
		if(parameters.length != 1) {
			return null;
		}		
		
		return getMessage(parameters[0]);
	}
	
	private String getMessage(String key) {
		return new String(key + ": This is the message."+
				"<br> This is an image: " + "<img src='favicon.ico'>");
	}

}
