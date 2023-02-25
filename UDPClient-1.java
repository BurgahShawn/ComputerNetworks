import java.io.*;
import java.net.*;
import java.util.*;

/** 
* UDPClient.java 
* Sends a HTTP request to a server, then receives packets from the server
* and re-assembles them to extract the message.
*
* @author Shawn Lin, Benjamin Dempsey, Abigail Livingston
* @version 7.21.21
*/

public class UDPClient{
  /* The server port to which the client 
  socket is going to connect, arbitrarily chosen. */
  public final static int SERVICE_PORT = 50001;
  
  public static void main(String[] args) throws Exception{

      // Creating variables
      String message = "";  
      int lastMessage = 0;
      boolean endFile = false;
      int packetCount = 0;
      int gremlinProb;
      ArrayList<String> fullDataList = new ArrayList<String>();

      // Instantiating client socket
      DatagramSocket clientSocket = new DatagramSocket();
      
      // Getting the IP address of the server
      InetAddress IPAddress = InetAddress.getLocalHost();
      
      // Creating buffer
      byte[] sendingDataBuffer = new byte[1024];

      /* Converting data to bytes and 
      storing them in the sending buffer */
      String http = "Get TestFile.html HTTP/1.0";
      sendingDataBuffer = http.getBytes();
      
      // Creating a UDP packet 
      DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer,
        sendingDataBuffer.length,IPAddress, SERVICE_PORT);
      System.out.println("Sending message to server...");
      System.out.println("\nMessage contents: \n" + http);
      
      //Getting gremlin probability from user
      Scanner in = new Scanner(System.in);
      System.out.print("\nPlease enter a probability for error between 1 and 100: ");
      gremlinProb = in.nextInt();
      in.close();

      // Sending UDP packet to server
      clientSocket.send(sendingPacket);
    
      // Collecting server response, loops until end of file is reached
      while(!endFile){ 

        // Creating buffer
        byte[] receivingDataBuffer = new byte[1024];

        // Creating UDP packet 
        DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer,
          receivingDataBuffer.length);
        clientSocket.receive(receivingPacket);
        System.out.println("\nServer reply received.");

        // Storing data in byte array
        byte[] content = new byte[1024]; 
        content = receivingPacket.getData();

        // Reassembling data
        message = new String(content);
        lastMessage = message.indexOf('\0');
        if(lastMessage != -1){
          endFile = true;
          break;
        }
        System.out.println("Packet " + packetCount + "'s contents: \n" + message);
        String modMessage = removeHeader(message);
        fullDataList.add(modMessage);

        // Using probability to decide whether gremlin is used
        Random r = new Random();
        int flip = r.nextInt(99) + 1;
        if(flip <= gremlinProb){
          gremlin(content);
        }

        // Detecting errors in data
        detectError(content, packetCount);

        packetCount++;
    }
      // Closing the socket connection with the server
      clientSocket.close();

      // Writing content to output file
      System.out.println("Reached end of packets.\n");
      System.out.println("\nSaving content to file...");
      String x = new String();
      for(int j = 0; j < fullDataList.size(); j++){
        x += fullDataList.get(j);
      }
      writeFile(x);
      System.out.println("Output file contents: \n" + x);
  }

  /* 
    * Method that decides whether to damage or pass along
    * a given packet.
    *
    * Inputs: byte array (packet)
    * Outputs: byte array (damaged packet)
  */
  public static byte[] gremlin(byte[] dataIn) throws IOException {
			
		Random r = new Random();
		//decides how many bytes to change in byte[]
		int x = 0;
		int percent = r.nextInt(100 - 1 + 1) + 1;
		if (percent < 50){
		    x = 1; 
		}
		else if (percent < 80) {
		    x = 2;
		}
		else if (percent <= 100){
		    x = 3;
		} 
		for(int i = 0; i < x; i++) {
			//selects the array to change
			int num = r.nextInt(dataIn.length);
			//selects the change value
			int num2 = r.nextInt(128);
			dataIn[num] = (byte) ~dataIn[num2];
		}
		 return dataIn;
	 }	
	

 /* 
    * Method that deletes the header from a packet 
    * to make it easier to read.
    *
    * Inputs: string (packet data)
    * Outputs: string (modified packet data)
  */
  public static String removeHeader(String data){

      String modified = data.substring(data.indexOf("h:") + 13);
      return modified;
  }

  /* 
    * Method that computes the checksum for a packet by 
    * summing all of the bytes together.
    *
    * Inputs: byte array (packet)
    * Outputs: int (sum)
  */
  public static int checkSum(byte[] data){

    int sum = 0;
    for(int i = 0; i < data.length; i++){
        sum += (int) data[i];
    }
    return sum;
  }

  /* 
    * Method that extracts the checksum for a *received* packet
    * to use in comparison for the Error Detection method.
    *
    * Inputs: byte array (packet)
    * Outputs: string (checksum)
  */
  public static String grabSum(byte[] data){

      String checkSum = ""; //string to store checksum
      boolean zeroes = true; //boolean for whether there are leading zeroes

      byte[] dataCheckSum = new byte[5]; //byte array to store 5 possible checksum values
      String packetData = new String(data); //convert to string to search through for index
      int index = packetData.indexOf(":") + 1;
      int j = 0;
      for(int i = index + 1; i < index + 6; i++){
          dataCheckSum[j] = data[i]; //write the values from input array to created one
          j++;
      }
      checkSum = new String(dataCheckSum); //convert back to string to search
      // Clears leading zeroes from checksum
      while(zeroes){
          zeroes = checkSum.startsWith("0");
          if(zeroes){
              checkSum = checkSum.substring(1);
          }
      }
      return checkSum;
  }
  /* 
    * Method that writes data from a packet to a file.
    *
    * Inputs: string (packet data)
    * Outputs: none
  */
  public static void writeFile(String data){

      try{
          PrintWriter writer = new PrintWriter("Output.txt", "UTF-8");
          writer.println(data);
          writer.close();
      }
      catch(IOException e){
          System.out.println("Failed to write file with error: " + e);
      }
      System.out.println("Successfully saved file 'Output.txt'.\n");
  }

  /* 
    * Method that compares checksums to detect errors.
    *
    * Inputs: byte array (packet), int (packet number)
    * Outputs: none 
  */
  public static void detectError(byte[] data, int packetNumber){

      int result = 0;
      String myCheckSum = grabSum(data); //calculates checksum from packet
      byte[] modHeader = data; //copy of byte array to turn into a string
      String checkSum = new String(modHeader); //string of copied byte array
      checkSum = removeHeader(checkSum); //removes header from data
      result = checkSum(checkSum.getBytes()); //calculates checksum without header
      
      if(myCheckSum.equals(Integer.toString(result)) || data[0] == 0){
          System.out.println("\nNo error found in packet #" + packetNumber);
      }
      else{
          System.out.println("\nERROR DETECTED IN PACKET # " + packetNumber);
      }
  }
}