/**
 * Created by bkozyrev on 20.02.2017.
 */
public class ApiResponse {

    private String base;
    private String date;
    private RateObject rates;

    public String getBase() {
        return base;
    }

    public String getDate() {
        return date;
    }

    public RateObject getRates() {
        return rates;
    }

    @Override
    public String toString() {
        return base + " => " + rates.getName() + " : " + rates.getRate();
    }
}