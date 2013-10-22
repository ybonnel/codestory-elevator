package fr.ybonnel;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.ElevatorService;
import fr.ybonnel.services.MongoService;
import fr.ybonnel.services.NearestElevator;
import fr.ybonnel.services.Omnibus;
import fr.ybonnel.services.UpAndDownElevator;
import fr.ybonnel.services.UpAndDownWithDirectionElevator;

import java.io.IOException;
import java.net.UnknownHostException;

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
    public static void startServer(int port, boolean waitStop, int portMongo) throws IOException {
        //startMongoIfNeeded(waitStop, portMongo);
        //bindMongoClient(portMongo);

        // Set the http port.
        setPort(port);
        // Set the path to static resources.
        setPublicResourcesPath("/fr/ybonnel/public");

        for (Elevator elevator : elevators) {
            new ElevatorService("/" + elevator.getClass().getSimpleName(), elevator).registerRoutes();
        }
        new ElevatorService("", new UpAndDownWithDirectionElevator()).registerRoutes();

        // Start the server.
        start(waitStop);
        //stopMongoIfNeeded(waitStop);
    }


    private static boolean dev = true;

    public static boolean isDev() {
        return dev;
    }

    private static MongodExecutable mongodExe = null;
    private static MongodProcess mongodProc = null;

    private static void stopMongoIfNeeded(boolean waitStop) {
        if (isDev() && waitStop) {
            mongodProc.stop();
            mongodExe.stop();
        }
    }

    private static void startMongoIfNeeded(boolean waitStop, int portMongo) throws IOException {
        if (isDev() && waitStop) {
            MongodStarter runtime = MongodStarter.getDefaultInstance();
            mongodExe = runtime.prepare(new MongodConfigBuilder()
                    .version(Version.Main.PRODUCTION)
                    .net(new Net(portMongo, Network.localhostIsIPv6()))
                    .build());
            mongodProc = mongodExe.start();
        }
    }

    private static void bindMongoClient(int portMongo) throws UnknownHostException {
        if (isDev()) {
            MongoService.setMongoClient(new MongoClient("localhost", portMongo), "dev");
        } else {
            String uriAsString = System.getProperty("MONGOHQ_URL_BEERS");
            MongoClientURI uri = new MongoClientURI(uriAsString);
            MongoService.setMongoClient(new MongoClient(uri), uri.getDatabase());
        }
    }

    /**
     * @return port to use
     */
    private static int getPort() {
        // Heroku
        String herokuPort = System.getenv("PORT");
        if (herokuPort != null) {
            dev = false;
            return Integer.parseInt(herokuPort);
        }

        // Cloudbees
        String cloudbeesPort = System.getProperty("app.port");
        if (cloudbeesPort != null) {
            dev = false;
            return Integer.parseInt(cloudbeesPort);
        }

        // Default port;
        return 9999;
    }

    /**
     * @return port to use for mongo.
     */
    private static int getPortMongo() {
        // Default port;
        return 9998;
    }

    public static void main(String[] args) throws IOException {
        // For main, we want to wait the stop.
        startServer(getPort(), true, getPortMongo());
    }
}
