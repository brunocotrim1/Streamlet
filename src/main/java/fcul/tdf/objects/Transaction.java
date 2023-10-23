package fcul.tdf.objects;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Transaction implements java.io.Serializable {
    private int sender;
    private int receiver;
    private int id;
    private int amount;
}
