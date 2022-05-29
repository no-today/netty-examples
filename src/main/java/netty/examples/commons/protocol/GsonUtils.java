package netty.examples.commons.protocol;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

/**
 * @author no-today
 * @date 2022/05/29 15:59
 */
public class GsonUtils {

    private static final Gson gson = new Gson();

    public static byte[] encode(Object object) {
        return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T decode(byte[] bytes, Class<T> clazz) {
        return gson.fromJson(new String(bytes), clazz);
    }

    public static String toJsonString(Object obj) {
        return gson.toJson(obj);
    }
}
