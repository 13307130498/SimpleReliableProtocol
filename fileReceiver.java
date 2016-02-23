import java.io.File;
import java.io.FileOutputStream;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class fileReceiver {

	static int packetNumber;
	static int[] ackresp = new int [21];
	static byte[][] buffer = new byte[21][];
	static int[] bufferlen = new int[21];
	static File file;
	static FileOutputStream output;
	static int lastPacketNumber;
	public static void main(String[] args) throws Exception 
	{
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		byte[] data = new byte[1000];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);
		CRC32 crc = new CRC32();
		byte[] resp;
		ByteBuffer respb;
		int sequenceNumber;
		int datalength;
		packetNumber = 0;
		lastPacketNumber = -1;
		for(int i = 0; i < 21; i++){
			ackresp[i] = 0;
		}
		while(true)
		{
			data = new byte[1000];
			pkt = new DatagramPacket(data, data.length);
			b = ByteBuffer.wrap(data);
			pkt.setLength(data.length);
			sk.receive(pkt);
			if (pkt.getLength() < 8)
			{
				System.out.println("Pkt too short");
				continue;
			}
			b.rewind();
			long chksum = b.getLong();
			sequenceNumber = b.getInt();
			datalength = b.getInt();
			//test
			System.out.println("receive packet:" + sequenceNumber);
			
			crc.reset();
			crc.update(data, 8, pkt.getLength() - 8);
			// Debug output
			// System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (crc.getValue() != chksum)
			{
				System.out.println("Pkt corrupt");
				/*resp = new byte[100];
				respb = ByteBuffer.wrap(resp);
				respb.putLong(0);
				respb.putInt(sequenceNumber);
				respb.putLong(0);
				crc.reset();
				crc.update(resp, 8, resp.length - 8);
				chksum = crc.getValue();
				respb.rewind();
				respb.putLong(chksum);
				DatagramPacket nck = new DatagramPacket(resp, 0, resp.length,
						pkt.getSocketAddress());
				sk.send(nck);*/
			}
			else
			{
				if(sequenceNumber == -1){
					lastPacketNumber = datalength;
					System.out.println("lastPacketNumber:" + lastPacketNumber + "==ackresp[0] :" + ackresp[0]);
					if(lastPacketNumber != -1 && ackresp[0] > lastPacketNumber){
						System.out.println("lastPacketNumber");
						output.close();
					}
					resp = new byte[100];
					respb = ByteBuffer.wrap(resp);
					respb.putLong(0);
					respb.putInt(sequenceNumber);
					respb.putLong(1);
					crc.reset();
					crc.update(resp, 8, resp.length - 8);
					chksum = crc.getValue();
					respb.rewind();
					respb.putLong(chksum);
					DatagramPacket ack = new DatagramPacket(resp, 0, resp.length,
							pkt.getSocketAddress());
					sk.send(ack);
				}
				else{
					int arrayIndex = sequenceNumber - ackresp[0] + 1;
					if(arrayIndex <= 0){
						resp = new byte[100];
						respb = ByteBuffer.wrap(resp);
						respb.putLong(0);
						respb.putInt(sequenceNumber);
						respb.putLong(1);
						crc.reset();
						crc.update(resp, 8, resp.length - 8);
						chksum = crc.getValue();
						respb.rewind();
						respb.putLong(chksum);
						DatagramPacket ack = new DatagramPacket(resp, 0, resp.length,
								pkt.getSocketAddress());
						sk.send(ack);
					}
					else if(arrayIndex < 21){
						ackresp[arrayIndex] = 1;
						if(sequenceNumber == 0 && ackresp[0] == 0){
							String filename = "";
							int namelen = datalength;
							for(int i = 0; i < namelen; i++){
								filename += b.getChar();
							}
							file = new File(filename);
							file.createNewFile();
							output = new FileOutputStream(filename);
							ackresp[0] = 1;
							ackresp[1] = 0;
						}
						else{
							buffer[arrayIndex] = data.clone();
							bufferlen[arrayIndex] = datalength;
						}
						int i;
						for(i = 1; i < 21; i++){
							if(ackresp[i] == 0){
								break;
							}
							else{
								ackresp[0]++;
							}
						}
						for(int j = 1; j < i; j++){
							output.write(buffer[j], 16, bufferlen[j]);
							System.out.println("write into file." + bufferlen[j]);
						}
						for(int j = i; j < 21; j++){
							ackresp[j - i + 1] = ackresp[j];
							buffer[j - i + 1] = buffer[j];
							bufferlen[j - i + 1] = bufferlen[j];
						}
						while(i > 1){
							ackresp[22 - i] = 0;
							buffer[22 - i] = null;
							bufferlen[22 - i] = -1;
							i--;
						}
						if(lastPacketNumber != -1 && ackresp[0] > lastPacketNumber){
							output.close();
						}

						System.out.println("Pkt " + sequenceNumber);
						resp = new byte[100];
						respb = ByteBuffer.wrap(resp);
						respb.putLong(0);
						respb.putInt(sequenceNumber);
						respb.putLong(1);
						crc.reset();
						crc.update(resp, 8, resp.length - 8);
						chksum = crc.getValue();
						respb.rewind();
						respb.putLong(chksum);
						DatagramPacket ack = new DatagramPacket(resp, 0, resp.length,
								pkt.getSocketAddress());
						sk.send(ack);
					}
				}
			}	
		}
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
