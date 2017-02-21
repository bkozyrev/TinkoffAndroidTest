import Deserializer.RatesDeserializer;
import Model.ApiResponse;
import Model.RateObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final String DIRECTORY_CREATE_EXCEPTION_TEXT = "Error in creating cache directory.";

    private static Gson gson;

    public static void main(String[] args) {

        createGson();

        final String fromCurrency = enterCurrency(FROM_CURRENCY_DISPLAY_TEXT);
        final String toCurrency = enterCurrency(TO_CURRENCY_DISPLAY_TEXT);

        //Executor service for performing async task
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        //TimerTask, that displays a dot, while data is loading
        final Timer timer = new Timer();
        timer.schedule(new ProgressTimer(), 0, 250);

        executorService.submit(new Runnable() {
            public void run() {

                //Firstly, trying to get data from cache
                String cachedData = getDataFromFile(fromCurrency, toCurrency);

                if (cachedData != null) {
                    //If cache exists, show data to user
                    System.out.println(cachedData);
                } else {
                    //If cache doesn't exist, perform api request
                    ApiResponse apiResponse = performRequest(fromCurrency, toCurrency);

                    if (apiResponse != null) {
                        //If data was loaded from server successfully, print it to user and save it to cache
                        System.out.println(apiResponse.toString());
                        saveDataToFile(apiResponse.toString(), fromCurrency, toCurrency);
                    } else {
                        //In all other "bad" cases display error message
                        System.out.println(EXCEPTION_TEXT);
                    }
                }

                //Cancel timer task after job is done
                timer.cancel();
            }
        });

        //Finally, shutdown an executor service
        executorService.shutdown();
    }

    /**
     * Initializing Gson and registering deserializer
     */
    private static void createGson() {
        gson = new GsonBuilder()
                .registerTypeAdapter(RateObject.class, new RatesDeserializer())
                .create();
    }

    /**
     * Providing simple cache.
     * Saves loaded data to "~/cache" folder. If folder doesn't exist, create it.
     * @param data          string, that has to be displayed to user later
     * @param fromCurrency  used to make an unique file name
     * @param toCurrency    used to make an unique file name
     */
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
            System.err.println(SAVE_FILE_EXCEPTION_TEXT);
        } catch (IllegalStateException exception) {
            System.err.println(exception.getMessage());
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    /**
     * Getting string from cache
     * @param fromCurrency  used to find a file name
     * @param toCurrency    used to find a file name
     * @return  string, that will be displayed to user. If such file doesn't exists, return null.
     */
    private static String getDataFromFile(String fromCurrency, String toCurrency) {
        BufferedReader br;

        //Here we can use try-with-resources statement, but on Java versions < 1.7 it won't work
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

    /**
     * @return destination to cache file
     */
    private static String getCachedFileName(String fromCurrency, String toCurrency) {
        return "cache/" + fromCurrency + "-" + toCurrency + ".txt";
    }

    /**
     * Method for getting currency string from user input
     * @param displayText   according text, that will be displayed as a prompt
     * @return  valid currency string
     */
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

    /**
     * Check for valid currency input, using CurrencyEnum
     * @param currency  string, that has to be checked
     * @return  true if currency string is valid, false otherwise
     */
    private static boolean isValidCurrencyEnum(String currency) {
        try {
            CurrencyEnum.valueOf(currency);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return true;
    }

    /**
     * Perform "Get" request to Api method
     * @return  ApiResponse object parsed from json response
     */
    private static ApiResponse performRequest(String fromCurrency, String toCurrency) {
        HttpURLConnection connection = null;

        //Again, here we can use try-with-resources statement, but on Java versions < 1.7 it won't work
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
            System.err.println(WRONG_URL_EXCEPTION_TEXT);
        } catch (IOException exception) {
            System.err.println(EXCEPTION_TEXT);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }
}
