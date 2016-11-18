	/**
	 * Block
	 * Class used to represent blocks that make up piece
	 * 
	 * @author Rohan Patel
	 *
	 */	
public class Block {

	/**
	 * block within piece
	 */
	byte[] block;
	
	/**
	 * offset within piece
	 */
	int offset;
	
	/**
	 * creates new block
	 * @param block
	 * @param offset within piece
	 */
	public Block(byte[] block, int offset){
		this.block = block;
		this.offset = offset;
	}
	
	
}
