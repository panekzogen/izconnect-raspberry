import org.alljoyn.bus.*;

import java.util.logging.Level;

public abstract class AbstractNetworkService {

    private static final String TAG = "AllJoyn";

    protected static final String PACKAGE_NAME = "org.taom.izconnect.network";
    private static final String OBJECT_PATH = "/izconnectService";
    private static final short CONTACT_PORT = 4753;

    private BusAttachment mBus;
    private Observer boardObserver;
    private Observer mobileObserver;
    private Observer pcObserver;
    private AboutObj mAboutObj;
    private AboutDataListener mAboutData;

    static {
        System.load("/home/pi/Documents/izconnect/lib/liballjoyn_java.so");
    }

    public AbstractNetworkService() {
    }

    public Status doConnect() {
        Status status;
        mBus = new BusAttachment(PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);

        status = mBus.connect();
        if (Status.OK != status) {
            GFLogger.log(Level.SEVERE, TAG, "Cannot connect to bus", PACKAGE_NAME);
            return status;
        }

        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);

        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = false;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

        status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
            @Override
            public boolean acceptSessionJoiner(short sessionPort,
                                               String joiner, SessionOpts sessionOpts) {
                return sessionPort == CONTACT_PORT;
            }
        });
        if (status != Status.OK) {
            GFLogger.log(Level.SEVERE, TAG, "Cannot bind port", String.valueOf(CONTACT_PORT));
            return status;
        }

        return status;
    }

    public Status registerInterface(BusObject busObject) {
        Status status;
        status = mBus.registerBusObject(busObject, OBJECT_PATH);
        if (status != Status.OK) {
            GFLogger.log(Level.SEVERE, TAG, "Cannot register bus object");
            return status;
        }

        status = mBus.registerSignalHandlers(busObject);
        if (status != Status.OK) {
            GFLogger.log(Level.SEVERE, TAG, "Problem while registering signal handler");
            return status;
        }

        return status;
    }

    public void unregisterInterface(BusObject busObject) {
        mBus.unregisterSignalHandlers(busObject);
        mBus.unregisterBusObject(busObject);
    }

    public Status announce() {
        Status status;

        mAboutObj = new AboutObj(mBus);
        mAboutData = getAboutData();
        status = mAboutObj.announce(CONTACT_PORT, mAboutData);
        if (status != Status.OK) {
            GFLogger.log(Level.SEVERE, TAG, "Problem while sending about info");
            return status;
        }

        return status;
    }

    public void registerListeners() {
        if (mBus != null) {
//            AboutListener aboutListener = getAboutListener();
//            if (aboutListener != null) {
//                mBus.registerAboutListener(aboutListener);
//            }

            Observer.Listener boardListener = getBoardListener();
            if (boardListener != null) {
                boardObserver = new Observer(mBus, new Class[]{BoardInterface.class});
                boardObserver.registerListener(boardListener);
            }

            Observer.Listener mobileListener = getMobileListener();
            if (mobileListener != null) {
                mobileObserver = new Observer(mBus, new Class[]{MobileInterface.class});
                mobileObserver.registerListener(mobileListener);
            }

            Observer.Listener pcListener = getPCListener();
            if (pcListener != null) {
                pcObserver = new Observer(mBus, new Class[]{PCInterface.class});
                pcObserver.registerListener(pcListener);
            }


            mBus.whoImplements(getInterestingInterfaces());
        }
    }

    public void unregisterListeners() {
        if (mBus != null) {
//            mBus.unregisterAboutListener(getAboutListener());

            if (boardObserver != null) {
                boardObserver.unregisterListener(getBoardListener());
            }

            if (mobileObserver != null) {
                mobileObserver.unregisterListener(getMobileListener());
            }

            if (pcObserver != null) {
                pcObserver.unregisterListener(getPCListener());
            }

            mBus.cancelWhoImplements(getInterestingInterfaces());
        }
    }

    public void doDisconnect() {
        if (mBus != null) {
            unregisterListeners();
            mBus.disconnect();
        }
    }

    public String getBusName() {
        return mBus.getUniqueName();
    }

    protected abstract AboutDataListener getAboutData();

    protected abstract AboutListener getAboutListener();

    protected abstract Observer.Listener getMobileListener();

    protected abstract Observer.Listener getPCListener();

    protected abstract Observer.Listener getBoardListener();

    protected abstract String[] getInterestingInterfaces();
}