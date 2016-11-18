import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import GivenTools.*;


/**
 * RUBTClient
 * Main program. Starts tracker and download from peer
 *  
 * @author Rohan Patel
 *
 */	

public class RUBTClient {

	/**
	 * Client peer id
	 */
	public static String peerId;
	/**
	 * file to save contents to
	 */
	public static String saveFile;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String torrent = "";
		peerId = generatePeerId();
		
		
		//Check if user gave a paramter else print error
		if(args.length == 2 && (args[0] != null || args[1] != null)){
			torrent = args[0];
			saveFile = args[1];
		}
		else{
			System.err.println("RUBTClient takes 2 parameters. RUBTClient somefile.torrent saveFile.extension");
			return;
		}
		
		//Checks if the given torrent file is valid
		File torrentFile = new File(torrent);
		if(!torrentFile.exists()){
			System.err.println("Torrent file does not exist");
			return;
		}
		
		//Converts the file to a byte array
		byte[] buffer = new byte[(int) torrentFile.length()];
		try{
			FileInputStream fis = new FileInputStream(torrentFile);
			fis.read(buffer);
			fis.close();
		}
		catch(Exception e){
			System.err.println("Could not convert file to byte array");
			return;
		}
		
		//torrentInfo is used to gather information from the torrent file.
		TorrentInfo torrentInfo = null;
		try {
			torrentInfo = new TorrentInfo(buffer);
		} catch (BencodingException e) {
			// TODO Auto-generated catch block
			System.err.println("Could not get info from torrent file");
			return;
		}
		
		
		//Create tracker, get tracker response, decode and save peer RUBT11/RU11;
		Tracker tracker = new Tracker(torrentInfo, peerId);
		ArrayList<Peer> peers = new ArrayList<Peer>();
		Peer peer = null;
		try{
			byte[] response= tracker.getTrackerResponse();
			
			//decode response from tracker. Check if failed. 
			@SuppressWarnings("unchecked")
			HashMap<ByteBuffer,Object> responseDictionary = (HashMap<ByteBuffer,Object>) Bencoder2.decode(response);
			Tracker.checkFailure(responseDictionary);
			
			//set interval for tracker
			tracker.getIntervalPeer(responseDictionary);
			
			File file = new File(RUBTClient.saveFile);
			if(!file.exists()){
				file.createNewFile();
			}
			
			//create PieceManager
			PieceManager pm = readBitfield(torrentInfo.piece_hashes.length, file);
			
			ArrayList<Peer> peerList = tracker.getPeers(responseDictionary, pm);
			System.out.println("Message from tracker:");
			ToolKit.print(responseDictionary);
			System.out.println();
			if(peerList.isEmpty()){
				System.err.println("No peers returned from tracker.");
				return;
			}
			
			//use all peers to download
			for(Peer p: peerList){
				//only connect to specified peers
				//if(p.peerId.contains("-RUBT11") || p.peerId.contains("-RU11")){
					System.out.println("Possible RU peer: " + p.peerId);
					peers.add(p);
					
				//}
			}
			System.out.println();
			/*
			if(peers.size() != 3){
				System.err.println("There should be 3 peers but tracker did not supply them all.");
				return;
			}
			
			
			//calculate RTT for each peer;
			peer = Pinger.getLowestRTT(peers);
			if(peer == null){
				System.err.println("Could not determine lowest RTT.");
				return;
			}
			*/
		
			
		
			
			//Periodically contact tracker
			tracker.trackerTimer = new Timer(true);
			tracker.trackerTimer.schedule(new TrackerTask(tracker), (tracker.getInterval() * 1000), (tracker.getInterval() * 1000));
			
			//Connect to peers and start download
			//1 thread per peer
			
			ArrayList<Thread> threads = new ArrayList<Thread>();
			for(Peer p: peers){
				System.out.println("Starting new thread for peer: " + p.peerId);
				Thread t = new Thread(p);
				threads.add(t);
				t.start();
			}
			long stopTime = 0;
			long startTime = System.nanoTime();
			try{
				for(Thread t: threads){
					t.join();
				}
			}
			catch(InterruptedException e){
				
			}
			
			stopTime = System.nanoTime();
			System.out.println("Downloaded Completed in " + (stopTime - startTime) + " nanoseconds");
			//cancel tracker contact timer
			if(tracker.trackerTimer != null){
				tracker.trackerTimer.cancel();
				tracker.trackerTimer.purge();
			}
			//send completed and stopped messages
			tracker.send("completed");
			
			System.out.println(RUBTClient.saveFile + " has been successfully downloaded!");
			tracker.send("stopped");
		}
		catch(BencodingException e){
			System.err.println("Bencoding exception while decoding the reponse.");
			return;
				
		}
		catch(Exception e){
			if(e.getMessage()==null){
				System.err.println("Error. Will now exit");
			}
			else{
				System.err.println(e.getMessage());
			}
			return;
		}
		
		
	}
	
	/**
	 * Escape url sequence
	 * @param sequence to escape
	 * @return escaped string
	 */
	public static String escapeString(byte[] value){
		String[] hexString = new String[value.length];
		String result = "";
		int i = 0;
		for(byte b : value) {
			hexString[i] = String.format("%02x", b);
			int decimalChar = Integer.parseInt(hexString[i], 16);
			if( (decimalChar >=48 && decimalChar <=57) || (decimalChar >=65 && decimalChar <=90) || (decimalChar >=97 && decimalChar <=122 || decimalChar == '\'')
					|| decimalChar == '!' || decimalChar == '.' || decimalChar == '-' || decimalChar == '_' || decimalChar == '(' || decimalChar == ')' || decimalChar == '*'){
					
					result += (char) b;
			}
			else{
				result += "%" + hexString[i];
			}
			i++;
	    }
		
		return result;	
	}
	
	/**
	 * Client peerId. Randomly generated on start
	 * @return clientId
	 */
	public static String generatePeerId(){
		String id = "-RJ4636-";
		String values = "0123456789abcdefghijklmnopqrstuvwxyz";
		
		Random r = new Random();
		for(int i = 0; i < 12; i++){
			id+=values.charAt(r.nextInt(values.length()));
		}
		return id;
		
	}
	
	public static PieceManager readBitfield(int length, File saveFile) {
		try{
			File file = new File("bitfield.dat");
			if(!file.exists()){
				return new PieceManager(length, new Bitfield(length), saveFile);
			}
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream("bitfield.dat"));
			PieceManager pm = (PieceManager) ois.readObject();
			pm.setDown();
			ois.close();
			return pm;
		}
		catch(Exception e){
			return new PieceManager(length, new Bitfield(length), saveFile);
		}
	}
	
	public static void writeAdmin(PieceManager pm) {
		try{
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream("bitfield.dat"));
			oos.writeObject(pm);
			oos.close();
		}
		catch(Exception e){
			
		}
		
	}

}
