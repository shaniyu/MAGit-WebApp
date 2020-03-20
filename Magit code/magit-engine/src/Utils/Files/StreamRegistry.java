package Utils.Files;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamRegistry {

    public static List<Closeable> streams = new ArrayList<>();

    public static void register(Closeable closeable) {
        streams.add(closeable);
    }

    public static void close() throws IOException {
        for(Closeable closeable : streams) {
            closeable.close();
        }
    }
}
