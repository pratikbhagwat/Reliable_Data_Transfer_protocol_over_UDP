import java.io.Serializable;

public class DataPacket implements Serializable {
    public byte[] data;
    public int packetNumber;
    public boolean finFlag = false;

    /**
     *
     * @param data : byte array of the dat ato be sent from lander to Base Station
     * @param packetNumber : sequence number of the packet.
     */
    public DataPacket(byte[] data,int packetNumber){
        this.data = data;
        this.packetNumber = packetNumber;
    }
}
