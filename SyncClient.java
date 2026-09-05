import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SyncClient {

    private static final String TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvX2NvbmN1cnNvIiwiZXhwIjoxNzgyMjkzMzE4fQ.mfhsaen7NiBAR1wj5otzDLW3l7rXTCmmUzyDcZtgtY0";

    public String obtenerEventos(long from) {

        try {

            URL url =
                    new URL(
                            "http://35.255.123.144:8080/api/sync?from="
                                    + from);

            HttpURLConnection connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + TOKEN);

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()));

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                response.append(line);
            }

            reader.close();

            return response.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }
}