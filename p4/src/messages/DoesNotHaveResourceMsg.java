package messages;

public class DoesNotHaveResourceMsg {
	private Object originalMsg;
	
	public DoesNotHaveResourceMsg(Object originalMsg) {
		this.originalMsg = originalMsg;
	}
	
	public Object getOriginalRequest() {
		return originalMsg;
	}
}
