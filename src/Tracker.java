import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import GivenTools.Bencoder2;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

/**
 * Tracker
 * Used to get peers through http get request
 * @author Rohan Patel
 *
 */	

public class Tracker {

	/**
     * Key used to retrieve the list of peers from tracker response.
     */
    public final static ByteBuffer PEERS = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', 's' });
    
	/**
     * Key used to retrieve the port from list of peers from tracker response.
     */
    public final static ByteBuffer PORT = ByteBuffer.wrap(new byte[]
    { 'p', 'o', 'r', 't' });
    
	/**
     * Key used to retrieve the peer id from list of peers from tracker response.
     */
    public final static ByteBuffer PEER_ID = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', ' ', 'i', 'd'});
    
	/**
     * Key used to retrieve the IP from list of peers from tracker response.
     */
    public final static ByteBuffer IP = ByteBuffer.wrap(new byte[]
    { 'i', 'p' });
    
	/**
     * Key used to retrieve the interval from tracker response.
     */
    public final static ByteBuffer INTERVAL = ByteBuffer.wrap(new byte[]
    { 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });
    
	/**
     * Key used to retrieve the failure reason from tracker response.
     */
    public final static ByteBuffer FAILURE = ByteBuffer.wrap(new byte[]
    { 'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ' , 'r', 'e', 'a', 's', 'o', 'n'});
    
    
    /**
     * port used to connect to tracker
     */
	int port;
	
	/**
	 * url request to send to tracker
	 */
	String requestURL;
	
	/**
	 * amount uploaded to peer
	 */
	long uploaded;
	
	/**
	 * amount downloaded from peer
	 */
	long downloaded;
	
	/**
	 * amount left to download from peer
	 */
	long left;
	
	/**
	 * peerId of peer
	 */
	String peerId;
	
	/**
	 * Information about torrent. Example: piece length, file length ...
	 */
	TorrentInfo torrentInfo;
	
	/**
	 * interval to wait to contact tracerk
	 */
	int trackerInterval;
	
	/**
	 * escaped hash sequence
	 */
	String escapedHash;
	
	/**
	 * Total number of pieces that need to be downloaded
	 */
	long numPieces;
	
	/**
	 * timer used to contact tracker periodically
	 */
	Timer trackerTimer;
	
	/**
	 * sets all fields
	 * @param torrentInfo
	 * @param clientID
	 */
	public Tracker(TorrentInfo torrentInfo, String id){
		this.torrentInfo = torrentInfo;
		this.requestURL = torrentInfo.announce_url.toString();
		this.peerId = id;
		this.port = 6881;
		this.uploaded = 0;
		this.downloaded = 0;
		this.left = torrentInfo.file_length;
		this.trackerInterval = 0;
		this.numPieces = (long) Math.ceil(torrentInfo.file_length/((double)torrentInfo.piece_length));
	}
	
	
	/**
	 * returns tracker response which includes list of peers
	 * @return tracker response in byte array
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws Exception
	 */
	public byte[] getTrackerResponse() throws MalformedURLException, IOException, Exception  {
		Boolean connected = false;
		byte[] response = null;
		//Connect to MULTIPLE PORTS 6881 to 6889 if one fails
		for(port = 6881; !connected && port <= 6889; port ++){
			this.escapedHash = RUBTClient.escapeString(torrentInfo.info_hash.array());
			String httpRequest = requestURL +"?info_hash="+ this.escapedHash
											+"&peer_id=" + this.peerId
											+"&port=" + this.port
											+"&uploaded=" + this.uploaded
											+"&downloaded=" + this.downloaded
											+"&left=" + this.left
											+"&event=" + "started";
			
			System.out.println("Contacting tracker : " + httpRequest);
			
			try{
				URL url = new URL(httpRequest);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();	
				urlConnection.setRequestMethod("GET");
				
				//Get tracker response
				InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				byte[] byteArray = new byte[1024];
				int numRead = 0;
				while(0 < (numRead = stream.read(byteArray))){
					byteStream.write(byteArray, 0, numRead);
				}
				
				//connected and received response
				connected = true;
				response = byteStream.toByteArray();
				byteStream.close();
				stream.close();
				urlConnection.disconnect();
				
				
			}
			
			catch(MalformedURLException e){
				throw new MalformedURLException("MalformedURLException while communicated with tracker.");
				
				
			}
			catch(IOException e){
				if(port != 6889){
					System.out.println("IOException while communicating with tracker. Trying another port");
				}
				
			}	
			catch(Exception e){
				
				throw new Exception("Error while communcating with tracker");
				
			}
		}
		if(port == 6890){
			throw new IOException("Could not communicated with tracker through ports 6881 - 6889");
			
		}
		return response;
	}
	
