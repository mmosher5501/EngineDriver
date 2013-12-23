
package jmri.jmrix.loconet;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;


import junit.framework.*;


/**
 * Generated by JBuilder
 * <p>Title: LnTrafficControllerTest </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * @author Bob Jacobsen
 * @version $Id$
 */
public class LnTrafficControllerTest extends TestCase {

  public LnTrafficControllerTest(String s) {
    super(s);
  }

    public void testNull() {
        // just to make JUnit feel better
    }
    
    private class LnTrafficListenerTestStub implements LnTrafficListener {
    	// set to true if the LocoNetListener receives a message
    	public boolean listenResultRcv = false;
    	// set to true if the LocoNetListener receives the echo of a sent message
        public boolean listenResultXmit = false;
        
        //public Date listenTimestamp = new Date(0);
        
        // reset values
        public void reset() {
        	listenResultRcv = false;
            listenResultXmit = false;
            //listenTimestamp = new Date(0);
        }
		public void notifyXmit(Date timestamp, LocoNetMessage m) {
			listenResultXmit = true;
			//listenTimestamp = timestamp;
		}
		public void notifyRcv(Date timestamp, LocoNetMessage m) {
			listenResultRcv = true;
			//listenTimestamp = timestamp;
		}
    }

    /** This class implements a simple test stub for LocoNetListener to test 
     * notifications of loconet messages.
     * An instance of this class is used to receive the notifications of 
     * the LnTrafficController. 
     * @author Matthias Keil
     *
     */
    private class LocoNetListenerTestStub implements LocoNetListener {
    	// set to true if the LocoNetListener receives a message
    	public boolean listenResultRcv = false;
    	// set to true if the LocoNetListener receives the echo of a sent message
        public boolean listenResultXmit = false;
        // reset values
        public void reset() {
        	listenResultRcv = false;
            listenResultXmit = false; }
		public void message(LocoNetMessage msg) {
			// we recognize the type of the message by size
			if (msg.getNumDataElements() == 4) {
				// 4 byte message used to test receive of a message
				listenResultRcv = true; }
			if (msg.getNumDataElements() == 2) {
				// 2 byte message used to test transmit of a message
				listenResultXmit = true; }
		}    	
    }
    
    /** This class implements a simple test driver to stimulate the receiving 
     * and sending of messages with a LnTrafficController.
     * @author Matthias Keil
     *
     */
    private class LnPortControllerTestDriver extends LnPortController {
    	/** This class is a test driver for input stream.
    	 * As there is no physical device available, the input stream is simulated.
    	 * LocoNetMessages can be put into the input stream by calling the 
    	 * injectLnMessage method. These messages should than be seen by the 
    	 * LnTrafficController as messages received from the loconet. 
    	 * @author matthiaskeil
    	 *
    	 */
    	private class InputStreamTestDriver extends InputStream {

    		// default
			public InputStreamTestDriver() {
				super(); }
			
			/** the actual input stream. */
			ArrayList<Byte> inStream = new ArrayList<Byte>();
			
			/** Where we are in the input stream. */
			int streamPos = 0;
			
			/** Put the test message in to the input stream array. */
			synchronized public void injectLnMessage(LocoNetMessage m) {
				for (int index = 0; index < m.getNumDataElements(); index++) {
					inStream.add((byte)m.getElement(index)); } }
			
			/** Implementation of the InputStream read() method.
			 * Returns -1 if there are no new bytes in the array, i.e., 
			 * we are at the last element in the array.
			 * Returns the value of the next byte in the array.
			 */
			@Override
			synchronized public int read() throws IOException {
				int result = -1;
				if (streamPos < inStream.size()) {
					result = inStream.get(streamPos);
					streamPos++; }
				return result; }
    		
    	}
    	/** Constructor initialises the and output streams. */ 
    	public LnPortControllerTestDriver() {
    		receiveStream = new InputStreamTestDriver();
    		sendStream = new ByteArrayOutputStream(500);
    	}
    	
    	// dummy
		public String openPort(String portName, String appName) { return appName;	}
		
		// dummy
		public void configure() {}

		// dummy implementation for baud rates, actual values don't care
		String [] rates = { "9786" };
		public String[] validBaudRates() { return rates; }

		/** Adds a loconet message to the input stream. */
		public void injectMessage(LocoNetMessage m) {
			receiveStream.injectLnMessage(m); }
		
		/** Return the test drivers stream for receive messages. 
		 * The object under test will call this method. */
		InputStreamTestDriver receiveStream = null;
		public DataInputStream getInputStream() {
			return new DataInputStream(receiveStream);	}

		/** Return the test drivers stream for transit messages.
		 * The object under test will call this method. */
		ByteArrayOutputStream sendStream = null;
		public DataOutputStream getOutputStream() {
			return new DataOutputStream(sendStream); }

