import java.io.*;
import java.net.*;

//define a TFTPSimManager class;
public class TFTPSimManager  implements Runnable
{
	public static final int MESSAGE_SIZE = 512;
	public static final int BUFFER_SIZE = MESSAGE_SIZE+4;
	public static final byte MAX_BLOCK_NUM = 127;
	public static final byte DATA = 3;
	public static final byte ACK = 4;
	public static final int REQ = 3;
	
	public static final int TIP = 2;
	public static final int PACKET = 1;


	
	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket clientPacket, serverPacket;
	private DatagramSocket clientSocket, serverSocket;
	private boolean exitNext;
	private int clientPort,serverPort;
	
	//Data for error generation
	private int errorType;
	private byte[] comparitorA;
	private byte[] comparitorB;
	private int errorDetail;
	private byte packetType;

	
	public TFTPSimManager( DatagramPacket dp, Error e ) {
	  	// Get a reference to the data inside the received datagram.
	    clientPacket = dp;
	    serverPort = 69;
	    exitNext = false;
	    
	    this.comparitorA = new byte[4];
	    this.comparitorB = new byte[4];
	    this.comparitorA[0] = 0;
	    this.comparitorA[1] = e.getBlockType();
	    this.comparitorA[2] = e.getBlockNumber().getCurrent()[0];
	    this.comparitorA[3] = e.getBlockNumber().getCurrent()[1];
	    this.comparitorB = this.comparitorA;
	    if (this.comparitorA[1] == 3) {
	    	this.comparitorA[1] = 1;
	    	this.comparitorB[1] = 2;
	    } else if (this.comparitorA[1] == 1) {
	    	this.comparitorA[1] = 3;
	    	this.comparitorB[1] = 3;
	    } else if (this.comparitorA[1] == 2) {
	    	this.comparitorA[1] = 4;
	    	this.comparitorB[1] = 4;
	    }
	    this.errorType = e.getErrorType();
	    this.packetType = e.getBlockType();
	    this.errorDetail = e.getErrorDetail();
	    //System.out.println(errorType);
	    //System.out.println("Looking for: "+this.errorDetail);
	    
	}

	
	public void run() {
		try {
			byte temp[];
			//  Construct  sendPacket to be sent to the server (to port 69)
			clientPort = clientPacket.getPort();
			temp = findRequestError(serverPacket = new DatagramPacket(clientPacket.getData(),clientPacket.getLength(),InetAddress.getLocalHost(),serverPort));
			for(int i = 0; i < temp.length; i++) System.out.print(temp[i]);
			System.out.println();
			serverPacket = new DatagramPacket(temp,temp.length,InetAddress.getLocalHost(),serverPort);
			System.out.println("Recieved Packet from client");
			serverSocket = new DatagramSocket();
			serverSocket.send(serverPacket);
			System.out.println("Forwarded packet to server");
			if(checkForEnd(serverPacket.getData()))return;
			
			byte data[] = new byte[BUFFER_SIZE];
			serverPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),serverPort);
			serverSocket.receive(serverPacket);
			serverPort = serverPacket.getPort();
			System.out.println("Recieved packet from server");
			temp = findError(clientPacket = new DatagramPacket (serverPacket.getData(),serverPacket.getLength(),InetAddress.getLocalHost(),clientPort));
			for(int i = 0; i < temp.length; i++) System.out.print(temp[i]);
			System.out.println();
			clientPacket = new DatagramPacket (temp,temp.length,InetAddress.getLocalHost(),clientPort);
			clientSocket = new DatagramSocket();
			clientSocket.send(clientPacket);
			System.out.println("Forwarded packet to client");
			if(checkForEnd(clientPacket.getData()))return;
		
			for(;;) {
				if(clientToServer()) return;
				if(serverToClient()) return;
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		  
	}
  
	private boolean checkForEnd(byte data[]) {
		if(data[0]==0&&data[1]==DATA) {
			int i;
			for(i = 4; i < data.length; i++) {
				if(data[i] == 0) {
					exitNext = true;
					return false;
				}
			}
		} else if(data[0]==0 && data[1]==ACK && exitNext) return true;
	  
		return false;
	}
  
