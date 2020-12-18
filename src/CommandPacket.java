import java.io.Serializable;

public class CommandPacket implements Serializable {
    public boolean command;// true = command, false = ack
    public int packetNumberExpected;
    public String expectedFile;
    public int recvWindow;

    /**
     *
     * @param command : whether it is a command packet or an ack packet (true = command, false = ack)
     * @param packetNumberExpected : next expected packet number from Lander
     * @param recvWindow : number of packets expected from the lander
     */
    public CommandPacket(boolean command, int packetNumberExpected,int recvWindow){
        this.command = command;
        this.packetNumberExpected = packetNumberExpected;
        this.recvWindow =recvWindow;
    }

    /**
     * @param command : whether it is a command packet or an ack packet (true = command, false = ack)
     * @param packetNumberExpected : next expected packet number from Lander
     * @param recvWindow : number of packets expected from the lander
     * @param expectedFile : expected file from the lander
     * @param recvWindow : number of packets expected from the lander
     */
    public CommandPacket(boolean command, int packetNumberExpected , String expectedFile,int recvWindow){
        this.command = command;
        this.packetNumberExpected = packetNumberExpected;
        this.expectedFile = expectedFile;
        this.recvWindow=recvWindow;
    }
}
