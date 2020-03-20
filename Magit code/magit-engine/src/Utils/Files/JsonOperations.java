package Utils.Files;

import com.google.gson.Gson;

import java.io.PrintWriter;

public class JsonOperations {

    public static void printToOut(Object o, PrintWriter out)
    {
        Gson gson = new Gson();
        String json = gson.toJson(o);
        System.out.println(json);
        out.print(json);
    }
}
