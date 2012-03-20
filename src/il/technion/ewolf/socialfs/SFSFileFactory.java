package il.technion.ewolf.socialfs;

import il.technion.ewolf.kbr.KeyFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class SFSFileFactory {

	private final Provider<SFSFile> sFSFileProvider;
	private final int nrSubFiles;
	private final KeyFactory keyFactory;
	
	private Credentials cred;
	
	@Inject
	SFSFileFactory(Provider<SFSFile> sFSFileProvider,
			@Named("socialfs.folders.init.nrsubfolders") int nrSubFiles,
			KeyFactory keyFactory) {
		this.sFSFileProvider = sFSFileProvider;
		this.nrSubFiles = nrSubFiles;
		this.keyFactory = keyFactory;
	}
	
	void login(Credentials cred) {
		this.cred = cred;
	}
	
	public SFSFile createNewFolder() {
		return createNewSFSFile()
				.setNrSubFiles(nrSubFiles, keyFactory);
	}
	
	public SFSFile createNewFile() {
		return createNewSFSFile()
				.setNrSubFiles(0, keyFactory);
	}
	
	public SFSFile createNewSFSFile() {
		SFSFile sfsFile = sFSFileProvider.get();
		
		sfsFile
			.setPubSigKey(cred.getProfile().getPubSigKey())
			.setPrvSigKey(cred.getPrvSigKey());
		
		return sfsFile;
	}
	
}
