package cx.ath.mancel01.modules.module3;

import cx.ath.mancel01.modules.module1.HelloUtils;
import cx.ath.mancel01.modules.module1.Logger;

public class Logging {

    public void log(String message) {
        new Logger().print(message);
        System.out.println(new HelloUtils().sayHello("Logger"));
    }
}
