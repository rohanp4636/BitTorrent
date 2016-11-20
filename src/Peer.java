import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
/**
 * Peer 
 * Used to download and verify pieces from peer
 * Also used to generate file.
 * 
 * @author Rohan Patel
 *
 */	
/**
	 * Peer
	 * 
	 * @author Rohan Patel
	 *
	 */	
public class Peer implements Runnable{
	/**
	 * peer id
	 */
	String peerId;
	
	/**
	 * peer Ip
	 */
	String peerIp;
	
	/**
	 * peer port
	 */
	int port;
	
	/**
	 * tracker
	 */
	Tracker tracker;
	
	/**
	 * stream to get input
	 */
	DataInputStream input;
	
	/**
	 * stream to get output
	 */
	DataOutputStream output;
	
	/**
	 * timer to send alive message to peer
	 */
	Timer aliveTimer;
	
	
	/**
	 * true if client chocked
	 */
	Boolean clientChoked;
	
	/**
	 * true if peer chocked
	 */
	Boolean peerChoked;
	
	/**
	 * true if client interested
	 */
	Boolean clientInterested;
	
	/**
	 * true if peer interested
	 */
	Boolean peerInterested;
	
	/**
	 * used to communicate with peer. sends messages
	 */
	Message messenger;
	
	/**
	 * used to determine which pieces to download
	 */
	PieceManager pieceManager;
	/**
	 * standard block size within piece. last block may be less
	 */
	/**
	 * bitfield for peer
	 */
	Bitfield field;
	

	
	public static int blockSize = 16384;
	

	/**
	 * create new peer
	 * @param id
	 * @param ip
	 * @param port
	 * @param tracker
	 */
	public Peer(String id, String ip, String port, Tracker track, PieceManager pm){
		this.peerId = id;
		this.peerIp = ip;
		this.port = Integer.parseInt(port);
		this.tracker = track;
		input = null;
		output = null;
		peerChoked = true;
		clientChoked = true;
		clientInterested = true;
		peerInterested = false;
		messenger = new Message(this);
		pieceManager = pm;
		
		
	}
	
	/**
	 * connects to peer and downloaded file
	 * @throws IOException
	 * @throws Exception
	 */
	public void connectAndDownload () throws IOException, Exception{
		
		try{
			//set up TCP connection
			Socket socket = new Socket(this.peerIp, this.port);
			input = new DataInputStream((socket.getInputStream()));
			output = new DataOutputStream(socket.getOutputStream());
		
			//send handshake and verify response
			writeOutSocket(createHandShake());
			byte[] peerResponse = new byte[68];
			input.readFully(peerResponse);
			if(!verifyPeerResponseHandshake(peerResponse)){
				output.close();
				input.close();
				socket.close();
				throw new Exception("Peer Handshake could not be verified. Closing Connection.");
			}
			//send bitfield
			messenger.sendBitfield(pieceManager);

			
			
			//send initial Alive;
			writeOutSocket(ByteBuffer.allocate(4).putInt(0).array());
			createTimer();
			
			//download file
			getFileFromPeer();
			
			//stop timer and close all connections and streams
			aliveTimer.cancel();
			output.close();
			input.close();
			socket.close();
		}
		catch(IOException e){
			if(aliveTimer!= null){
				aliveTimer.cancel();
				aliveTimer.purge();
			}
			
			throw new IOException("IOException while communicating with Peer");
		}
		catch(Exception e){
			if(aliveTimer!= null){
				aliveTimer.cancel();
				aliveTimer.purge();
			}
			throw new Exception("Error while communicating with Peer.");
			
		}
		
		
		
	}
	
