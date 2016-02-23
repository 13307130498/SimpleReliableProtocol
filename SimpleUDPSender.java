import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class SimpleUDPSender {

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 4) {
			System.err.println("Usage: SimpleUDPSender <host name> <port number> <source file> <destination file name>");
			System.exit(-1);
		}

		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		//int num = Integer.parseInt(args[2]);
		String fileName = args[2];
		String destinationFileName = args[3];
		DatagramSocket sk = new DatagramSocket();
		DatagramPacket pkt;
		byte[] data = new byte[1000];
		ByteBuffer b = ByteBuffer.wrap(data);
		File file = new File(fileName);
		FileInputStream input = new FileInputStream(file);
		CRC32 crc = new CRC32();

		while (1)
		{
			b.clear();
			// reserve space for checksum
			b.putLong(0);
			b.putInt(i);
			crc.reset();
			crc.update(data, 8, data.length-8);
			long chksum = crc.getValue();
			b.rewind();
			b.putLong(chksum);

			pkt = new DatagramPacket(data, data.length, addr);
			// Debug output
			//System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(data));
			sk.send(pkt);
		}
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
