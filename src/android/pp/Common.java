package android.pp;

public class Common {
	
	/**
     * REQUESTS TO SERVER GG
    */
    static final int GG_USERLIST_REQUEST80 = 0x002f;
    static final int GG_LOGIN80 = 0x0031;
    static final int GG_LIST_EMPTY = 0x0012;
    
    /**
     * STATES 
     */
    static final int GG_STATUS_AVAIL_DESCR = 0x0004;
    /**
     * REPLIES FROM SERVER GG
    */
    static final int GG_WELCOME = 0x0001;
    static final int GG_USERLIST_REPLY80 = 0x0030;
    static final int GG_LOGIN_OK80 = 0x0035;
    static final int GG_LOGIN_FAILED = 0x0009;

    /**
     * CLASS FLAGS
    */
    static final int FLAG_ACTIVITY_REGISTER = 10;
    static final int FLAG_CONTACTBOOK = 11;
    
    /**
     * IMPORT/EXPORT CONTACTBOOK TYPES
    */
    static final byte GG_USERLIST_GET=  0x02; //import listy
    
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int CLIENT_REGISTER = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int CLIENT_UNREGISTER = 2;
    
    /**
     * Command to service to login user.
    */
   static final int CLIENT_LOGIN = 3;
   /**
    * Command to service to download a contactbook from server.
   */
   static final int CLIENT_GET_CONTACTBOOK = 4;
   
   static final int CLIENT_START_INTENT_CONTACTBOOK = 5;
}
