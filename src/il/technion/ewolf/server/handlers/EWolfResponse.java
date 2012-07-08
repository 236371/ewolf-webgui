package il.technion.ewolf.server.handlers;

public abstract class EWolfResponse {
	public String result;
	static final String RES_SUCCESS = "success";
	static final String RES_NOT_FOUND = "not found";
	static final String RES_BAD_REQUEST = "bad request";
	static final String RES_INTERNAL_SERVER_ERROR = "internal server error";
	static final String RES_GENERIC_ERROR = "error";

	public EWolfResponse(String result) {
		this.result = result;
	}
	
	public EWolfResponse() {
		this.result = RES_SUCCESS;
	}
	
}
