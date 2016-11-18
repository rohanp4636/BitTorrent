

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
public class PieceManager implements Serializable{

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
	
	private transient File file;
	/**
	 * instantiates the PieceManager
	 * if first time pieceList is initialized. if not previous pieces will be populated.
	 */
	public PieceManager(int numPieces, Bitfield field, File file){
		 pieceList = new boolean[numPieces];
		 downloading = new boolean[numPieces];
		 bitfield = field;
		 this.file = file;
		 try {
			raf = new RandomAccessFile(this.file, "rw");
		} catch (FileNotFoundException e) {
			
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
				raf.seek(0);;
			} catch (IOException e) {
				
			}
		}
		
		
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
		for(int i = 0; i < pieceList.length;i++){
			downloading[i] = pieceList[i];
		}
	}
	
	
	
	
}
