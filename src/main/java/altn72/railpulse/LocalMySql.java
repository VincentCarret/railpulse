package altn72.railpulse;

import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Starts the local Windows MySQL service before Spring creates its datasource. */
final class LocalMySql {
    private LocalMySql() {
    }

    static void startIfNeeded(Environment environment) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.startsWith("jdbc:mysql://")) {
            return;
        }
        URI database = URI.create(url.substring("jdbc:".length()));
        String host = database.getHost();
        if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)
                && !"::1".equals(host) && !"[::1]".equals(host)) {
            return;
        }
        int port = database.getPort() == -1 ? 3306 : database.getPort();
        if (isListening(host, port)) {
            return;
        }

        System.out.println("Rail Pulse : démarrage du service MySQL95...");
        // Windows may require administrator rights to start a stopped service.
        // Request elevation only when a normal Start-Service fails.
        String command = "$ErrorActionPreference='Stop'; try { "
                + "Start-Service -Name MySQL95 } catch { "
                + "$helper = Start-Process powershell.exe -Verb RunAs -WindowStyle Hidden "
                + "-ArgumentList '-NoProfile -Command \"try { Start-Service MySQL95 -ErrorAction Stop } catch { exit 1 }\"' "
                + "-Wait -PassThru; if ($helper.ExitCode -ne 0) { exit 1 } }";
        try {
            Process service = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-Command", command).inheritIO().start();
            if (!service.waitFor(120, TimeUnit.SECONDS)) {
                service.destroyForcibly();
                throw new IllegalStateException("Le démarrage de MySQL95 a dépassé le délai autorisé.");
            }
            if (service.exitValue() != 0) {
                throw new IllegalStateException("Impossible de démarrer MySQL95. Accepte la demande administrateur de Windows.");
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!isListening(host, port)) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException("MySQL95 ne répond pas sur le port " + port
                            + ". Vérifie le port dans application.properties et la configuration MySQL.");
                }
                Thread.sleep(250);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lancer PowerShell pour démarrer MySQL95.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Démarrage de MySQL interrompu.", exception);
        }
    }

    private static boolean isListening(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
