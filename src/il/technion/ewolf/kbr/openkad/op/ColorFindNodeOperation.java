package il.technion.ewolf.kbr.openkad.op;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;

import java.util.List;

public class ColorFindNodeOperation extends FindValueOperation implements CompletionHandler<KadMessage, Node> {

	protected ColorFindNodeOperation(int kBucketSize) {
		super(kBucketSize);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Node> call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void completed(KadMessage msg, Node attachment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void failed(Throwable exc, Node attachment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getNrQueried() {
		// TODO Auto-generated method stub
		return 0;
	}

	// TODO: implement
}
