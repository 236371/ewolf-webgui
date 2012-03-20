package il.technion.ewolf.socialfs.background;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractBackgroundTask extends TimerTask {

	private final Timer timer;
	
	protected AbstractBackgroundTask(Timer timer) {
		this.timer = timer;
	}
	
	protected abstract long getPeriod();
	
	public void register() {
		timer.scheduleAtFixedRate(this, 0, getPeriod());
	}
}
