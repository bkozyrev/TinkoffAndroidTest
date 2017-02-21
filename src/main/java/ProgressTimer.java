import java.util.TimerTask;

public class ProgressTimer extends TimerTask {

    @Override
    public void run() {
        System.out.print(".");
    }
}
