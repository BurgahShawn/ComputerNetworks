import java.io.*;
import java.net.*;

/** 
* UDPServer.java 
* Receives a HTTP request from a client, reads the file, then segments it and 
* places the segments in packets to send back to the client.
*
* @author Shawn Lin, Benjamin Dempsey, Abigail Livingston
* @version 7.21.21
*/
public class UDPServer{
  // Server UDP socket runs at this port, arbitrarily chosen.
  public final static int SERVICE_PORT=50001;
 
  public static void main(String[] args) throws Exception{
    while(true){

      // Instantiating server socket
      DatagramSocket serverSocket = new DatagramSocket(SERVICE_PORT);
      int packetCount = 0;
      int endFile = 0;
      
      // Creating buffers 
      byte[] sendingNullPacket = new byte[1];   
      byte[] receivingDataBuffer = new byte[1024];   

      // Creating a UDP packet
      DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
      System.out.println("Waiting for a client to connect...");
      
      // Receiving data from the client and storing in inputPacket
      serverSocket.receive(inputPacket);
      System.out.println("\nClient message received. Processing request...\n");
      
      // Getting filename from received data, reading and printing file
      String receivedData = new String(inputPacket.getData());
      byte[] receivedFile = new byte[107374];
      int wasRead;
      String[] splitReq = receivedData.split(" ");
      String fileName = splitReq[1];
      RandomAccessFile dataFile = new RandomAccessFile(fileName, "r");
      long fileSize = dataFile.length();
      wasRead = dataFile.read(receivedFile, 0, receivedFile.length);
      String recFile = new String(receivedFile);
      System.out.println("Requested file's contents: \n" + recFile);
      dataFile.seek(0); //set pointer to beginning of file


      // Obtaining client's IP address and the port
      InetAddress senderAddress = inputPacket.getAddress();
      int senderPort = inputPacket.getPort();

      // Creating and sending packets to client, loop breaks when end of file is reached
      while (endFile != -1){

          String tempHeader = "";
          String sendData = "";
          byte[] header;
          byte[] sendPacket;
          byte[] sendingDataBuffer = new byte[1024];

          tempHeader = buildFileHeader(packetCount, fileSize); //create header
          header = tempHeader.getBytes(); 
          sendPacket = makeRoom(header);

          /*
           * Read from file and place in sendData, starting from the end of the
           * header and stopping at 1024 bytes (segmenting). Returns '-1' if
           * end of file is reached.
           */
          endFile = dataFile.read(sendPacket, header.length, (sendPacket.length - header.length)); 

          // Break if end of file
          if(endFile == -1){
            break;
          }
          System.out.println("Processing packet " + (packetCount) + "...\n");
          sendData = getSum(sendPacket);
          System.out.println("Packet " + (packetCount) + "'s contents: " 
            + sendData + "\n");
          sendingDataBuffer = sendData.getBytes();
          System.out.println("Sending packet " + (packetCount) + "...\n");
          DatagramPacket outputPacket = new DatagramPacket(sendingDataBuffer, 
            sendingDataBuffer.length,senderAddress,senderPort);
          // Sending the created packet to client
          serverSocket.send(outputPacket);
          
          packetCount++;
        }
      String n = "\0"; //null character
      sendingNullPacket = n.getBytes(); //put packet data (in bytes) into packet buffer
      System.out.println("Reached the end of the file. Sending null packet...\n");
      DatagramPacket outputPacketNull = new DatagramPacket(sendingNullPacket, 
        sendingNullPacket.length, senderAddress, senderPort);
      serverSocket.send(outputPacketNull);
      // Close socket and request file
      serverSocket.close();
      dataFile.close();
    }
  }

  /* 
    * Method that creates a header for the HTTP response 
    * messages.
    *
    * Inputs: int (packet number), long (file size)
    * Outputs: string (header)
  */
  public static String buildFileHeader(int packetNum, long fileSize){
      String fHeader = "Packet " + (packetNum) + "\n" + "Checksum: " + "00000\r\n"  + "HTTP/1.0 200 Document Follows\r\n"
        + "Content-Type: text/plain\r\n" + "Content-Length: " + fileSize + "\r\n\r\n";
      return fHeader;
  }

  /* 
    * Method that offsets a packet with enough space
    * for a header.
    *
    * Inputs: byte array (packet)
    * Outputs: byte array (packet with header space)
  */
  public static byte[] makeRoom(byte[] header){
      byte[] header2 = new byte[1024];
      for(int offset = 0; offset < header2.length; offset++){
          if(offset < header.length){
              header2[offset] = header[offset];
          }
          else{
              header2[offset] = 32;
          }
      }
      return header2;
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
    * Method that creates a checksum to be inserted
    * into a packet header.
    *
    * Inputs: byte array (packet)
    * Outputs: string (packet including checksum value)
  */
  public static String getSum(byte[] data){
      
      String info = new String(data);
      info = info.substring(info.indexOf("h:") + 13); //remove header
      byte[] checkData = info.getBytes();
      int checkSum = checkSum(checkData); //calculate checksum of packet without header
      info = Integer.toString(checkSum); 

      String packetData = new String(data);
      int index = packetData.indexOf(":") + 2; //place pointer at position to insert value
      byte[] sum = info.getBytes();
      int length = info.length();

      // Replacing '00000' in packet header with checksum value
      switch(length){
          case 2:
            data[index + 3] = sum[0];
            data[index + 4] = sum[1];
            break;
          case 3:
            data[index + 2] = sum[0];
            data[index + 3] = sum[1];
            data[index + 4] = sum[2];
            break;
          case 4:
            data[index + 1] = sum[0];
            data[index + 2] = sum[1];
            data[index + 3] = sum[2];
            data[index + 4] = sum[3];
            break;
          case 5:
            data[index] = sum[0];
            data[index + 1] = sum[1];
            data[index + 2] = sum[2];
            data[index + 3] = sum[3];
            data[index + 4] = sum[4];
            break;
          default:
            break;

      }
      String result = new String(data);
      return result;
  }
}

