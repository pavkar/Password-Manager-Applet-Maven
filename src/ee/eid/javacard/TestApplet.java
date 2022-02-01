/**
 * 
 */
package ee.eid.javacard;

import javacard.framework.*;
import javacardx.annotations.*;

/**
 * Applet class
 * 
 * @author <user>
 */
@StringPool(value = {
	    @StringDef(name = "Package", value = "ee.eid.javacard.pack"),
	    @StringDef(name = "AppletName", value = "TestApplet")},
	    // Insert your strings here 
	name = "TestAppletStrings")
public class TestApplet extends Applet {
	
	 //Define the value of CLA/INS in APDU, you can also define P1, P2.
	 private static final byte CLA_DEMO_TEST = (byte)0xB0;
	 private static final byte INS_DEMO_TMP1 = (byte)0x11;
	 private static final byte INS_DEMO_TMP2 = (byte)0x12;

  /**
   * Installs this applet.  
   * Create an instance of the 
   * Applet subclass using its constructor, 
   * and to register the instance.
   * 
   * @param bArray
   *            the array containing installation parameters
   * @param bOffset
   *            the starting offset in bArray
   * @param bLength
   *            the length in bytes of the parameter data in bArray
   */
   public static void install(byte[] bArray, short bOffset, byte bLength) {
       new TestApplet().register();
   }

   /**
    * Processes an incoming APDU. Process the command APDU, 
    * All APDUs are received by the JCRE and preprocessed. 
    * 
    * @see APDU
    * @param apdu
    *            the incoming APDU
    */
   @Override
   public void process(APDU apdu) {
		//Select the Applet, through the select method, this applet is selectable, After successful selection, all APDUs are delivered to the currently selected applet via the process method.
		if (selectingApplet()) {
		   return;
		}
		//Get the APDU buffer byte array.
		byte[] buffer = apdu.getBuffer();
		//Calling this method indicates that this APDU has incoming data. 
		apdu.setIncomingAndReceive();
		
		//If the CLA is not equal to 0xB0(CLA_DEMO_TEST),  throw an exception.
		if(buffer[ISO7816.OFFSET_CLA] != CLA_DEMO_TEST) {
		   ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		//Dispatch INS in APDU.
		switch (buffer[ISO7816.OFFSET_INS]) {
		   //The APDU format "B0110000" or "B01101020311223300".
			case INS_DEMO_TMP1:
			   sendData(apdu);
			   break;
			   //The APDU format "B0125566" or "B012000002112200".
			case INS_DEMO_TMP2:
			   //Set 0x5555 into the 'buffer' array,  the offset is 0.
			   Util.setShort(buffer, (short)0, (short)0x5555);
			   //Send the first 2 bytes data in 'buffer', the hex of JCRE sending data is "55 55 90 00".
			   apdu.setOutgoingAndSend((short)0, (short)2);
			   break;
			default:
			   //If you don't know the INS, throw an exception.
			   ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
   }
   
   //Define a function named 'SendData'
   private void sendData(APDU apdu) {
		//Get the apdu buffer datas again.
		byte [] buffer = apdu.getBuffer();
		   
		//Define an answer byte string
		byte answerString[] = {'H','E','L','L','O',' ','W','O','R','L','D',' ','T','E','S','T'};
		//Copy answerString character to APDU Buffer.
		Util.arrayCopyNonAtomic(answerString, (short)0, buffer, (short)0, (short)answerString.length);
		      
		//Set the data transfer direction to outbound.
		apdu.setOutgoing();
		//Set the actual length of response data.
		apdu.setOutgoingLength((short)answerString.length);
		//Sends the data of APDU buffer 'buf', the length is 'len' bytes,  the offset is 0.
		apdu.sendBytes((short) 0, (short)answerString.length);
   }
}
