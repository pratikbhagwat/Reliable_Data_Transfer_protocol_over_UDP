import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Lander {
    public static int dataByteArraySize = 50000;
    public static RandomAccessFile randomAccessFile;
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        DatagramSocket datagramSocket = new DatagramSocket(63000);

        while (true){// server always on
            byte[] receivingData = new byte[60000];
            DatagramPacket receivingPacket = new DatagramPacket(receivingData,receivingData.length);
            datagramSocket.receive(receivingPacket);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( receivingPacket.getData());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            CommandPacket commandPacket = (CommandPacket)objectInputStream.readObject();
            if (commandPacket.expectedFile!=null){
                handleDataCommand(commandPacket,receivingPacket);
            }else {
                handleLsCommand(commandPacket,receivingPacket);
            }

        }
    }

    /**
     * This method will respond to any of the command packets like ls
     * @param commandPacket Instance of Command Packet
     * @param receivingPacket : Recieved packet from the base station
     * @throws IOException
     */
    private static void handleLsCommand(CommandPacket commandPacket, DatagramPacket receivingPacket) throws IOException {
        File f = new File("/var/log/data");
        String[] availableFiles = f.list();


        DataPacket dataPacket = new DataPacket(String.join(" ",availableFiles).getBytes(),commandPacket.packetNumberExpected);
        dataPacket.finFlag=true;
        sendDataPacket(dataPacket,receivingPacket);

    }
    /**
     * This method will respond to any of the ack packets or data related commands like get.
     * @param commandPacket Instance of Command Packet
     * @param receivingPacket : Recieved packet from the base station
     * @throws IOException
     */
    private static void handleDataCommand(CommandPacket commandPacket, DatagramPacket receivingPacket) throws IOException {

        File f = new File("/var/log/data");
        String[] availableFiles = f.list();
        HashSet<String> setOfAvailableFiles = new HashSet<>();

        assert availableFiles != null;
        Arrays.stream(availableFiles).forEach(filename->setOfAvailableFiles.add(filename));

        if (setOfAvailableFiles.contains( commandPacket.expectedFile)){
            randomAccessFile = new RandomAccessFile(new File("/var/log/data/" + commandPacket.expectedFile) , "r");

            int numberOfDataPacketsToBeSent = commandPacket.recvWindow;


            ArrayList<DataPacket> dataPackets = new ArrayList<>();
            randomAccessFile.seek((commandPacket.packetNumberExpected-1) * dataByteArraySize );// dataByteArraySize byte data in each packet.
            int packetNumber = commandPacket.packetNumberExpected;

            for (int i=0;i<numberOfDataPacketsToBeSent;i++){
                byte[] data = new byte[dataByteArraySize];
                int numberOfBytesRead = randomAccessFile.read(data);
                if ( (numberOfBytesRead == dataByteArraySize)) {
                    DataPacket dataPacket = new DataPacket(data, packetNumber);
                    packetNumber += 1;
                    dataPackets.add(dataPacket);
                }else if (numberOfBytesRead==-1){
                    if (dataPackets.size()>0){
                        dataPackets.get(dataPackets.size()-1).finFlag=true;
                    }
                    break;
                }else {
                    byte[] lastDataChunk = Arrays.copyOfRange(data,0,numberOfBytesRead);
                    DataPacket dataPacket = new DataPacket(lastDataChunk, packetNumber);
                    dataPacket.finFlag = true;
                    dataPackets.add(dataPacket);
                    break;
                }
            }

            dataPackets.forEach(dataPacket -> {
                try {
                    sendDataPacket(dataPacket,receivingPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }else {
            String responseFileNotFound = "File not available at Lander";
            byte[] data = responseFileNotFound.getBytes();
            DataPacket dataPacket = new DataPacket(data,commandPacket.packetNumberExpected);
            dataPacket.finFlag = true;
            sendDataPacket(dataPacket,receivingPacket);
        }

    }

    /**
     * This method will respond to any of the ack packets or data related commands like get.
     * @param dataPacket Instance of data Packet to be sent to Base station.
     * @param receivingPacket : Received packet from the base station
     * @throws IOException
     */
    private static void sendDataPacket(DataPacket dataPacket,DatagramPacket receivingPacket) throws IOException {
        DatagramSocket datagramSocket=new DatagramSocket();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(dataPacket);
        objectOutputStream.flush();
        byte[] msg = byteArrayOutputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,receivingPacket.getAddress(),receivingPacket.getPort());
        datagramSocket.send(packet);
    }
}
