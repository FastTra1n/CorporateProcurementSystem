package com.team.corporate;

import com.team.corporate.config.ApplicationServices;
import com.team.corporate.config.FileConfigurationProvider;
import com.team.corporate.config.ConfigurationProvider;
import com.team.corporate.console.ConsoleApplication;
import com.team.corporate.console.ConsoleInput;
import com.team.corporate.console.ConsoleTerminal;
import com.team.corporate.repositories.impls.*;
import com.team.corporate.services.impls.*;
import com.team.corporate.utils.DataInitializer;
import com.team.corporate.utils.HibernateFactory;
import org.jline.reader.LineReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    static void main() {
        UiApplication.launch(UiApplication.class);
//        int result = run(new FileConfigurationProvider());
//        if (result != 0) {
//            System.exit(result);
//        }
    }

    static int run(ConfigurationProvider configurationProvider) {
        try {
            start(configurationProvider);
            return 0;
        } catch (Exception e) {
            LOG.error("Не удалось запустить или завершить приложение", e);
            System.err.println("Не удалось запустить или завершить приложение. Проверьте файл настроек и доступность базы данных.");
            return 1;
        }
    }

    private static void start(ConfigurationProvider configurationProvider) throws Exception {
        try (var services = ApplicationServices.open(configurationProvider.getConfiguration());
             var terminal = ConsoleTerminal.open()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            new ConsoleApplication(
                    new ConsoleInput(reader),
                    services.users(),
                    services.products(),
                    services.categories(),
                    services.orders(),
                    services.items(),
                    services.inventories(),
                    services.warehouses(),
                    services.csvExport(),
                    services.statistics()
            ).run();
        }
    }
}