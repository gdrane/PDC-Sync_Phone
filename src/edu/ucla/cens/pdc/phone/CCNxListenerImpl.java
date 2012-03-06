package edu.ucla.cens.pdc.phone;

import java.io.IOException;
import java.security.PublicKey;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.impl.CCNFlowControl.SaveType;
import org.ccnx.ccn.io.content.PublicKeyObject;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.KeyLocator;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;

import android.util.Log;

public class CCNxListenerImpl implements CCNInterestHandler {
	
	private CCNxListenerImpl () {
		
	}
	
	public static CCNxListenerImpl getInstance() {
		if(_listener == null) 
			_listener = new CCNxListenerImpl();
		return _listener;
	}
	

	@Override
	public boolean handleInterest(Interest interest) {
		try {
			ContentName postfix = interest.name().postfix(
					ContentName.fromNative("/ndn/ucla.edu/apps"));
			Log.i(TAG, "Received interest " + interest);
			if(postfix.stringComponent(1).equals("key")){
				CCNHandle handle = NDNManager.getInstance().getCCNHandle();
				KeyManager keymgr = handle.keyManager();
				final PublisherPublicKeyDigest digest = keymgr.getDefaultKeyID();
				final PublicKey key = keymgr.getPublicKey(digest);
				final KeyLocator locator = keymgr.getKeyLocator(digest);	
				final CCNTime version = new CCNTime();
				PublicKeyObject pko = new PublicKeyObject(interest.name(), key, 
						SaveType.RAW, digest, locator, handle);
				pko.disableFlowControl();
				pko.save(version);
			}
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static CCNxListenerImpl _listener;
	
	private final String TAG = "CCNxListenerImpl";

}
