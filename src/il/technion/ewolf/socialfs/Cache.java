package il.technion.ewolf.socialfs;

import il.technion.ewolf.kbr.Key;


public interface Cache<T extends KeyHolder> {

	void insert(T f);
	
	T search(Key fileKey);
	
}
