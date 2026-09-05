import java.util.ArrayList;
import java.util.List;

public class EventParser {

    public static List<TransactionEvent> parseAll(String json) {

        List<TransactionEvent> eventos =
                new ArrayList<>();

        try {

            String[] bloques =
                    json.split("\\{");

            for (String bloque : bloques) {

                if (!bloque.contains("secuencia")) {
                    continue;
                }

                long secuencia =
                        Long.parseLong(
                                bloque.split("\"secuencia\":")[1]
                                        .split(",")[0]
                                        .trim());

                long origen =
                        Long.parseLong(
                                bloque.split("\"origen\":")[1]
                                        .split(",")[0]
                                        .replace("\"", "")
                                        .trim());

                long destino =
                        Long.parseLong(
                                bloque.split("\"destino\":")[1]
                                        .split(",")[0]
                                        .replace("\"", "")
                                        .trim());

                double monto =
                        Double.parseDouble(
                                bloque.split("\"monto\":")[1]
                                        .split(",")[0]
                                        .trim());

                long timestamp =
                        Long.parseLong(
                                bloque.split("\"timestamp\":")[1]
                                        .split("}")[0]
                                        .replace("]", "")
                                        .replace(",", "")
                                        .trim());

                eventos.add(
                        new TransactionEvent(
                                secuencia,
                                origen,
                                destino,
                                monto,
                                timestamp));
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return eventos;
    }
}