	private void getFileFromPeer() throws IOException{
	
	
		int askingPieceIndex = 0;
		try{
			
			
			//send interested message to peer to unchoke 
			messenger.sendInterested();
			Piece piece = null;
						
			while (true){
				if(PieceManager.getSetStop(0)){
					return;
				}
				byte[] arr = new byte[4];
				
				this.input.readFully(arr);
				//prefix is length of response
				int prefix = ByteBuffer.wrap(arr).getInt();
				//alive message
				if(prefix == 0){
					//System.out.println("Alive Message");
				}
				//choke,unchoke, interested, uninterested
				else if(prefix == 1){
					byte id = this.input.readByte();
					
					//peer choked
					if(id == 0){
						peerChoked = true;
						messenger.sendInterested();
					}
					//peer unChoked
					else if(id == 1){
						peerChoked = false;
						if(tracker.getLeft() == 0){
							return;
						}
						askingPieceIndex = getPieceIndex();
						if(askingPieceIndex == -1){
							return;
						}
						if(piece == null){
							piece = new Piece(tracker.torrentInfo.piece_length,blockSize, askingPieceIndex, tracker.torrentInfo.piece_hashes[askingPieceIndex]);
						}
						//ask for piece
						
						messenger.sendPieceRequest(askingPieceIndex, piece);
					}
					//peer Interested
					else if(id == 2 ){
						peerInterested = true;
					}
					//peer unInterested
					else if (id == 3){
						peerInterested = false;
					}

				}
				//have message..not used in phase 1
				else if(prefix == 5){
					byte id = this.input.readByte();
					if(id == 4){
						byte[] pieceIndex = new byte[4];
						this.input.readFully(pieceIndex);
					}
				}
				//request message
				else if(prefix == 13){
					byte[] have = new byte[12];
					this.input.readFully(have);
					if(have[0] == 6){
						//get payload from request
						byte[] index = new byte[4];
						System.arraycopy(have, 0, index, 0, 4);
						byte[] begin = new byte[4];
						System.arraycopy(have, 5, begin, 0, 4);
						byte[] lengh = new byte[4];
						System.arraycopy(have, 9, lengh, 0, 4);
					}
					
				}
				//variable length message. piece or bitfield or other
				else{
					//get payload
					byte[] buff = new byte[prefix-1];
					byte id = this.input.readByte();
					this.input.readFully(buff);
					
					//bitfield
					if(id == 5){
						//also verify bitfield.. last bit if 0 for 511 pieces -> 64 bytes last byte is 11111110 since total number of bytes needed is 63.@@@
						byte[] bf = new byte[buff.length+5];
						byte[] head = ByteBuffer.allocate(5).putInt(1+buff.length).array();
						head[4] = (byte) 5;
						
						System.arraycopy(head, 0, bf, 0, 5);
						System.arraycopy(buff, 0, bf, 5, buff.length);
						field = new Bitfield(bf);
						
						//verify bitfield?
					}
					
					//piece message. piece we requested
					else if(id == 7){
						byte[] index = new byte[4];
						System.arraycopy(buff, 0, index, 0, 4);
						byte[] begin = new byte[4];
						System.arraycopy(buff, 4, begin, 0, 4); 
						byte[] block = new byte[buff.length-8];
						
						System.arraycopy(buff, 8, block, 0, block.length);
						
						//will be used later to get piece index
						int pieceIndex = ByteBuffer.wrap(index).getInt();
						int byteOffset = ByteBuffer.wrap(begin).getInt();
						
						//add block to piece. if completed verify and write to file. then request next piece if exists
						
						if(piece.addBlock(block, byteOffset)){
							byte[] fullPiece = piece.verifyPiece();
							if(fullPiece != null){
								//send have message acknowledging receipt
								messenger.sendHave(askingPieceIndex);
								

								//updated values
								tracker.setDownloaded(fullPiece.length);
								tracker.setLeft(fullPiece.length);
								
								if(PieceManager.getSetStop(0)){
									return;
								}
								System.out.println("Downloaded piece: " + askingPieceIndex + " using Peer: " + peerId + " " + peerIp);
								pieceManager.setPiece(askingPieceIndex, fullPiece);
								
								if(tracker.getLeft() == 0){
									return;
								}
								askingPieceIndex = getPieceIndex();
													
								
								//check if completed all pieces
								if(askingPieceIndex == -1){
									return;
									
								}
								//check if last piece. may have smaller piece length
								else if(askingPieceIndex == tracker.torrentInfo.piece_hashes.length-1){
									int len = tracker.torrentInfo.file_length % tracker.torrentInfo.piece_length;
									piece = new Piece(len,blockSize, askingPieceIndex, tracker.torrentInfo.piece_hashes[askingPieceIndex], len);
								}
								//create next piece
								else{
									piece = new Piece(tracker.torrentInfo.piece_length,blockSize, askingPieceIndex, tracker.torrentInfo.piece_hashes[askingPieceIndex]);
								}
								
							}
							else{
								piece = new Piece(tracker.torrentInfo.piece_length,blockSize, askingPieceIndex, tracker.torrentInfo.piece_hashes[askingPieceIndex]);
								
							}
						}
						
						//if unchoked ask for next piece
						if(this.peerChoked == false){						
							messenger.sendPieceRequest(askingPieceIndex, piece);
						}
					}
					//if need to handle cancel.
					else{
						
					}
					
				}			
			}
		}
		catch (IOException e) {
			
			throw new IOException();
		}
		
		
		//return file;
		
		
	}
	
