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

import fr.ybonnel.services.model.Command;
import fr.ybonnel.services.model.Commands;
import fr.ybonnel.simpleweb4j.exception.HttpErrorException;
import fr.ybonnel.simpleweb4j.handlers.ContentType;
import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.Route;
import fr.ybonnel.simpleweb4j.handlers.RouteParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.get;

public class ElevatorService {

    private final Logger logger;

    private final Elevators elevators;
    private final String route;

    public ElevatorService(String route, Elevators elevators) {
        this.elevators = elevators;
        this.route = route;
        this.logger = LoggerFactory.getLogger(elevators.getClass());
    }

    public Route<Void, Commands> getNextCommandRoute() {
        return new Route<Void, Commands>(route + "/nextCommands", Void.class, ContentType.PLAIN_TEXT) {
            @Override
            public Response<Commands> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                Commands nextCommands;
                synchronized (elevators) {
                    nextCommands = elevators.nextCommands();
                }
                long endTime = System.nanoTime();
                logger.info("Call of nextCommand, response : {}, time({}us)", nextCommands.getCommands(), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(nextCommands);
            }
        };
    }

    public Route<Void, Void> getResetRoute() {
        return new Route<Void, Void>(route + "/reset", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevators) {
                    elevators.reset(routeParams.getParam("cause"),
                            Integer.parseInt(routeParams.getParam("lowerFloor")),
                            Integer.parseInt(routeParams.getParam("higherFloor")),
                            Integer.parseInt(routeParams.getParam("cabinSize")),
                            Integer.parseInt(routeParams.getParam("cabinCount")));
                }
                long endTime = System.nanoTime();
                logger.info("Call of reset({}) : time({}us)", routeParams.getParam("cause"), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getCallRoute() {
        return new Route<Void, Void>(route + "/call", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevators) {
                    elevators.call(
                            Integer.parseInt(routeParams.getParam("atFloor")),
                            routeParams.getParam("to"));
                }
                long endTime = System.nanoTime();
                logger.info("Call of call({},{}) : time({}us)", routeParams.getParam("atFloor"), routeParams.getParam("to"), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getGoRoute() {
        return new Route<Void, Void>(route + "/go", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevators) {
                    elevators.go(
                            Integer.parseInt(routeParams.getParam("cabin")),
                            Integer.parseInt(routeParams.getParam("floorToGo")));
                }
                long endTime = System.nanoTime();
                logger.info("Call of go({},{}) : time({}us)", routeParams.getParam("cabin"), routeParams.getParam("floorToGo"), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getUserHasEnteredRoute() {
        return new Route<Void, Void>(route + "/userHasEntered", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevators) {
                    elevators.userHasEntered(
                            Integer.parseInt(routeParams.getParam("cabin"))
                    );
                }
                long endTime = System.nanoTime();
                logger.info("Call of userHasEntered({}) : time({}us)", Integer.parseInt(routeParams.getParam("cabin")), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getUserHasExitedRoute() {
        return new Route<Void, Void>(route + "/userHasExited", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevators) {
                    elevators.userHasExited(
                            Integer.parseInt(routeParams.getParam("cabin"))
                    );
                }
                long endTime = System.nanoTime();
                logger.info("Call of userHasExited({}): time({}us)", Integer.parseInt(routeParams.getParam("cabin")), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public void registerRoutes() {
        get(getNextCommandRoute());
        get(getResetRoute());
        get(getCallRoute());
        get(getGoRoute());
        get(getUserHasEnteredRoute());
        get(getUserHasExitedRoute());
    }
}
