package altn72.railpulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConfigurationTemps {
    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