	/**
	 * send complete message to tracker when download is complete
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws Exception
	 */
	public void send(String message) throws MalformedURLException, IOException, Exception {
		Boolean connected = false;
		byte[] response = null;
		//CHANGE TO USE MULTIPLE PORTS 6881 to 6889 if one fails
		for(port = 6881; !connected && port <= 6889; port ++){
			this.escapedHash = RUBTClient.escapeString(torrentInfo.info_hash.array());
			String httpRequest = requestURL +"?info_hash="+ this.escapedHash
											+"&peer_id=" + this.peerId
											+"&port=" + this.port
											+"&uploaded=" + getUploaded()
											+"&downloaded=" + getDownloaded()
											+"&left=" + getLeft();
			if(!message.isEmpty()){
				httpRequest += "&event=" + message;
			}
											
			
			System.out.println("Contacting tracker : " + httpRequest);
			try{
				//send request to tracker - complete
				URL url = new URL(httpRequest);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();	
				urlConnection.setRequestMethod("GET");
				InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				byte[] byteArray = new byte[1024];
				int numRead = 0;
				while(0 < (numRead = stream.read(byteArray))){
					byteStream.write(byteArray, 0, numRead);
				}
				
				response = byteStream.toByteArray();
				//close all connections
				byteStream.close();
				stream.close();
				urlConnection.disconnect();
				@SuppressWarnings("unchecked")
				HashMap<ByteBuffer,Object> responseDictionary = (HashMap<ByteBuffer,Object>) Bencoder2.decode(response);
				Tracker.checkFailure(responseDictionary);
				getIntervalPeer(responseDictionary);
				trackerTimer.cancel();
				trackerTimer.purge();
				trackerTimer = new Timer(true);
				trackerTimer.schedule(new TrackerTask(this), (getInterval() * 1000), (getInterval() * 1000));
				connected = true;

				return;
			}
			catch(MalformedURLException e){
				throw new MalformedURLException("MalformedURLException while sending message to tracker.");
				
				
			}
			catch(IOException e){
				if(port != 6889){
					System.out.println("IOException while sending message to tracker. Trying another port");
				}
				
			}	
			catch(Exception e){
				e.printStackTrace();
				throw new Exception("Error while communcating with tracker");
				
			}
		}
		if(port == 6890){
			throw new IOException("Could not send message to tracker through ports 6881 - 6889");
			
		}
	}
	
	

	/**
	 * get tracker interval
	 * @param responseDictionary
	 */
	public void getIntervalPeer(HashMap<ByteBuffer,Object> responseDictionary){
		if(responseDictionary.containsKey(INTERVAL)){
			int interval = ((Integer) responseDictionary.get(INTERVAL)).intValue();
			if(interval > 180){
				interval = 180;
			}
			setInterval(interval);
		}
		
		
	}
	
	/**
	 * get list of peers from tracker response
	 * @param responseDictionary
	 * @return list of peers
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Peer> getPeers(HashMap<ByteBuffer,Object> responseDictionary, PieceManager pm){
		ArrayList<HashMap<ByteBuffer,Object>> peerMap = null;
		if(responseDictionary.containsKey(PEERS)){
			peerMap = (ArrayList<HashMap<ByteBuffer,Object>>) responseDictionary.get(PEERS);
		}
		ArrayList<Peer> peerList = new ArrayList<Peer>();
		for(HashMap<ByteBuffer,Object> map: peerMap){
			
			peerList.add(new Peer(new String(((ByteBuffer)map.get(PEER_ID)).array()),
								  new String(((ByteBuffer)map.get(IP)).array()),
								  ((Integer) map.get(PORT)).toString(),
								  this,
								  pm));	
			
		}
		
		return peerList;
		
		
		
	}
	/**
	 * print keys in tracker response
	 * @param responseDictionary
	 */
	public static void printKeys( HashMap<ByteBuffer,Object> responseDictionary){
		for(ByteBuffer key : responseDictionary.keySet()){
			try {
				System.out.println(new String(key.array(), "ASCII"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * check if tracker request failed
	 * @param responseDictionary
	 * @throws Exception
	 */
	public static void checkFailure(HashMap<ByteBuffer,Object> responseDictionary) throws Exception{
		if(responseDictionary.containsKey(FAILURE)){
			throw new Exception(new String(((ByteBuffer)responseDictionary.get(Tracker.FAILURE)).array()));
		}
	}
	
	public synchronized void contactTracker(){
		try {
			System.out.println("Sending updates to tracker.");
			send("");
		} catch (Exception e) {
			System.err.println("Could not send update to tracker");
		}
	}
	/**
	 * gets uploaded
	 * @return uploaded
	 */
	public synchronized long getUploaded(){
		return this.uploaded;
		
	}
	/**
	 * sets 
	 * @param uploaded
	 */
	public synchronized void setUploaded(long uploaded){
		this.uploaded += uploaded;
		
	}
	/**
	 * gets downloaded
	 * @return downloaded
	 */
	public synchronized long getDownloaded(){
		return this.downloaded;
		
	}
	/**
	 * sets downloaded
	 * @param downloaded
	 */
	public synchronized void setDownloaded(long downloaded){
		this.downloaded += downloaded;
		
	}
	/**
	 * gets left
	 * @return left
	 */
	public synchronized long getLeft(){
		return this.left;
		
	}
	/**
	 * sets left
	 * @param left
	 */
	public synchronized void setLeft(long left){
		this.left -= left;
		
	}
	/**
	 * gets interval
	 * @return interval
	 */
	public synchronized int getInterval(){
		return this.trackerInterval;
		
	}
	/**
	 * sets interval
	 * @param trackerInterval
	 */
	public synchronized void setInterval(int trackerInterval){
		this.trackerInterval = trackerInterval;
	}
	
}
