import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;


public class BaseStation {
    public static InetAddress destinationIPAddress;
    public static boolean stopReceving = false;
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        DatagramSocket socket = new DatagramSocket();
        destinationIPAddress = InetAddress.getByName(args[0]);

        promptForCommand();

    }

    /**
     * This method will prompt the user for input of command and handle the data transfer.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void promptForCommand() throws IOException, ClassNotFoundException {
        System.out.println("Enter the command");
        Scanner sc = new Scanner(System.in);
        String commandString = sc.nextLine() ;
        CommandPacket commandPacket;

        if (commandString.startsWith("get")){ // get the desired file
            commandPacket = new CommandPacket(true,1,commandString.substring(4),1);
            sendCommandAndReceiveData(commandPacket);
        }else if (commandString.startsWith("ls")) { // look for the files in Lander
            commandPacket = new CommandPacket(true,1,1);
            sendCommandAndPrintData(commandPacket);
        }

    }

    /**
     * This method will send the command to the Lander and it will print the complete response received from the lander.
     *
     * @param commandPacket : Command packet to be send to the Lander.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void sendCommandAndPrintData(CommandPacket commandPacket) throws IOException, ClassNotFoundException {
        DatagramSocket datagramSocket=new DatagramSocket();
        datagramSocket.setSoTimeout(5000);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(commandPacket);
        objectOutputStream.flush();

        byte[] msg = byteArrayOutputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,destinationIPAddress,63000);
        datagramSocket.send(packet);

        ArrayList<DataPacket> orderedData;

        while (! stopReceving){
            orderedData = recieveBatchData(datagramSocket,commandPacket.recvWindow,commandPacket.packetNumberExpected);
            if (orderedData.size()==0){
                commandPacket.recvWindow = Math.max(1,commandPacket.recvWindow/2);
                sendAck(commandPacket,datagramSocket);
                continue;
            } else if (orderedData.size()<commandPacket.recvWindow){
                commandPacket = new CommandPacket(false,orderedData.get(orderedData.size()-1).packetNumber+1,commandPacket.expectedFile, Math.max(commandPacket.recvWindow/2,1) );// if partial data packets from the window are received
            }
            else{
                commandPacket = new CommandPacket(false,orderedData.get(orderedData.size()-1).packetNumber+1,commandPacket.expectedFile,commandPacket.recvWindow*2);// now on the command packets will be ack packets only.
            }
            sendAck(commandPacket,datagramSocket);
            printData(orderedData);
        }
    }

    /**
     *
     * Printing logic.
     * Prints the data bytes received from the lander.
     * @param orderedData : ArrayList of the Data Packets received from the lander.
     */
    private static void printData(ArrayList<DataPacket> orderedData) {
        for (DataPacket dataPacket : orderedData){
            for (byte b : dataPacket.data){
                System.out.print((char) b);
            }
        }
    }

    /**
     * This method will send the command to the Lander and it will store the complete response received from the lander in a local file.
     *
     * @param commandPacket : Command packet to be send to the Lander.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void sendCommandAndReceiveData(CommandPacket commandPacket) throws IOException, ClassNotFoundException {
        DatagramSocket datagramSocket=new DatagramSocket();
        datagramSocket.setSoTimeout(5000);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(commandPacket);
        objectOutputStream.flush();

        byte[] msg = byteArrayOutputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,destinationIPAddress,63000);
        datagramSocket.send(packet);

        ArrayList<DataPacket> orderedData;

        while (! stopReceving){
            orderedData = recieveBatchData(datagramSocket,commandPacket.recvWindow,commandPacket.packetNumberExpected);
            if (orderedData.size()==0){
                commandPacket.recvWindow = Math.max(1,commandPacket.recvWindow/2);
                sendAck(commandPacket,datagramSocket);
                continue;
            } else if (orderedData.size()<commandPacket.recvWindow){
                commandPacket = new CommandPacket(false,orderedData.get(orderedData.size()-1).packetNumber+1,commandPacket.expectedFile, Math.max(commandPacket.recvWindow/2,1) );// if partial data packets from the window are received
            }
            else{
                commandPacket = new CommandPacket(false,orderedData.get(orderedData.size()-1).packetNumber+1,commandPacket.expectedFile,commandPacket.recvWindow*2);// now on the command packets will be ack packets only.
            }
            sendAck(commandPacket,datagramSocket);
            appendDataOnLocalFile(orderedData);
        }

    }

    /**
     * This method will append the data from the packets to a local file
     * @param orderedData : Data packets in correct order
     * @throws IOException
     */
    private static void appendDataOnLocalFile(ArrayList<DataPacket> orderedData) throws IOException {
        Path copiedFilePath = Paths.get("/var/log/data/CopiedFilest.mp4");
        if ( ! Files.exists(copiedFilePath) ){
            Files.createFile(copiedFilePath);
        }
        for (DataPacket dataPacket:orderedData){
            Files.write(copiedFilePath,dataPacket.data, StandardOpenOption.APPEND);
        }
    }

    /**
     *send the ack to Lander
     * @param ack : Acknowledgement to be sent to the Lander
     * @param datagramSocket : Datagram socket using which the ack must be sent
     * @throws IOException
     */
    private static void sendAck(CommandPacket ack,DatagramSocket datagramSocket) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(ack);
        objectOutputStream.flush();

        byte[] msg = byteArrayOutputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,destinationIPAddress,63000);
        datagramSocket.send(packet);

    }

    /**
     * This method will receive the batch of packets from lander and it will order them according to the sequence number.
     * @param datagramSocket socket used to receive the data from lander
     * @param recvWindow number of packets expected from lander
     * @param nextExpectedPacket sequence number of the next packet expected from lander
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static ArrayList<DataPacket> recieveBatchData(DatagramSocket datagramSocket, int recvWindow,int nextExpectedPacket) throws IOException, ClassNotFoundException {

        ArrayList<DataPacket> receivedDataPackets = new ArrayList<>();

        for (int i = 0; i < recvWindow; i++) {
            byte[] rcvPacketdata = new byte[60000];
            DatagramPacket rcvPacket = new DatagramPacket(rcvPacketdata, rcvPacketdata.length);
            try {
                datagramSocket.receive(rcvPacket);
            }catch (SocketTimeoutException e){
                System.out.println("Timeout has occurred time to start congestion avoidance");

                return getValidPacketsInTheSequence(receivedDataPackets,nextExpectedPacket);
            }

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( rcvPacket.getData());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            DataPacket dataPacket = (DataPacket)objectInputStream.readObject();
            receivedDataPackets.add(dataPacket);
            if (dataPacket.finFlag){
                stopReceving = true;
                break;
            }
        }

        receivedDataPackets.sort(Comparator.comparingInt(o -> o.packetNumber));

        return receivedDataPackets;

    }

    /**
     *
     * @param receivedDataPackets : Received chunk of data packets
     * @param nextExpectedPacket : the sequence number of 1st expected packet in the chunk.
     * @return
     */
    private static ArrayList<DataPacket> getValidPacketsInTheSequence(ArrayList<DataPacket> receivedDataPackets, int nextExpectedPacket) {
        ArrayList<DataPacket> validDataPackets = new ArrayList<>();
        receivedDataPackets.sort(Comparator.comparingInt(o -> o.packetNumber));
        for (DataPacket dataPacket:receivedDataPackets){
            if (dataPacket.packetNumber==nextExpectedPacket){
                validDataPackets.add(dataPacket);
                nextExpectedPacket++;
            }else {
                break;
            }
        }
        return validDataPackets;
    }
}
