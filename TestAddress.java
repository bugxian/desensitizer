import com.desensitizer.builtin.desensitizer.AddressDesensitizer;

public class TestAddress {
    public static void main(String[] args) {
        AddressDesensitizer desensitizer = new AddressDesensitizer();
        String input = "72658 Braun Springs, Forbesview, NY 94223";
        String result = desensitizer.desensitize(input);
        System.out.println("输入: " + input);
        System.out.println("输出: " + result);
        System.out.println("预期: Forbesview, NY 94223");
        System.out.println("匹配: " + result.equals("Forbesview, NY 94223"));
    }
}