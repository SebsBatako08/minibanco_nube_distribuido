public class EventProcessor {

    private final AccountClient accountClient =
            new AccountClient();

    private final ReplicaDatabase database;

    public EventProcessor(
            ReplicaDatabase database) {

        this.database = database;
    }

    public synchronized void apply(
            TransactionEvent event) {

        if(event.getSequence()
                <= database.getLastSequence()) {

            return;
        }

        Account source =
                database.getAccounts()
                        .get(
                                event.getSourceAccountId());

        Account target =
                database.getAccounts()
                        .get(
                                event.getTargetAccountId());

        if(source == null){

            source =
                    accountClient.obtenerCuenta(
                            event.getSourceAccountId());

            if(source != null){

                database.getAccounts().put(
                        source.getId(),
                        source);
            }
        }

        if(target == null){

            target =
                    accountClient.obtenerCuenta(
                            event.getTargetAccountId());

            if(target != null){

                database.getAccounts().put(
                        target.getId(),
                        target);
            }
        }

        // DEBUG
        if(source == null){

            System.out.println(
                    "No se pudo cargar cuenta origen: "
                            + event.getSourceAccountId());
        }

        if(target == null){

            System.out.println(
                    "No se pudo cargar cuenta destino: "
                            + event.getTargetAccountId());
        }

        if(source == null || target == null){

            System.out.println(
                    "Evento problemático: "
                            + event.getSequence());

            return;
        }

        source.setBalance(
                source.getBalance()
                        - event.getAmount());

        target.setBalance(
                target.getBalance()
                        + event.getAmount());

        database.setLastSequence(
                event.getSequence());

        SequenceManager.saveSequence(
                event.getSequence());

        System.out.println(
                "Evento aplicado: "
                        + event.getSequence());
    }
}