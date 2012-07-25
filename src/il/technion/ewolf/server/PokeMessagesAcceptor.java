package il.technion.ewolf.server;

import java.util.List;

import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;

import com.google.inject.Inject;

public class PokeMessagesAcceptor implements Runnable {
	private final SocialMail smail;

	@Inject
	public PokeMessagesAcceptor(SocialMail smail) {
		this.smail = smail;
	}

	@Override
	public void run() {
		try {
			while (true) {
				List<SocialMessage> messages = smail.readInbox();
				for (SocialMessage m : messages) {
					if (m.getClass() == PokeMessage.class) {
						((PokeMessage)m).accept();
						continue;
					}
				}
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			//TODO what can I do here?
		}

	}

}
