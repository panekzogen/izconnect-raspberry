import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.pi4j.io.gpio.*;

public class BoardServiceImpl implements BusObject, BoardInterface {
    private static final File IZCONNECT_FOLDER = new File(System.getProperty("user.home"), "izconnect");
    private Map<String, FileOutputStream> incomingFiles = new ConcurrentHashMap<>();

    private final GpioPinDigitalOutput ledPin;
    private final GpioPinDigitalInput motionPin;

    private boolean autoMode = false;
    private boolean motionDetected = false;

    public BoardServiceImpl() {
        final GpioController gpio = GpioFactory.getInstance();
        ledPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_21, PinState.HIGH);
        ledPin.setShutdownOptions(true, PinState.LOW);

        motionPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_29, PinPullResistance.PULL_DOWN);
        motionPin.setShutdownOptions(true);
        motionPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                motionDetected = event.getState().isHigh();
            }

        });

        Timer timer = new Timer(false);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (autoMode) {
                    if (motionDetected) {
                        ledPin.high();
                    } else {
                        ledPin.low();
                    }
                }
            }
        }, 0, 3000);
    }

    @Override
    public String getDeviceName() throws BusException {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return  "Board";
        }
    }

    @Override
    public String getDeviceOS() throws BusException {
        return System.getProperty("os.name");
    }

    @Override
    public void setLight(boolean turnOn) throws BusException {
        if (turnOn) {
            ledPin.high();
        } else {
            ledPin.low();
        }
    }

    @Override
    public boolean getLight() throws BusException {
        return false;
    }

    @Override
    public void setAutoMode(boolean autoMode) throws BusException {
        this.autoMode = autoMode;
    }

    @Override
    public boolean getAutoMode() throws BusException {
        return false;
    }

    @Override
    public void fileData(String filename, byte[] data, boolean isScript) throws BusException {
        if (data.length == 0) {
            FileOutputStream out = incomingFiles.get(filename);
            if (out != null) {
                incomingFiles.remove(filename);
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            File root = new File(IZCONNECT_FOLDER, isScript ? "scripts" : "");
            if(!root.exists()) {
                root.mkdirs();
            }
            File received = new File(root, filename);

            FileOutputStream out = incomingFiles.get(filename);
            if (out == null) {
                if (received.exists()) {
                    received.delete();
                }
                try {
                    out = new FileOutputStream(received,true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                incomingFiles.put(filename, out);
            }
            try {
                out.write(data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void runScript(String scriptName) throws BusException {
        Runtime rt = Runtime.getRuntime();
        String scriptPath = new File(IZCONNECT_FOLDER, "scripts/" + scriptName).getAbsolutePath();
        try {
            rt.exec(new String[] {"chmod", "+x", scriptPath});
            rt.exec(new String[] {"sh", scriptPath});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
