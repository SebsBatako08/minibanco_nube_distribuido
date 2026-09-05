import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AccountClient {

    private static final String TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvX2NvbmN1cnNvIiwiZXhwIjoxNzgyMjkzMzE4fQ.mfhsaen7NiBAR1wj5otzDLW3l7rXTCmmUzyDcZtgtY0";

    public Account obtenerCuenta(long id) {

        try {

            URL url =
                    new URL(
                            "http://35.255.123.144:8080/api/accounts/"
                                    + id);

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

            String json =
                    response.toString();

            long accountId =
                    Long.parseLong(
                            json.split("\"id\":")[1]
                                    .split(",")[0]
                                    .trim());

            String owner =
                    json.split("\"propietario\":")[1]
                            .split(",")[0]
                            .replace("\"", "")
                            .trim();

            double balance =
                    Double.parseDouble(
                            json.split("\"balance\":")[1]
                                    .replace("}", "")
                                    .trim());

            return new Account(
                    accountId,
                    owner,
                    balance);

        } 
        catch (Exception e) {

    System.out.println(
            "Error obteniendo cuenta: "
                    + id);

    e.printStackTrace();

    return null;
            }
        }
    }
