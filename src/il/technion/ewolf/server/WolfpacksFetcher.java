package il.technion.ewolf.server;

import java.util.ArrayList;
import java.util.List;

public class WolfpacksFetcher implements JsonDataFetcher {
	
	public class WolfpackData {
		
		public WolfpackData(String title, String key) {
			super();
			this.title = title;
			this.key = key;
		}
		
		public String title;
		public String key;
	}

	@Override
	public Object fetchData(String... parameters) {
		/*!
		 * The parameters should be a pair of worlds:
		 * 	First word:		Title
		 * 	Second word:	key word (tag).
		 */
		List<WolfpacksFetcher.WolfpackData> wolfpacks =
				new ArrayList<WolfpacksFetcher.WolfpackData>();
		
		if(parameters.length % 2 != 0) {
			return null;
		}
		
		for (int i = 0; i < parameters.length; i+=2) {
			wolfpacks.add(new WolfpackData(parameters[i],parameters[i+1]));
		}
		
		return wolfpacks;
	}

}
