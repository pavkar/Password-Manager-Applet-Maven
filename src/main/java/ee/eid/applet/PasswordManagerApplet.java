package ee.eid.applet;

import javacard.framework.*;
import javacardx.crypto.Cipher;

public class PasswordManagerApplet extends Applet {
    //Define the value of CLA/INS in APDU, you can also define P1, P2.
    static final byte CLA_READ_BINARY = (byte) 0xB0;
    static final byte INS_VERIFY = (byte) 0x50;
    static final byte INS_CHANGE_PIN = (byte) 0x51;
    static final byte INS_RESET_PIN = (byte) 0x52;
    static final byte INS_SAVE_PASSWORD = (byte) 0x53;
    static final byte INS_GET_PASSWORD = (byte) 0x54;
    static final byte INS_DELETE_PASSWORD = (byte) 0x55;

    final static short SW_BAD_PIN = (short) 0x6900;
    final static short SW_SECURITY_STATUS_NOT_SATISFIED = (short) 0x6680;
    final static short SW_APPLET_BLOCKED = (short) 0x6999;
    final static short SW_NO_PASSWORD = (short) 0x6A88;

    private OwnerPIN adminPin;
    private OwnerPIN userPin;

    private Password[] passwordData;

    private Cipher cipher;

    public PasswordManagerApplet(byte[] buffer, short offset, byte length) {
        passwordData = new Password[10];

        userPin = new OwnerPIN((byte) 3, (byte) 4);
        userPin.update(new byte[] {0x00, 0x00, 0x00, 0x00}, (short) 0, (byte) 4);

        adminPin = new OwnerPIN((byte) 3, (byte) 5);
        adminPin.update(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00}, (short) 0, (byte) 5);

        cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new PasswordManagerApplet(bArray, bOffset, bLength).register();
    }

    public void process(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();

        if (selectingApplet()) {
            return;
        }
        if (apduBuffer[ISO7816.OFFSET_CLA] == CLA_READ_BINARY) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (apduBuffer[ISO7816.OFFSET_INS]) {
            case INS_VERIFY:
                verifyPIN(apdu);
                break;
            case INS_CHANGE_PIN:
                changePIN(apdu);
                break;
            case INS_RESET_PIN:
                resetPIN(apdu);
                break;
            case INS_SAVE_PASSWORD:
                savePassword(apdu);
                break;
            case INS_GET_PASSWORD:
                getPassword(apdu);
                break;
            case INS_DELETE_PASSWORD:
                deletePassword(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }

    }

    private void getPassword(APDU apdu) {
        if (!userPin.isValidated()) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        byte location = buffer[ISO7816.OFFSET_CDATA];

        if ((short) location > passwordData.length) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }

        if (passwordData[location] == null) {
            ISOException.throwIt(SW_NO_PASSWORD);
        }

        byte[] toReturn = passwordData[location].data;
//        cipher.doFinal(toReturn, (short) 0, (short) (toReturn.length), buffer, buffer[ISO7816.OFFSET_CDATA]);

        Util.arrayCopyNonAtomic(toReturn, (short)0, buffer, (short)0, (short)toReturn.length);

        apdu.setOutgoing();

        apdu.setOutgoingLength((short)toReturn.length);

        apdu.sendBytes((short) 0, (short)toReturn.length);
    }


    private void savePassword(APDU apdu) {
        if (!userPin.isValidated()) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        byte[] passwordPosition = new byte[1];

        short lc = (short) (buffer[ISO7816.OFFSET_LC]);
        if (lc > (short)14) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH);

        for(short x = 0; x < passwordData.length; x++) {
            if (passwordData[x] == null) {
                passwordData[x] = new Password(lc);
                for (short y = 0; y < lc; y++) {
                    passwordData[x].data[y] = buffer[ISO7816.OFFSET_CDATA + y];
                    passwordPosition[0] = (byte) x;
                }
//                cipher.doFinal(passwordData[x].data, (short) 0, (short) (passwordData[x].data.length), buffer, buffer[ISO7816.OFFSET_CDATA]);
                break;
            }
        }

        Util.arrayCopyNonAtomic(passwordPosition, (short)0, buffer, (short)0, (short)passwordPosition.length);

        apdu.setOutgoing();

        apdu.setOutgoingLength((short)passwordPosition.length);

        apdu.sendBytes((short) 0, (short)passwordPosition.length);
    }

    private void deletePassword(APDU apdu) {
        if (!userPin.isValidated()) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        byte location = buffer[ISO7816.OFFSET_CDATA];

        if ((short) location > passwordData.length) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }

        if (passwordData[location] == null) {
            ISOException.throwIt(SW_NO_PASSWORD);
        }

        byte[] toReturn = passwordData[location].data;
//        cipher.doFinal(toReturn, (short) 0, (short) (toReturn.length), buffer, buffer[ISO7816.OFFSET_CDATA]);

        passwordData[location] = null;

        Util.arrayCopyNonAtomic(toReturn, (short)0, buffer, (short)0, (short)toReturn.length);

        apdu.setOutgoing();

        apdu.setOutgoingLength((short)toReturn.length);

        apdu.sendBytes((short) 0, (short)toReturn.length);
    }

    private void verifyPIN(APDU apdu) {
        byte[] apdubuf = apdu.getBuffer();
        short dataLen = apdu.setIncomingAndReceive();

        if (!userPin.check(apdubuf, ISO7816.OFFSET_CDATA, (byte) dataLen)) {
            if (userPin.getTriesRemaining() <= (byte) 0) {
                ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
            }
            ISOException.throwIt(SW_BAD_PIN);
        }
    }

    private void changePIN(APDU apdu) {
        byte[] apdubuf = apdu.getBuffer();
        short dataLen = apdu.setIncomingAndReceive();

        if (!userPin.isValidated()) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        userPin.update(apdubuf, ISO7816.OFFSET_CDATA, (byte) dataLen);
        userPin.check(apdubuf, ISO7816.OFFSET_CDATA, (byte) dataLen);
    }

    private void resetPIN(APDU apdu) {
        byte[] apdubuf = apdu.getBuffer();
        short dataLen = apdu.setIncomingAndReceive();

        if (adminPin.check(apdubuf, ISO7816.OFFSET_CDATA, (byte) dataLen) == false) {
            if (adminPin.getTriesRemaining() <= (byte) 0) {
                ISOException.throwIt(SW_APPLET_BLOCKED);
            }
            ISOException.throwIt(SW_BAD_PIN);
        }

        byte[] resetPIN = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        userPin.update(resetPIN, (byte) 0, (byte) 4);
        userPin.resetAndUnblock();
    }

}

