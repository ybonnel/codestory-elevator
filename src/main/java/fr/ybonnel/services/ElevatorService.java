/*
 * Copyright 2013- Yan Bonnel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ybonnel.services;

import fr.ybonnel.simpleweb4j.exception.HttpErrorException;
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

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.addSpecificHandler;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.get;

public class ElevatorService {

    private final Logger logger;

    private final Elevator elevator;
    private String route;

    public ElevatorService(String route, Elevator elevator) {
        this.elevator = elevator;
        this.route = route;
        this.logger = LoggerFactory.getLogger(elevator.getClass());
    }

    public AbstractHandler getHandler() {
        return new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if (request.getPathInfo().equals(route + "/nextCommand")) {
                    Command nextCommand;
                    synchronized (elevator) {
                        nextCommand = elevator.nextCommand();
                    }
                    logger.info("Call of nextCommand, response : {}", nextCommand);
                    response.getWriter().print(nextCommand);
                    response.getWriter().flush();
                    response.getWriter().close();
                }
            }
        };
    }

    public Route<Void, Void> getResetRoute() {
        return new Route<Void, Void>(route + "/reset", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                synchronized (elevator) {
                    logger.info("Call of reset({})", routeParams.getParam("cause"));
                    elevator.reset(routeParams.getParam("cause"));
                }
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getCallRoute() {
        return new Route<Void, Void>(route + "/call", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                synchronized (elevator) {
                    logger.info("Call of call({},{})", routeParams.getParam("atFloor"), routeParams.getParam("to"));
                    elevator.call(Integer.parseInt(routeParams.getParam("atFloor")),
                            routeParams.getParam("to"));
                }
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getGoRoute() {
        return new Route<Void, Void>(route + "/go", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                synchronized (elevator) {
                    logger.info("Call of go({})", routeParams.getParam("floorToGo"));
                    elevator.go(Integer.parseInt(routeParams.getParam("floorToGo")));
                }
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getUserHasEnteredRoute() {
        return new Route<Void, Void>(route + "/userHasEntered", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                synchronized (elevator) {
                    logger.info("Call of userHasEntered");
                    elevator.userHasEntered();
                }
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getUserHasExitedRoute() {
        return new Route<Void, Void>(route + "/userHasExited", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                synchronized (elevator) {
                    logger.info("Call of userHasExited");
                    elevator.userHasExited();
                }
                return new Response<>(null);
            }
        };
    }

    public void registerRoutes() {
        addSpecificHandler(getHandler());
        get(getResetRoute());
        get(getCallRoute());
        get(getGoRoute());
        get(getUserHasEnteredRoute());
        get(getUserHasExitedRoute());
    }
}
