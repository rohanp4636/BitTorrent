

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Piece Manager
 * Responsible for managing which pieces have been downloaded or need to be downloaded.
 * Used by Peers to determine which piece they should request or allowed to send.
 * @author Rohan Patel
 *
 */
public class PieceManager implements Serializable, Runnable{

	/**
	 * pieces downloaded
	 */
	private int pieces;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * pieces which have been downloaded
	 */
	private boolean[] pieceList;
	/**
	 * pieces which have been downloaded or are being downloaded
	 */
	transient private boolean[] downloading;
	/**
	 * bitfield
	 */
	private Bitfield bitfield;
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
	
	private transient RandomAccessFile raf;
	
	private  File file;
	
	private transient ArrayList<Peer> peers;
	
	private transient ArrayList<Thread> threads;
	
	public transient static boolean stop;
	/**
	 * instantiates the PieceManager
	 * if first time pieceList is initialized. if not previous pieces will be populated.
	 */
	public PieceManager(int numPieces, Bitfield field, File file){
		 pieceList = new boolean[numPieces];
		 downloading = new boolean[numPieces];
		 bitfield = field;
		 stop = false;
		 this.file = file;
		 try {
			raf = new RandomAccessFile(this.file, "rw");
		} catch (FileNotFoundException e) {
			
		}
		
	}
	
	
	public void setPeers(ArrayList<Peer> peers){
		this.peers = peers;
	}
	
	public void run() {
		// TODO Auto-generated method stub
		try {
			startDownload();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			
			return;
		}
	}
	
	public static synchronized boolean  getSetStop(int i){
		if(stop == true){
			return true;
		}
		if(i == 0){
			return stop;
		}
		else{
			stop = !stop;
			return stop;
		}
	}
	
	public void startDownload(){
		InputStreamReader fileInputStream=new InputStreamReader(System.in);
	    BufferedReader bufferedReader=new BufferedReader(fileInputStream);
		System.out.println("To begin download type anything, or type 'quit' at anytime to stop.");
		String input = "";
		try {
			input = bufferedReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		threads = new ArrayList<Thread>();
		for(Peer p: peers){
			System.out.println("Starting new thread for peer: " + p.peerId);
			Thread t = new Thread(p);
			threads.add(t);
			t.start();
		}
		
		
			boolean term;
			while(term = !getSetStop(0)){
				int count = 0;
				try {
					if(bufferedReader.ready()){
						input = bufferedReader.readLine();
						if(input.compareToIgnoreCase("quit") == 0){
							getSetStop(1);
						}
					}
				} catch (IOException e) {
					
				}
				for(Thread t: threads){
					
					if(t.isAlive()){
						count++;
					}
				}
				if(count == 0 ){
					break;
				}
			}
			if(term){
				System.out.println("Download was stopped by user input.");
			}
		
	}
	
	/**
	 * sets pieceList to represent whats been downloaded. also updates bitfield
	 * and writes to file
	 * @param index
	 */
	public void setPiece(int index, byte[] piece){
		
		synchronized(pieceList){
			pieces++;
			if(index >= 0 && index < pieceList.length){
				pieceList[index] = true;
			}
			setBitField(index);
			//System.out.println("Status... Downloaded "+pieces + "/" + pieceList.length);
			try {
				raf.seek((index*piece.length));
				raf.write(piece);
				raf.seek(0);
				
			} catch (IOException e) {
				
			}
		}
		
		
	}
	
	public int numPiecesDownloaded(){
		return pieces;
	}
	
	public boolean canDownload(int index){
		synchronized(this.downloading){
			if(this.downloading[index]){
				return false;
			}
			else{
				this.downloading[index] = true;
				return true;
			}
			
		}
	}
	
	/**
	 * determines what piece to download next randomly
	 * @return index of piece to download.
	 */
	public int getPiece(){
		ArrayList<Integer> list= new ArrayList<Integer>();
		synchronized(pieceList){
			
			for(int i = 0; i < pieceList.length; i ++){
				if(!pieceList[i]){
					list.add(i);
				}
			}
		}
		Random r = new Random();
		return list.get(r.nextInt(list.size()));
		
		
	}
	
		 
	/**
	 * return bitfield
	 * @return
	 */
	public byte[] getBitField(){
		
		synchronized(this.bitfield){
			return bitfield.getBitfield();
		}
	
	}
	
	/**
	 * updates bitfield values after piece is downloaded
	 * @param index
	 */
	public void setBitField(int index){
		
		synchronized(this.bitfield){
			bitfield.setBitfield(index);
		}
	
	}
	
	public void setTracker(Tracker t){
		t.setDownloaded(this.downloaded);
		t.setUploaded(t.getUploaded());
		t.setLeft(this.left);
	}
	


	/**
	 * sets 
	 * @param uploaded
	 */
	public synchronized void setUploaded(long uploaded){
		this.uploaded = uploaded;
		
	}

	/**
	 * sets downloaded
	 * @param downloaded
	 */
	public synchronized void setDownloaded(long downloaded){
		this.downloaded = downloaded;
		
	}

	/**
	 * sets left
	 * @param left
	 */
	public synchronized void setLeft(long left){
		this.left = left;
		
	}
	
	public void setDown(){
		if(downloading == null){
			downloading = new boolean[pieceList.length]; 
			
		}
		if(raf == null){
			try {
				raf = new RandomAccessFile(this.file, "rw");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(int i = 0; i < pieceList.length;i++){
			
			downloading[i] = pieceList[i];
		}
	}
	
	
	
	
}
