import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacVerify {
    public static void main(String[] args) throws Exception {
        byte[] key = new byte[20];
        byte[] data = "Hi There".getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key, "HmacSHA512"));
        byte[] out = mac.doFinal(data);
        String b64 = Base64.getEncoder().encodeToString(out);
        System.out.println("RFC4231 TC1 b64=" + b64);
        System.out.println("expect      =wj1xSm+dLpW0Fd4OQ8SQqVQ8/Fz6dQXemSDcNoJ4xWAOVKhCAv7Z8wn+KKEqjN4GpH1iAbPH0R5RRzX+7KZYWw==");
        System.out.println("MATCH=" + "wj1xSm+dLpW0Fd4OQ8SQqVQ8/Fz6dQXemSDcNoJ4xWAOVKhCAv7Z8wn+KKEqjN4GpH1iAbPH0R5RRzX+7KZYWw==".equals(b64));
    }
}
