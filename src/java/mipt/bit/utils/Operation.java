package mipt.bit.utils;

public class Operation {
    public String type;
    // public String key;
    public Long key;
    public Long value;

    public Operation(String type, Long key, Long value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public Operation(String type, Long key) {
        this.type = type;
        this.key = key;
        this.value = null;
    }
}
