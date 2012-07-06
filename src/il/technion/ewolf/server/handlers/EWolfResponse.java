package il.technion.ewolf.server.handlers;

public class EWolfResponse {
	public String result;

	public EWolfResponse(String result) {
		super();
		this.result = result;
	}
	
	public EWolfResponse() {
		super();
		this.result = "success";
	}
	
}
