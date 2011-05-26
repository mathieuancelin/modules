package cx.ath.mancel01.modules.module2;

import cx.ath.mancel01.modules.module1.HelloUtils;
import cx.ath.mancel01.modules.module3.Logging;

public class App {

    private final HelloUtils utils = new HelloUtils();
    private final Logging log = new Logging();

    public static void main(String[] args) {
        App app = new App();
        app.hello("Mathieu");
        app.hello("Jeremy");
        app.hello("Samuel");
        app.hello("Guillaume");
        app.hello("Kevin");
        app.hello("Adeline");
    }

    public void hello(String name) {
        System.out.println("============================================");
        System.out.println(utils.sayHello(name));
        log.log("Just said hello to " + name);
    }
}
