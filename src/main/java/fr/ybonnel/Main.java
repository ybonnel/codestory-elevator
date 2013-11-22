package fr.ybonnel;

import fr.ybonnel.services.AlzheimerElevator;
import fr.ybonnel.services.ByUserElevator;
import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.ElevatorService;
import fr.ybonnel.services.FastDeliverElevator;
import fr.ybonnel.services.NearestElevator;
import fr.ybonnel.services.Omnibus;
import fr.ybonnel.services.OptimizedAlzheimerElevator;
import fr.ybonnel.services.UpAndDownWithDirectionElevator;
import fr.ybonnel.simpleweb4j.exception.HttpErrorException;
import fr.ybonnel.simpleweb4j.handlers.ContentType;
import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.Route;
import fr.ybonnel.simpleweb4j.handlers.RouteParameters;
import fr.ybonnel.simpleweb4j.handlers.filter.AbstractFilter;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.*;

/**
 * Main class.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public final static Elevator elevators[] = {
            new Omnibus(),
            new FastDeliverElevator(),
            new NearestElevator(),
            new UpAndDownWithDirectionElevator(),
            new AlzheimerElevator(),
            new OptimizedAlzheimerElevator(),
            new ByUserElevator()
    };

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

        for (Elevator elevator : elevators) {
            new ElevatorService("/" + elevator.getClass().getSimpleName(), elevator).registerRoutes();
        }
        final ByUserElevator elevator = new ByUserElevator();
        new ElevatorService("/elevator", elevator).registerRoutes();

        get(new Route<Void, List<Integer>>("/stats", Void.class) {

            @Override
            public Response<List<Integer>> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                return new Response<>(elevator.getPeopleByTick());
            }
        });
        get(new Route<Void, String>("status", Void.class, ContentType.PLAIN_TEXT) {
            @Override
            public Response<String> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                StringBuilder result = new StringBuilder();
                result.append("currenttick : ").append(elevator.getCurrentTick()).append('\n');
                result.append("currentscore : ").append(elevator.getCurrentScore()).append('\n');
                result.append("resetCount : ").append(elevator.getResetCount()).append('\n');
                return new Response<>(result.toString());
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
