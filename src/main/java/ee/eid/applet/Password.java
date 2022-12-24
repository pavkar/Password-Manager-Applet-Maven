package ee.eid.applet;

public class Password {
    public byte[] data;

    public Password(short dataLen) {
        data = new byte[dataLen];
    }
}
