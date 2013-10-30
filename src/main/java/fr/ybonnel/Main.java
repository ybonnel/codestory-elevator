package fr.ybonnel;

import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.ElevatorService;
import fr.ybonnel.services.NearestElevator;
import fr.ybonnel.services.Omnibus;
import fr.ybonnel.services.UpAndDownElevator;
import fr.ybonnel.services.UpAndDownWithDirectionElevator;

import java.io.IOException;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.*;

/**
 * Main class.
 */
public class Main {

    public final static Elevator elevators[] = {
            new Omnibus(),
            new UpAndDownElevator(),
            new NearestElevator(),
            new UpAndDownWithDirectionElevator()
    };


    /**
     * Start the server.
     * @param port http port to listen.
     * @param waitStop true to wait the stop.
     */
    public static void startServer(int port, boolean waitStop) throws IOException {

        // Set the http port.
        setPort(port);
        // Set the path to static resources.
        setPublicResourcesPath("/fr/ybonnel/public");

        for (Elevator elevator : elevators) {
            new ElevatorService("/" + elevator.getClass().getSimpleName(), elevator).registerRoutes();
        }
        new ElevatorService("", new UpAndDownWithDirectionElevator()).registerRoutes();
        new ElevatorService("/elevator", new UpAndDownWithDirectionElevator()).registerRoutes();

        // Start the server.
        start(waitStop);
    }


    /**
     * @return port to use
     */
    private static int getPort() {
        // Heroku
        String herokuPort = System.getenv("PORT");
        if (herokuPort != null) {
            return Integer.parseInt(herokuPort);
        }

        // Cloudbees
        String cloudbeesPort = System.getProperty("app.port");
        if (cloudbeesPort != null) {
            return Integer.parseInt(cloudbeesPort);
        }

        // Default port;
        return 9999;
    }

    public static void main(String[] args) throws IOException {
        // For main, we want to wait the stop.
        startServer(getPort(), true);
    }
}