		// dummy
		public boolean status() { return true; }
    	
    }
    /** This tests that LnTrafficController will receive and transmit LoconetMessages and
     * notify registered LocoNetListeners.
     */
    public void testSendAndReceiveLocoNetListener() {
    	// create a test message that will be injected to our port controller
    	// we should see this message later as a received message
    	LocoNetMessage mRcv = new LocoNetMessage(4);
    	mRcv.setElement(0, 0xBF);
    	mRcv.setElement(1, 0x45);
    	mRcv.setElement(2, 0x00);
    	mRcv.setParity();
    	
    	// create the port controller test driver
    	LnPortControllerTestDriver pc = new LnPortControllerTestDriver();

    	// create our LocoNetListener stub
    	LocoNetListenerTestStub listen = new LocoNetListenerTestStub(); 
    	
    	/* 
    	 * create and initialise the object under test
    	 */

    	// create traffic controller
    	// LnPacketizer is used to test the LnTrafficController
    	LnPacketizer tc = new LnPacketizer();

    	// connect it to our test driver
    	tc.connectPort(pc);
    	
    	// register the test listener
    	tc.addLocoNetListener(~0, listen);
    	
    	// run it
    	tc.startThreads();
    	
    	/*
    	 * stimulate receiving of a message
    	 */
    	pc.injectMessage(mRcv);
    	

    	try {
    		// give the system some time to do it's work
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	// check if we got notified of the received message, not the transmit one
    	Assert.assertTrue(listen.listenResultRcv);
    	Assert.assertFalse(listen.listenResultXmit);

    	listen.reset();
    	
    	/*
    	 * stimulate the sending of a message
    	 */
    	
    	// send a loconet message which should result in a 
    	// notification to our test listener, too
    	LocoNetMessage mTx = new LocoNetMessage(2);
    	mTx.setElement(0, 0x85);
    	tc.sendLocoNetMessage(mTx);
    	
    	try {
    		// give the system some time to do it's work
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	// check if we got the notification for the transmit message
    	Assert.assertTrue(listen.listenResultXmit);
    	Assert.assertFalse(listen.listenResultRcv);
    }
        
    /** This tests that LnTrafficController will receive and transmit LoconetMessages and
     * notify registered LocoNetListeners.
     */
    public void testSendAndReceiveLnTrafficListener() {
    	// create a test message that will be injected to our port controller
    	// we should see this message later as a received message
    	LocoNetMessage mRcv = new LocoNetMessage(4);
    	mRcv.setElement(0, 0xBF);
    	mRcv.setElement(1, 0x45);
    	mRcv.setElement(2, 0x00);
    	mRcv.setParity();
    	
    	// create the port controller test driver
    	LnPortControllerTestDriver pc = new LnPortControllerTestDriver();

    	// create our LocoNetListener stub
    	LnTrafficListenerTestStub listen = new LnTrafficListenerTestStub(); 
    	
    	/* 
    	 * create and initialise the object under test
    	 */

    	// create traffic controller
    	// LnPacketizer is used to test the LnTrafficController
    	LnPacketizer tc = new LnPacketizer();

    	// connect it to our test driver
    	tc.connectPort(pc);
    	
    	// register the test listener for receive and transmit messages
    	tc.addTrafficListener(LnTrafficListener.LN_TRAFFIC_ALL, listen);
    	
    	// run it
    	tc.startThreads();
    	
    	/*
    	 * stimulate receiving of a message
    	 */
    	pc.injectMessage(mRcv);
    	

    	try {
    		// give the system some time to do it's work
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	// check if we got notified of the received message, not the transmit one
    	Assert.assertTrue(listen.listenResultRcv);
    	Assert.assertFalse(listen.listenResultXmit);

    	listen.reset();
    	
    	/*
    	 * stimulate the sending of a message
    	 */
    	
    	// send a loconet message which should result in a 
    	// notification to our test listener, too
    	LocoNetMessage mTx = new LocoNetMessage(2);
    	mTx.setElement(0, 0x85);
    	tc.sendLocoNetMessage(mTx);
    	
    	try {
    		// give the system some time to do it's work
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	// check if we got the notification for the transmit message
    	Assert.assertTrue(listen.listenResultXmit);
    	Assert.assertFalse(listen.listenResultRcv);

    	listen.reset();
    	
    	/*
    	 * change filter and check that there is no receive message notification
    	 */
    	tc.changeTrafficListener(LnTrafficListener.LN_TRAFFIC_TX, listen);
    	pc.injectMessage(mRcv);
    	

    	try {
    		// give the system some time to do it's work
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	// check if we got notified of the received message, not the transmit one
    	Assert.assertFalse(listen.listenResultRcv);
    	Assert.assertFalse(listen.listenResultXmit);

    	listen.reset();
    	
    	/*
    	 * check that transmit messages still get through
    	 */
    	
    	// send a loconet message which should result in a 
    	// notification to our test listener, too
    	LocoNetMessage mTx2 = new LocoNetMessage(2);
    	mTx2.setElement(0, 0x85);
    	tc.sendLocoNetMessage(mTx2);
    	
    	try {
    		// give the system some time to do it's work
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	// check if we got the notification for the transmit message
    	Assert.assertTrue(listen.listenResultXmit);
    	Assert.assertFalse(listen.listenResultRcv);

    	listen.reset();
    }

    // Main entry point
	static public void main(String[] args) {
		String[] testCaseName = {LnTrafficControllerTest.class.getName()};
		junit.swingui.TestRunner.main(testCaseName);
	}

	// test suite from all defined tests
	public static Test suite() {
		TestSuite suite = new TestSuite(LnTrafficControllerTest.class);
		return suite;
	}

    // The minimal setup for log4J
    protected void setUp() { apps.tests.Log4JFixture.setUp(); }
    protected void tearDown() { apps.tests.Log4JFixture.tearDown(); }

}
