package fr.ybonnel;

import fr.ybonnel.services.ByUserElevator;
import fr.ybonnel.services.ByUserElevators;
import fr.ybonnel.services.ElevatorService;
import fr.ybonnel.simpleweb4j.exception.HttpErrorException;
import fr.ybonnel.simpleweb4j.handlers.ContentType;
import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.Route;
import fr.ybonnel.simpleweb4j.handlers.RouteParameters;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.*;

/**
 * Main class.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(request.getMethod());
        requestURL.append(" - ");
        requestURL.append(request.getPathInfo());
        if (request.getQueryString() == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(request.getQueryString()).toString();
        }
    }


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

        addSpecificHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                logger.info("Call of {} for {}", request.getRemoteAddr(), getFullURL(request));
            }
        });

        final ByUserElevators elevators = new ByUserElevators();
        new ElevatorService("/elevator", elevators).registerRoutes();

        get(new Route<Void, String>("/status", Void.class, ContentType.PLAIN_TEXT){

            @Override
            public Response<String> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                StringBuilder builder = new StringBuilder();
                int index = 0;
                for (ByUserElevator elevator : elevators.getElevators()) {
                    builder.append("elevator ").append(index++).append(" :").append("\n");
                    builder.append("\tcurrenttick : ").append(elevator.getCurrentTick()).append('\n');
                    builder.append("\tcurrentscore : ").append(elevator.getCurrentScore()).append('\n');
                    builder.append("\tresetCount : ").append(elevator.getResetCount()).append('\n');
                }
                builder.append("\nPeopleByTick : ").append("\n").append(elevators.getPeopleByTick().subList(0, Math.min(elevators.getCurrentTick(), ByUserElevators.MAX_TICK)));

                return new Response<>(builder.toString());
            }
        });

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
