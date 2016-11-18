import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Bitfield represents what has been downloaded. can be serialized.
 * @author Rohan Patel
 *
 */
public class Bitfield implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private byte[] bitfield;
	
	public Bitfield(int length){
		length = (int) (Math.ceil(length/8.0));
		bitfield = new byte[length + 5];
		byte[] head = ByteBuffer.allocate(5).putInt(1+length).array();
		head[4] = (byte) 5;
		
		System.arraycopy(head, 0, bitfield, 0, 5);
		

	}
	
	public Bitfield(byte[] arr){
		bitfield = arr;
		

	}
	
	public synchronized byte[] getBitfield(){
		return bitfield;
	}
	
	public synchronized void setBitfield(int index){
		byte b = bitfield[(int) ((index/8.0)+5)];
		
		byte pos = 1;
		int x = (8- ((index%8) +1));
		byte shift = (byte) (1 << x);
		bitfield[(int) ((index/8.0)+5)] = (byte) (b | shift);
		
	}
	
	public synchronized void setBitFieldArr(byte[] arr){
		this.bitfield = arr;
	}
	
	public synchronized boolean hasIndex(int index){
		byte b = bitfield[(int) ((index/8.0)+5)];
		byte pos = 1;
		int x = (8- ((index%8) +1));
		byte shift = (byte) (1 << x);
		
		if(((byte) (b & shift)) == shift){
			return true;
		}
		else{
			return false;
		}
	}
	
}
