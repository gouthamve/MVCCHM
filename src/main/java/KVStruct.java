/**
 * Created by goutham on 20/11/16.
 */
public class KVStruct{
    public int key;
    public int value;

    public KVStruct(){
        key=0;
        value=0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KVStruct kvStruct = (KVStruct) o;

        if (key != kvStruct.key) return false;
        return value == kvStruct.value;
    }
}