	/**
	 * determines which piece to download next
	 * @return
	 */
	public int getPieceIndex(){
		for(int i = 0; i < tracker.torrentInfo.piece_hashes.length; i++){
			if(field.hasIndex(i) && pieceManager.canDownload(i)){
				return i;
			}
		}
		return -1;
		
		
	}
	
	/**
	 * gets existing timerBool or sets. used in threads.
	 * @param set - if true then set createNewTimerBool to bool and return createTimer, else return createNewTimerBool
	 * @param bool
	 * @return
	 
	public synchronized Boolean setGetNewTimerBool(Boolean set, Boolean bool){
		if(set){
			this.createNewTimerBool = bool;
			return this.createNewTimerBool;
		}
		else{
			return this.createNewTimerBool;			
		}
	}
	
	 create timer to send alive message
	 *
	public void createTimer(){
		if(setGetNewTimerBool(false,false)){
			if(this.aliveTimer != null){
				this.aliveTimer.cancel();
				this.aliveTimer.purge();
			}
		}
		setGetNewTimerBool(true,false);
		this.aliveTimer = new Timer(true);
		//send alive every 1.9 minutes
		this.aliveTimer.schedule(new AliveTask(this), 0, 114000);
		
	}
	
	*/
	
	/**
	 * create timer to send alive message
	 */
	public void createTimer(){
		
		this.aliveTimer = new Timer(true);
		//send alive every 1.9 minutes
		this.aliveTimer.schedule(new AliveTask(this), 114000, 114000);
		
	}
	
	public synchronized void resetTimer(){
		if(this.aliveTimer != null){
			this.aliveTimer.cancel();
			this.aliveTimer.purge();
			this.aliveTimer = new Timer(true);
			this.aliveTimer.schedule(new AliveTask(this), 114000, 114000);
		}
	}
	
	/**
	 * write bytes to peer. used in threads
	 * @param byte array to send
	 * @throws IOException
	 */
	synchronized void writeOutSocket(byte [] arr) throws IOException{
		try {
			resetTimer();
			this.output.write(arr);
			this.output.flush();
		} catch (IOException e) {
			output.flush();
			throw new IOException();
		}
	}

	
	
	/**
	 * verify peer response for correct hash and peerId
	 * @param response
	 * @return true if verified else false
	 */
	private boolean verifyPeerResponseHandshake(byte[] response){
		if(response == null){
			return false;
		}
		if(response.length != 68){
			return false;
		}
		//check hash
		byte[]	hash = tracker.torrentInfo.info_hash.array();
		for(int i = 0; i < hash.length; i ++){
			if(hash[i] != response[28+i]){
				return false;
			}
		}
		//check peerId
		String id = new String(response, 48,20);
		if(!id.equals(this.peerId)){
			return false;
		}
		
		System.out.println("Peer hash has been verified for " + peerId + "  " + peerIp);
			//ToolKit.print(response);
		
		return true;
	}
	
	/**
	 * creates handshake to send to tracker
	 * @return
	 */
	private byte[] createHandShake(){
		//49 + len(BitTorrent Protocol) = 68 
		byte[] hs = new byte[68];
		hs[0] = 19;
		byte[] protocol = ByteBuffer.wrap(new byte[]
			    { 'B','i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' }).array();
		System.arraycopy(protocol, 0, hs, 1, protocol.length);
		hs[20] = 0; hs[21] = 0; hs[22] = 0; hs[23] = 0; hs[24] = 0; hs[25] = 0; hs[26] = 0; hs[27] = 0;
		System.arraycopy(tracker.torrentInfo.info_hash.array(),0,hs,28,20);
		System.arraycopy(RUBTClient.peerId.getBytes(),0,hs,48,20);
	
		return hs;
	}
	
	
	
	
	/**
	 * prints peer info
	 */
	public String toString(){
		return peerId+ "    "+ peerIp+"    " +  port;
	}
	
	
	/**
	 * starts new thread
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			connectAndDownload();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			
			return;
		}
	}
}
