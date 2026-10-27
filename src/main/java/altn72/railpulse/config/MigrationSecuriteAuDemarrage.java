package altn72.railpulse.config;

import altn72.railpulse.service.MigrationMotsDePasse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class MigrationSecuriteAuDemarrage implements ApplicationRunner {
    private final MigrationMotsDePasse migration;

    public MigrationSecuriteAuDemarrage(MigrationMotsDePasse migration) {
        this.migration = migration;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        migration.migrer();
    }
}
