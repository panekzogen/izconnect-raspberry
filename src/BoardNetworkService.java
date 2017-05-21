import org.alljoyn.bus.AboutDataListener;
import org.alljoyn.bus.AboutListener;
import org.alljoyn.bus.Observer;

public class BoardNetworkService extends AbstractNetworkService {
    @Override
    protected AboutDataListener getAboutData() {
        return new BoardAboutData();
    }

    @Override
    protected AboutListener getAboutListener() {
        return null;
    }

    @Override
    protected Observer.Listener getMobileListener() {
        return null;
    }

    @Override
    protected Observer.Listener getPCListener() {
        return null;
    }

    @Override
    protected Observer.Listener getBoardListener() {
        return null;
    }

    @Override
    protected String[] getInterestingInterfaces() {
        return new String[]{PACKAGE_NAME + ".izconnect."};
    }
}
