import java.io.*;
import java.net.*;
import java.util.*;
/**
 * This file implements the RIP Version 2 protocol.
 * @author Pranit Meher (pxm3417@rit.edu)
 *
 */

public class RIP_v2 {
	
	public static void main(String[] args) {
		// Main method of the program
		
		// Declarations and inputs
		String configFileName = args[0];
		Scanner input = new Scanner(System.in);
		// Read File
		RIP_v2 reader = new RIP_v2();
		reader.readFile(configFileName);
	}

	public void readFile(String configFileName){
		// This function reads File line by line
		
		// Declarations
		BufferedReader readLine_br = null;
		FileReader readLine_fr = null;
		String line;
		String searchLink = "LINK:";
		String searchNetwork = "NETWORK:";
		String selfHop = "0.0.0.0:0";
		int selfCost = 0;
		routingTableClass routingTableObject = new routingTableClass();
				
		try{
			readLine_fr = new FileReader(configFileName);
			readLine_br = new BufferedReader(readLine_fr);
			
			// reading line by line
			while((line = readLine_br.readLine()) != null){
				
				// making an array of the input
				String[] ipArray_1 = line.split(" ");
				
				// for LINKS
				if(ipArray_1[0].equals(searchLink))
				{
					
					String[] ipArray = {ipArray_1[0],ipArray_1[1],ipArray_1[2]};
					//segregating the IP's and the Port Numbers
					String ip_1 = ipArray[1].split(":")[0];
					int port_1 = Integer.parseInt(ipArray[1].split(":")[1]);
					String ip_2 = ipArray[2].split(":")[0];
					int port_2 = Integer.parseInt(ipArray[2].split(":")[1]);
					
					//making objects for receiving and sending
					Runnable senderObject = new Sender(ip_2,port_2, port_1, routingTableObject);
					Runnable receiverObject = new Receiver(ip_1, port_1,ip_2,port_2,routingTableObject);
					
					// making Receiver and Sender Thread
					Thread receiverThread = new Thread(receiverObject);
					Thread senderThread = new Thread(senderObject);
					
					//starting the sending and receiving Threads
					senderThread.start();
					receiverThread.start();
				}
				// for NETWORKS
				else if(ipArray_1[0].equals(searchNetwork))
				{
					String[] ipArray = {ipArray_1[0],ipArray_1[1]};
					ArrayList addEntry = new ArrayList();
					addEntry.add(ipArray[1]);
					addEntry.add(selfHop);
					addEntry.add(selfCost);
								
					// making entry in the routing table
					synchronized (routingTableObject) {
						routingTableObject.routingTable.add(addEntry);
						routingTableObject.printTable();
					}
				}	
			}
		}
		catch(IOException exc){
			exc.printStackTrace();
		}
	}

}

//--------------------------------------------------------------------------------------------------

class routingTableClass implements Serializable{
	
	ArrayList<ArrayList> routingTable = new ArrayList<>();
	
	public void printTable() {
		System.out.println("Address \t\tNext Hop \t\tCost");
		System.out.println("======================================================");
		
		Iterator<ArrayList> iter = routingTable.iterator(); // just an iterator
		while(iter.hasNext()){
			ArrayList temp = iter.next();
			System.out.println(temp.get(0) + "\t\t" + temp.get(1) + "\t\t" + temp.get(2));
		}
		System.out.println("--------------------------------------------------------\n");
	}

	public ArrayList<ArrayList> compareRoutingTables(routingTableClass routingTableObject, String receive_ip, int receive_port, String send_ip, int send_port) {
		
		ArrayList<ArrayList> routingtb = routingTableObject.routingTable;		
		Iterator<ArrayList> iter_other = routingtb.iterator(); // just an iterator
		
		//making a temporary arrayList of all the Networks
		ArrayList networks = new ArrayList<>();
		while(iter_other.hasNext()){
			ArrayList adding = iter_other.next();
			networks.add(adding.get(0));
		}
		
		int last_index = networks.size();
		
		for(int i = 0; i < last_index; i++){
			//iterating through all the networks from the received routing table
			//declarations
			String ip_other = networks.get(i).toString();
			int found_flag = 0;
			ArrayList ToInsertAfter = new ArrayList<>();
			ArrayList ToRemoveAtfer = null;
			Iterator<ArrayList> iter_own = this.routingTable.iterator(); // just an iterator
			int InsertAt = -1;
			
			while(iter_own.hasNext()){
				// comparing values of received table with own routing table
				InsertAt++;
				ArrayList temp_i = iter_own.next();
				
				// if two similar IPs are found
				if(ip_other.equals((String)temp_i.get(0))){
					
					// if cost is 16
					if(((int)routingtb.get(i).get(2) == 16) && ((int)temp_i.get(2) != 0) ){
						ToRemoveAtfer = temp_i;
						String nextHop = send_ip+":"+send_port;
						ToInsertAfter.add(0,(String)temp_i.get(0));
						ToInsertAfter.add(1,nextHop);
						ToInsertAfter.add(2,16);
						found_flag++;
					}
					// if cost is less than the current cost
					else if((int)routingtb.get(i).get(2) < (int)temp_i.get(2)){
						ToRemoveAtfer = temp_i;
						String nextHop = send_ip+":"+send_port;
						ToInsertAfter.add(0,(String)temp_i.get(0));
						ToInsertAfter.add(1,nextHop);
						ToInsertAfter.add(2,Math.min(16,(int) routingtb.get(i).get(2) +1));
						found_flag++;
					}
					found_flag++;
					
					break;
				}
			}
			
			if(found_flag == 2){
				this.routingTable.remove(ToRemoveAtfer);
				this.routingTable.add(InsertAt,ToInsertAfter);
			}
			else if(found_flag == 0){
				ArrayList toAdd = new ArrayList<>();
				toAdd = routingtb.get(i);
				// updating NextHop
				toAdd.remove(1);
				String nextHop = send_ip+":"+send_port;
				toAdd.add(1, nextHop);
				//updating Cost
				int temp_cost = (int) toAdd.get(2);
				toAdd.remove(2);
				toAdd.add(2, Math.min(16,temp_cost+1));
				this.routingTable.add(toAdd);				
			}
		
			if(found_flag == 0 || found_flag == 2){
				printTable();
			}
		}
		return this.routingTable;
	}

