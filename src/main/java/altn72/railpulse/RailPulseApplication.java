package altn72.railpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;

@SpringBootApplication
public class RailPulseApplication {

    public static void main(String[] args) {

        SpringApplication application = new SpringApplication(RailPulseApplication.class);
        application.addListeners((ApplicationEnvironmentPreparedEvent event) ->
                LocalMySql.startIfNeeded(event.getEnvironment()));
        application.run(args);


    }


}
