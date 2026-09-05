import java.util.List;

public class ReplicaNode {

    public static void main(String[] args) {

        ReplicaDatabase db =
                new ReplicaDatabase();

        long savedSequence =
                SequenceManager.loadSequence();

        db.setLastSequence(
                savedSequence);

        System.out.println(
                "Ultima secuencia: "
                        + savedSequence);

        EventProcessor processor =
                new EventProcessor(db);

        SyncClient client =
                new SyncClient();

        String respuesta =
                client.obtenerEventos(
                        db.getLastSequence());

        if(respuesta == null){

            System.out.println(
                    "\nNo se pudo conectar al líder.");

            return;
        }

        System.out.println(
                "\nRespuesta del líder:");

        System.out.println(
                respuesta);

        List<TransactionEvent> eventos =
                EventParser.parseAll(
                        respuesta);

        if(eventos.isEmpty()){

            System.out.println(
                    "\nNo hay eventos nuevos.");
        }

        for(TransactionEvent evento
                : eventos){

            processor.apply(
                    evento);

            System.out.println(
                    "\nEvento recibido desde líder: "
                            + evento.getSequence());
        }

        System.out.println(
                "\nUltima secuencia actual: "
                        + db.getLastSequence());

        System.out.println(
                "\nBalances después de sincronizar:");

        if(db.getAccounts().containsKey(1L)){

            System.out.println(
                    db.getAccounts()
                            .get(1L)
                            .getBalance());
        }

        if(db.getAccounts().containsKey(2L)){

            System.out.println(
                    db.getAccounts()
                            .get(2L)
                            .getBalance());
        }
    }
}