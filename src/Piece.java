	/**
	 * Piece
	 * Class used to represent pieces downloaded from peer
	 *  
	 * @author Rohan Patel
	 *
	 */	

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

//use this to store a piece and verify hash. if verified save to file and reuse piece for next
//piece
public class Piece {
	/**
	 * piece index
	 */
	int index;
	
	/**
	 * list of blocks within piece
	 */
	ArrayList<Block> blocks;
	
	/**
	 * length of piece
	 */
	int length;
	
	/**
	 * length of each block
	 */
	int pieceLength;
	
	/**
	 * number of blocks
	 */
	int numPieces;
	
	/**
	 * bytes downloaded for peice
	 */
	int downloaded;
	
	/**
	 * bytes left to download for piece
	 */
	int left;
	
	/**
	 * hash to verify peice
	 */
	ByteBuffer hash;
	
	/**
	 * creates piece object
	 * @param length
	 * @param blockLength
	 * @param index
	 * @param hash
	 */
	public Piece(int length, int pieceLength, int index, ByteBuffer hash){
		if(length < Peer.blockSize){
			this.length = length;
			this.pieceLength = length;
		}
		else{
			this.length = length;
			this.pieceLength = pieceLength;
		}
		blocks = new ArrayList<Block>();
		this.numPieces = 0;
		this.downloaded = 0;
		this.left = length;
		this.index = index;
		this.hash = hash;
		
	}
	
	public Piece(int length, int pieceLength, int index, ByteBuffer hash, int left){
		if(length < Peer.blockSize){
			this.length = length;
			this.pieceLength = length;
		}
		else{
			this.length = length;
			this.pieceLength = pieceLength;
		}
		blocks = new ArrayList<Block>();
		this.numPieces = 0;
		this.downloaded = 0;
		this.left = left;
		this.index = index;
		this.hash = hash;
		
	}
	/**
	 * gets length for block
	 * @return block length
	 */
	public int getNextBlockLength(){
		if(left >= pieceLength){
			return pieceLength;
		}
		else{
			return left;
		}
		
	}
	
	/**
	 * gets block offset within piece
	 * @return block offset
	 */
	public int getByteOffset(){
		return downloaded;
	}
	
	/**
	 * add block to list of blocks
	 * @param block
	 * @param offset
	 * @return true if piece completed else false
	 */
	public Boolean addBlock(byte[] arr, int offset){
		Boolean added = false;
		if(blocks.isEmpty()){
			blocks.add(new Block(arr, offset));
			added = true;
		}
		else{
			Block temp = new Block(arr, offset);
			
			for(int i = 0; i < blocks.size(); i ++){
				//insert before
				if(blocks.get(i).offset > temp.offset){
					blocks.add(i, temp);
					added = true;
					break;
				}
				//insert at the end
				else if( (i == (blocks.size() - 1)) && blocks.get(i).offset < temp.offset){
					blocks.add(temp);
					added = true;
					break;
				}
				//if already present break
				else if(blocks.get(i).offset == temp.offset){
					break;
				}
			}
		}
		
		//update left and downloaded field
		if(added){
			numPieces++;
			downloaded = downloaded + arr.length;
			left = left - arr.length;			
		}
		
		//return true if done. else false
		if(left <= 0 ){
			return true;
		}
		else{
			return false;
		}

		
	}
	
	/**
	 * verify hash for piece
	 * @return completed piece if verified else null
	 */
	public byte[] verifyPiece(){
		
		byte[] fullPiece = new byte[length];
		//merge blocks into single byte array
		for(int i = 0; i < blocks.size(); i ++){
			System.arraycopy(blocks.get(i).block, 0, fullPiece, blocks.get(i).offset, blocks.get(i).block.length);
		}
		
		try {
			//verify piece using sha-1 hash
			MessageDigest messageDig = MessageDigest.getInstance("sha-1");
			byte[] pieceSha1Hash = messageDig.digest(fullPiece);
			if( Arrays.equals(pieceSha1Hash,this.hash.array())){
				return fullPiece;
			}
			else{
				System.err.println("Piece could not be verified. Trying again.");
				return null;
			}
			
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Piece could not be verified. Trying again.");
			return null;
		}

		
	}
}