	public ArrayList<ArrayList> makeInfinite(String send_ip, int send_port) {
		// this method makes the cost infinite (16)
		
		String nextHop = send_ip+":"+send_port;
		Iterator<ArrayList> iter_own = this.routingTable.iterator(); // just an iterator
		while(iter_own.hasNext()){
			ArrayList tempList = iter_own.next();
			if(nextHop.equals(tempList.get(1))){
				tempList.remove(2);
				tempList.add(16);
			}
		}
		printTable();
		return this.routingTable;
	}
}

//--------------------------------------------------------------------------------------------------

class Sender implements Runnable{
	String send_ip;	// ip to send at
	int send_port;	// port to send at
	int from_port;	// port sent from
	DatagramSocket sendSocket;
	routingTableClass routingTableObject;
	
	// constructor
	public Sender(String ip_2, int port_2, int port_1,routingTableClass routingTable){
		send_ip = ip_2;
		send_port = port_2;
		from_port = port_1;
		routingTableObject = routingTable;
	}
	
	public void makeSocket(){
		try{
			sendSocket = new DatagramSocket();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		makeSocket();
		String send_ip = this.send_ip;
		int send_port = this.send_port;
		DatagramSocket sendSocket = this.sendSocket;
		while(true){
			try {
				Thread.sleep(5000);
				// sending routing table
				ByteArrayOutputStream outputData = new ByteArrayOutputStream();
				ObjectOutputStream objectOutput = new ObjectOutputStream(outputData);
				
				synchronized (routingTableObject) {
					objectOutput.writeObject(routingTableObject);
				}
				byte[] buffer = outputData.toByteArray();
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(send_ip), send_port);
				sendSocket.send(packet);
				
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}

//--------------------------------------------------------------------------------------------------
class Receiver implements Runnable{

	int receive_port;	// port to receive at
	String receive_ip;	// ip to receive at
	DatagramSocket receivingSocket; // socket to receive at
	routingTableClass ownRoutingTable; // object of the own routing table
	routingTableClass routingTableObject; // object of the incoming routing table
	int send_port; // port to send at
	String send_ip; // ip to send at
	
	
	public Receiver(String ip_1, int port_1, String ip_2, int port_2, routingTableClass routingTableObject1) {
		receive_port = port_1;
		receive_ip = ip_1;
		send_port = port_2;
		send_ip = ip_2;
		this.ownRoutingTable = routingTableObject1;
	}
	public void makeSocket(){
		try {
			this.receivingSocket = new DatagramSocket(receive_port);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		makeSocket();
		DatagramSocket receivingSocket = this.receivingSocket;
		
		while(true){
			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			try {
				receivingSocket.setSoTimeout(10000); 
				// accepting input
				receivingSocket.receive(packet);
				byte[] data = packet.getData();
				ByteArrayInputStream inputData = new ByteArrayInputStream(data);
				ObjectInputStream ObjectData = new ObjectInputStream(inputData);
				routingTableObject = (routingTableClass) ObjectData.readObject();
				
				synchronized (ownRoutingTable) {
					// comparing the received table with current table
					ownRoutingTable.routingTable = ownRoutingTable.compareRoutingTables(routingTableObject, receive_ip, receive_port, send_ip, send_port);
				}

		        // Reseting the length of the packet
		        packet.setLength(buffer.length);
			} catch (SocketTimeoutException e) {
				synchronized (ownRoutingTable) {
					// after timeout, making the cost infinite
					ownRoutingTable.routingTable = ownRoutingTable.makeInfinite(send_ip,send_port);
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}	
	}
}
//--------------------------------------------------------------------------------------------------