import java.io.IOException;
import java.nio.ByteBuffer;
/**
 * Message
 * Class used to send messages to peer
 * 
 * @author Rohan Patel
 *
 */	
public class Message {
	
	/**
	 * type of message
	 */
	String type;
	
	/**
	 * peer to send message to
	 */
	Peer peer;
	
	/**
	 * choke message
	 */
	byte[] choke;
	
	/**
	 * unChoke message
	 */
	byte[] unChoke;
	
	/**
	 * interested message
	 */
	byte[] interested;
	
	/**
	 * unInterested message
	 */
	byte[] unInterested;
		
	/**
	 * create new Messenger
	 * @param Peer
	 */
	public Message(Peer p){
		this.peer = p;
		this.choke = ByteBuffer.allocate(5).putInt(1).array();
		this.choke[4] = (byte) 0;
		this.unChoke = ByteBuffer.allocate(5).putInt(1).array();
		this.unChoke[4] = (byte) 1;
		this.interested = ByteBuffer.allocate(5).putInt(1).array();
		this.interested[4] = (byte) 2;
		this.unInterested = ByteBuffer.allocate(5).putInt(1).array();
		this.unInterested[4] = (byte) 3;
	}
	
	/**
	 * send choke message
	 * @throws IOException
	 */
	void sendChoke() throws IOException{		 
		try {
			this.peer.writeOutSocket(this.choke);
		} catch (IOException e) {
			throw new IOException();
		}

	}
	
	/**
	 * send unChoke message
	 * @throws IOException
	 */
	void sendUnchoke() throws IOException{
		try {
			this.peer.writeOutSocket(this.unChoke);
		} catch (IOException e) {
			throw new IOException();
		}
	}
	
	/**
	 * send interested message
	 * @throws IOException
	 */
	void sendInterested() throws IOException{
		try {
			this.peer.writeOutSocket(this.interested);
		} catch (IOException e) {
			throw new IOException();
		}
	}
	
	/**
	 * send unInterested message
	 * @throws IOException
	 */
	void sendUnInterested() throws IOException{
		try {
			this.peer.writeOutSocket(this.unInterested);
		} catch (IOException e) {
			throw new IOException();
		}
	}
	
	/**
	 * send piece request to peer
	 * @param requestedIndex
	 * @param piece
	 * @throws IOException
	 */
	void sendPieceRequest(int requestedIndex, Piece piece) throws IOException{
		
		byte[] arr = new byte[17];
		byte[] head = ByteBuffer.allocate(5).putInt(13).array();
		head[4] = (byte) 6;
		System.arraycopy(head, 0, arr, 0, 5);
		
		byte[] index = ByteBuffer.allocate(4).putInt(requestedIndex).array();
		System.arraycopy(index, 0, arr, 5, 4);
		byte[] begin = ByteBuffer.allocate(4).putInt(piece.getByteOffset()).array();
		System.arraycopy(begin, 0, arr, 9, 4);
		byte[] lengh = ByteBuffer.allocate(4).putInt(piece.getNextBlockLength()).array();
		System.arraycopy(lengh, 0, arr, 13, 4);	
		
		try {
			this.peer.writeOutSocket(arr);
		} catch (IOException e) {
			throw new IOException();
		}
	}
	
	/**
	 * send have message to acknowledge receipt of piece
	 * @param haveIndex
	 * @throws IOException
	 */
	void sendHave(int haveIndex) throws IOException{
		byte[] arr = new byte[9];
		byte[] head = ByteBuffer.allocate(5).putInt(5).array();
		head[4] = (byte) 4;
		System.arraycopy(head, 0, arr, 0, 5);
		
		byte[] index = ByteBuffer.allocate(4).putInt(haveIndex).array();
		System.arraycopy(index, 0, arr, 5, 4);
		
		try {
			this.peer.writeOutSocket(arr);
		} catch (IOException e) {
			throw new IOException();
		}
	}
	/**
	 * Sends initial bitfield to peer
	 * @throws IOException
	 */
	void sendBitfield(PieceManager pm) throws IOException{
				
		try {
			this.peer.writeOutSocket(pm.getBitField());
		} catch (IOException e) {
			throw new IOException();
		}
		
	}
	
	
	
	
	


}
