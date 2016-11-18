import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimerTask;
/**
 * AliveTask
 * Sends out an alive message to peer every 2 min.
 * 
 * @author Rohan Patel
 *
 */	
public class AliveTask extends TimerTask {
	/**
	 * peer associated with timer
	 */
	Peer peer;
	/**
	 * message to be sent
	 */
	byte[] alive;
	
	/**
	 * create message
	 * @param peer
	 */
	public AliveTask(Peer peer){
		this.peer = peer;
		alive = ByteBuffer.allocate(4).putInt(0).array();
	}
	
	
	
	/**
	 * send message to peer 
	 */
	@Override
	public void run(){
		try {
			System.out.println("Sending Alive Message");
			peer.writeOutSocket(alive);
			
			
		} catch (IOException e) {
			return;
		}
		
	}

}
