package cx.ath.mancel01.modules.module2;

import cx.ath.mancel01.modules.module1.HelloUtils;
import cx.ath.mancel01.modules.module3.Logging;

public class App {

    public static void main(String[] args) {
        System.out.println(new HelloUtils().sayHello("Mathieu"));
        new Logging().log("Just said hello to Mathieu");
    }
}
