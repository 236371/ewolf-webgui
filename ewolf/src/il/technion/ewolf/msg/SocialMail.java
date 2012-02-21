package il.technion.ewolf.msg;

import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.stash.crypto.AsymmetricEncryptedObject;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import ch.lambdaj.Lambda;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class SocialMail {

	// dependencies
	private final SocialFS socialFS;
	private final DHT messageDHT;
	private final Provider<AsymmetricEncryptedObject<SocialMessage>> encryptedMessageProvider;
	private final Provider<ContentMessage> contentMessageProvider;
	private final Provider<PokeMessage> pokeMessageProvider;
	
	
	@Inject
	SocialMail(SocialFS socialFS,
			@Named("ewolf.messages.dht") DHT messageDHT,
			Provider<AsymmetricEncryptedObject<SocialMessage>> encryptedMessageProvider,
			Provider<ContentMessage> contentMessageProvider,
			Provider<PokeMessage> pokeMessageProvider) {
		
		this.socialFS = socialFS;
		this.messageDHT = messageDHT;
		this.encryptedMessageProvider = encryptedMessageProvider;
		
		this.contentMessageProvider = contentMessageProvider;
		this.pokeMessageProvider = pokeMessageProvider;
		
	}
	
	
	public ContentMessage createContentMessage() {
		ContentMessage $ = contentMessageProvider.get();
		$.setSender(socialFS.getCredentials().getProfile());
		return $;
	}
	
	public PokeMessage createPokeMessage() {
		PokeMessage $ = pokeMessageProvider.get();
		$.setSender(socialFS.getCredentials().getProfile());
		return $;
	}
	
	public List<SocialMessage> readInbox() {
		List<SocialMessage> $ = new ArrayList<SocialMessage>();
		
		UserID myUID = socialFS.getCredentials().getProfile().getUserId();
		PrivateKey privEncKey = socialFS.getCredentials().getPrvEncKey();
		
		List<Serializable> msgs = messageDHT.get("messages", myUID.getKey().toBase64());
		for (Serializable s : msgs) {
			try {
				@SuppressWarnings("unchecked")
				AsymmetricEncryptedObject<SocialMessage> encryptedMessage = (AsymmetricEncryptedObject<SocialMessage>)s;
				
				SocialMessage msg = encryptedMessage.decrypt(privEncKey);
				$.add(msg.setTransientParams(socialFS));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if ($.isEmpty())
			return $;
		return Lambda.sort($, Lambda.on(SocialMessage.class).getTimestamp());
		
	}
	
	public void send(SocialMessage msg, Profile to) {
		AsymmetricEncryptedObject<SocialMessage> encryptedMessage = encryptedMessageProvider.get()
				.encrypt(msg, to.getPubEncKey());
		messageDHT.put(encryptedMessage, "messages", to.getUserId().getKey().toBase64());
	}
}
