package il.technion.ewolf.kbr.openkad.cache;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ColorLRUKadCache extends LRUKadCache {

	private int nrColors;
	private int myColor;

	@Inject
	ColorLRUKadCache(
			@Named("openkad.cache.bucket.size") int size,
			@Named("openkad.bucket.colors.nrcolors") int nrColors,
			@Named("openkad.local.color") int myColor) {
		super(size);
		this.nrColors = nrColors;
		this.myColor = myColor;
	}

	
	@Override
	public void insert(Key key, List<Node> nodes) {
		if (key.getColor(nrColors) != myColor)
			return;
		super.insert(key, nodes);
	}
}
