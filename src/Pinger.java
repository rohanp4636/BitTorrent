import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Pinger
 * Utility class that takes in a list of peers. Pings all of 
 * them to determing lowest RTT and returns that peer
 *  
 * @author Rohan Patel
 *
 */	
public class Pinger {
	
	/**
	 * Determines peer with fastest RTT
	 * @param peer list
	 * @return peer
	 */
	public static Peer getLowestRTT(ArrayList<Peer> peers){
		
		long avgTime = Long.MAX_VALUE;
		Peer fastPeer = null;
		
		for(Peer p : peers){
			System.out.println("Pinging PeerID: " + p.peerId + " IP: " + p.peerIp);
			long timeForAttempts = 0;
			int attempts = 0;
			for(int i = 0; i < 10; i ++){
				try{
					InetAddress peerPing = InetAddress.getByName(p.peerIp);
					long sentPing = System.nanoTime();
					if(peerPing.isReachable(4000)){
						attempts++;
						long finishPingTime = System.nanoTime();
						System.out.println("Ping attempt: " + (i+1) + " ID: " + p.peerId + " IP: " + p.peerIp + " Completed in " + (finishPingTime-sentPing) + " nanoseconds" );
						timeForAttempts += finishPingTime-sentPing;
					}
				
				}
				catch(UnknownHostException e){
					System.err.println("Ping attempt: " + (i+1) +". Can not ping ID: " + p.peerId + " IP: " + p.peerIp);
				}
				catch(IOException e){
					System.err.println("Ping attempt: " + (i+1) +". Timed out for ID: " + p.peerId + " IP: " + p.peerIp);
				}
			
			}
			if(attempts == 0){
				System.err.println("All Ping attempts failed for ID: " + p.peerId + " IP: " + p.peerIp);
			}
			else{
				long time = timeForAttempts/attempts;
				System.out.println("Average RTT: " + time + " nanoseconds");
				System.out.println("-----------------------------------------");
				if(time < avgTime){
					avgTime = time;
					fastPeer = p;
				}
			}
			
			
		}
		
		if(fastPeer == null){
			System.err.println("All Ping attempts failed for all peers failed.");
		}
		else{
			System.out.println("\nID: " + fastPeer.peerId + " IP: " + fastPeer.peerIp + " was selected with a RTT of: " + avgTime + " nanoseconds\n");
		}
		return fastPeer;
		
	}

	
	
}
