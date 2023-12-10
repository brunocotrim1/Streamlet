package fcul.tdf.objects;

import fcul.tdf.enums.Type;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@ToString
public class Message implements Serializable {
    Object content;
    Type type;
    int sender;
    long sequence;
    Object additionalInfo;
    boolean isReconnected;
}