	private boolean clientToServer() {
		byte data[] = new byte[BUFFER_SIZE];
		try {
			clientPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),clientPort);
			clientSocket.receive(clientPacket);
			System.out.println("Recieved packet from client");
			byte temp[] = findError(serverPacket = new DatagramPacket(clientPacket.getData(),clientPacket.getLength(),InetAddress.getLocalHost(),serverPort));
			for(int i = 0; i < temp.length; i++) System.out.print(temp[i]);
			System.out.println();
			serverPacket = new DatagramPacket(temp,temp.length,InetAddress.getLocalHost(),serverPort);
			serverSocket.send(serverPacket);
			System.out.println("Forwarded packet to server");
			return checkForEnd(clientPacket.getData());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return false; 	
	}
  
	private boolean serverToClient() {
		byte data[] = new byte[BUFFER_SIZE];
		try {
			serverPacket = new DatagramPacket(data,BUFFER_SIZE,InetAddress.getLocalHost(),serverPort);
			serverSocket.receive(serverPacket);
			System.out.println("Recieved packet from server");
			byte temp[] = findError(clientPacket = new DatagramPacket (serverPacket.getData(),serverPacket.getLength(),InetAddress.getLocalHost(),clientPort));
			for(int i = 0; i < temp.length; i++) System.out.print(temp[i]);
			System.out.println();
			clientPacket = new DatagramPacket (temp,temp.length,InetAddress.getLocalHost(),clientPort);
			clientSocket.send(clientPacket);
			System.out.println("Forwarded packet to client");
			return checkForEnd(serverPacket.getData());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return false; 	
	}
	
	private byte[] findRequestError(DatagramPacket packet) {
		if(packet.getData()[0]==0 && packet.getData()[1]==this.packetType);
		return packet.getData();
	}
	
	private byte[] findError(DatagramPacket packet) {
		byte temp[] = new byte[2];
		System.arraycopy(packet.getData(), 2, temp, 0, 2);
		//System.out.println(packet.getData()[0]==0);
		//System.out.println(packet.getData()[1]==this.packetType);
		//System.out.println(this.blockNumber.compare(temp));
		for (int i = 0; i < 4; i++) {
			if(packet.getData()[i] != this.comparitorA[i] || packet.getData()[i] != this.comparitorB[i]) return packet.getData();
		}
		return makeError(packet);
	}
	
	private byte[] makeError(DatagramPacket packet) {
		System.out.println();
		System.out.println("Error being generated.");
		System.out.println();
		byte[] block = new byte[BUFFER_SIZE];
		if (this.errorType == PACKET) {
			System.arraycopy(packet.getData(), 0, block, 0, packet.getLength());
			byte temp[];
			if(this.packetType == REQ) {
				int i;
				boolean set;
				switch (this.errorDetail) {
					case 1:
						block[0]++;
						break;
						
					case 2:
						block[1]++;
						break;
						
					case 3:
						temp = new byte[4];
						System.arraycopy(block, 0, temp, 0, temp.length);
						block = temp;
						break;
						
					case 4:
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) break;
						}
						temp = new byte[i];
						System.arraycopy(block, 0, temp, 0, temp.length);
						block = temp;
						break;
						
					case 5:
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) break;
						}
						temp = new byte[i+1];
						System.arraycopy(block, 0, temp, 0, temp.length);
						temp[temp.length-1] = 0;
						block = temp;
						break;
						
					case 6:
						set = false;
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) {
								if (set) break;
								else set = true;
							}
						}
						temp = new byte[i+1];
						System.arraycopy(block, 0, temp, 0, temp.length);
						block = temp;
						break;
						
					case 7:
						set = false;
						for(i = 4; i < block.length; i++) {
							if (block[i]==0) {
								if (set) break;
								else set = true;
							}
						}
						temp = new byte[i+3];
						System.arraycopy(block, 0, temp, 0, temp.length);
						temp[temp.length-2] = 0;
						temp[temp.length-1] = 7;
						block = temp;
						break;
						
					default:
						System.out.println("Error: invalid error details.");
						break;
				}
				
				return block;
				
			} else if (this.packetType == 1) {//DATA
				switch (this.errorDetail) {
					case 1:
						block[0]++;
						break;
						
					case 2:
						block[1]++;
						break;
						
					case 3:
						temp = new byte[2];
						System.arraycopy(block, 2, temp, 0, 2);
						BlockNumber bn = new BlockNumber(temp);
						System.arraycopy(bn.getNext(), 0, block, 2, 2);
						break;
						
					case 4:
						temp = new byte[BUFFER_SIZE + 3];
						temp[BUFFER_SIZE] = 1;
						temp[BUFFER_SIZE+1] = 1;
						temp[BUFFER_SIZE+2] = 1;
						block = temp;
						break;
						
					default:
						System.out.println("Error: invalid error details.");
						break;
				}
				
				return block;
			} else if (this.packetType == 2) {//ACK
				switch (this.errorDetail) {
					case 1:
						block[0]++;
						break;
						
					case 2:
						block[1]++;
						break;
						
					case 3:
						temp = new byte[2];
						System.arraycopy(block, 2, temp, 0, 2);
						BlockNumber bn = new BlockNumber(temp);
						System.arraycopy(bn.getNext(), 0, block, 2, 2);
						break;
						
					case 4:
						temp = new byte[7];
						temp[4] = 1;
						temp[5] = 1;
						temp[6] = 1;
						block = temp;
						break;
						
					default:
						System.out.println("Error: invalid error details.");
						break;
				}
				
				return block;
			} else {
				System.out.println("Invalid packet type chosen.");
				System.exit(1);
			}
			return packet.getData();
		} else if (this.errorType == TIP) {
			DatagramPacket temp = new DatagramPacket(packet.getData(),packet.getLength(),packet.getAddress(),packet.getPort());
			try {
				DatagramSocket fakePort = new DatagramSocket();
				fakePort.send(temp);
				fakePort.receive(temp = new DatagramPacket(new byte[BUFFER_SIZE],BUFFER_SIZE));
				fakePort.close();
				if(temp.getData()[0]==0 && temp.getData()[1]==5 && temp.getData()[2]==0 && temp.getData()[3]==5) {
					int i;
					for(i = 4; i < BUFFER_SIZE; i++) {
						if(temp.getData()[i]==0) break;
					}
					if (i+1 >= BUFFER_SIZE - 4) {
						System.out.println("Error: TID Error Message has no closing zero");
						return packet.getData();
					}
					for(int j = i; j < BUFFER_SIZE; j++) {
						if (temp.getData()[j]!=0) {
							System.out.println("Error: TID Error Message has data after closing zero");
							return packet.getData();
						}
					}
					System.out.println("TID Error recieved");
					return packet.getData();
				} else {
					System.out.println("Error: TID Error encoded improperly");
					return packet.getData();
				}
			} catch (SocketException e) {
				e.printStackTrace();
				System.out.println("Socket Exception Error");
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Socket Exception Error");
				System.exit(1);
			}
			
		} else {
			System.out.println("Incorrect error type.  Shutting down.");
			System.exit(1);
		}
		return packet.getData();
	}
}
