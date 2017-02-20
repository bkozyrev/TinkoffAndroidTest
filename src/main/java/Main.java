import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bkozyrev on 20.02.2017.
 */
public class Main {

    private static final String API_URL = "http://api.fixer.io/latest?base=%1$s&symbols=%2$s";
    private static final String FROM_CURRENCY_DISPLAY_TEXT = "Enter from currency:";
    private static final String TO_CURRENCY_DISPLAY_TEXT = "Enter to currency:";
    private static final String CACHE_READ_SUCCESS_DISPLAY_TEXT = "Got data from cache";
    private static final String RESPONSE_SUCCESS_DISPLAY_TEXT = "Got data from web";
    private static final String WRONG_CURRENCY_DISPLAY_TEXT = "Sorry, this is wrong currency format. Try again.";
    private static final String WRONG_URL_EXCEPTION_TEXT = "Wrong url.";
    private static final String EXCEPTION_TEXT = "Ops, something went wrong.";
    private static final String SAVE_FILE_EXCEPTION_TEXT = "Error while saving file.";
    private static final String DIRECTORY_CREATE_EXCEPTION_TEXT = "Ops, something went wrong.";

    private static Gson gson;

    public static void main(String[] args) {

        createGsonBuilder();

        final String fromCurrency = enterCurrency(FROM_CURRENCY_DISPLAY_TEXT);
        final String toCurrency = enterCurrency(TO_CURRENCY_DISPLAY_TEXT);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        //ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        final Timer timer = new Timer();
        timer.schedule(new ProgressTimer(), 0, 250);

        executorService.submit(new Runnable() {
            public void run() {
                String cachedData = getDataFromFile(fromCurrency, toCurrency);

                if (cachedData != null) {
                    System.out.println(cachedData);
                } else {
                    ApiResponse apiResponse = performRequest(fromCurrency, toCurrency);

                    if (apiResponse != null) {
                        System.out.println(apiResponse.toString());
                        saveDataToFile(apiResponse.toString(), fromCurrency, toCurrency);
                    } else {
                        System.out.println(EXCEPTION_TEXT);
                    }
                }

                timer.cancel();
            }
        });

        /*executorService.schedule(new Runnable() {
            public void run() {
                String cachedData = getDataFromFile(fromCurrency, toCurrency);

                if (cachedData != null) {
                    System.out.println(cachedData);
                } else {
                    ApiResponse apiResponse = performRequest(fromCurrency, toCurrency);

                    if (apiResponse != null) {
                        System.out.println(apiResponse.toString());
                        saveDataToFile(apiResponse.toString(), fromCurrency, toCurrency);
                    } else {
                        System.out.println(EXCEPTION_TEXT);
                    }
                }

                timer.cancel();
            }
        }, 1, TimeUnit.SECONDS);*/

        executorService.shutdown();
    }

    private static void createGsonBuilder() {
        gson = new GsonBuilder()
                .registerTypeAdapter(RateObject.class, new RatesDeserializer())
                .create();
    }

    private static void saveDataToFile(String data, String fromCurrency, String toCurrency) {
        PrintWriter printWriter = null;

        try {
            File file = new File(getCachedFileName(fromCurrency, toCurrency));
            if(!file.getParentFile().exists() && !file.getParentFile().mkdirs()){
                throw new IllegalStateException(DIRECTORY_CREATE_EXCEPTION_TEXT);
            }
            printWriter = new PrintWriter(getCachedFileName(fromCurrency, toCurrency));
            printWriter.println(data);
        } catch (FileNotFoundException exception) {
            System.out.println(SAVE_FILE_EXCEPTION_TEXT);
        } catch (IllegalStateException exception) {
            System.out.println(exception.getMessage());
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    private static String getDataFromFile(String fromCurrency, String toCurrency) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(getCachedFileName(fromCurrency, toCurrency)));

            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }

            br.close();
            System.out.println("\n" + CACHE_READ_SUCCESS_DISPLAY_TEXT);
            return sb.toString();
        } catch (FileNotFoundException exception) {
            return null;
        } catch (IOException exception) {
            return null;
        }
    }

    private static String getCachedFileName(String fromCurrency, String toCurrency) {
        return "cache/" + fromCurrency + "-" + toCurrency + ".txt";
    }

    private static String enterCurrency(String displayText) {
        Scanner consoleInput = new Scanner(System.in);
        String currency;

        do {
            System.out.println(displayText);
            currency = consoleInput.nextLine().toUpperCase();

            if (!isValidCurrencyEnum(currency)) {
                System.out.println(WRONG_CURRENCY_DISPLAY_TEXT);
            }
        } while (!isValidCurrencyEnum(currency));

        return currency;
    }

    private static boolean isValidCurrencyEnum(String currency) {
        try {
            CurrencyEnum.valueOf(currency);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return true;
    }

    private static ApiResponse performRequest(String fromCurrency, String toCurrency) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(String.format(API_URL, fromCurrency, toCurrency));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.connect();

            int status = connection.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    ApiResponse apiResponse = gson.fromJson(br, ApiResponse.class);
                    br.close();
                    System.out.println("\n" + RESPONSE_SUCCESS_DISPLAY_TEXT);
                    return apiResponse;
            }

        } catch (MalformedURLException exception) {
            System.out.println(WRONG_URL_EXCEPTION_TEXT);
        } catch (IOException exception) {
            System.out.println(EXCEPTION_TEXT);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }
}
