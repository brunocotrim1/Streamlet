package fcul.tdf.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Objects;

@Data
@ToString
@AllArgsConstructor
public class Transaction implements java.io.Serializable {
    private int sender;
    private int receiver;
    private int id;
    private int amount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return sender == that.sender && receiver == that.receiver && id == that.id && amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, receiver, id, amount);
    }

    public String toString() {
        return String.valueOf(id);
    }
}
