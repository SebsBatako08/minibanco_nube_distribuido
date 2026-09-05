public class TransactionEvent {

    private long sequence;

    private long sourceAccountId;

    private long targetAccountId;

    private double amount;

    private long timestamp;

    public TransactionEvent(
            long sequence,
            long sourceAccountId,
            long targetAccountId,
            double amount,
            long timestamp) {

        this.sequence = sequence;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public long getSequence() {
        return sequence;
    }

    public long getSourceAccountId() {
        return sourceAccountId;
    }

    public long getTargetAccountId() {
        return targetAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}