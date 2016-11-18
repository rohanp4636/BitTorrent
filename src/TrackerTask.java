import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimerTask;

/**
 * TrackerTask
 * Sends updates to tracker
 * 
 * @author Rohan Patel
 *
 */	
public class TrackerTask extends TimerTask{
	
	Tracker tracker;

	
	/**
	 * create message
	 * @param peer
	 */
	public TrackerTask(Tracker tr){
		this.tracker = tr;
		
	}
	
	
	
	/**
	 * send message to peer 
	 */
	@Override
	public void run(){
		tracker.contactTracker();
		
	}
	
}
