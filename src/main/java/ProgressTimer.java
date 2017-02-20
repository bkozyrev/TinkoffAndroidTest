import java.util.TimerTask;

/**
 * Created by bkozyrev on 20.02.2017.
 */
public class ProgressTimer extends TimerTask {

    @Override
    public void run() {
        System.out.print(".");
    }
}
