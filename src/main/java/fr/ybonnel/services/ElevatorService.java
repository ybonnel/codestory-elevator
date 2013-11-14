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
import fr.ybonnel.simpleweb4j.handlers.ContentType;
import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.Route;
import fr.ybonnel.simpleweb4j.handlers.RouteParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public Route<Void, Command> getNextCommandRoute() {
        return new Route<Void, Command>(route + "/nextCommand", Void.class, ContentType.PLAIN_TEXT) {
            @Override
            public Response<Command> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                Command nextCommand;
                synchronized (elevator) {
                    nextCommand = elevator.nextCommand();
                }
                long endTime = System.nanoTime();
                logger.info("Call of nextCommand, response : {}, time({}us)", nextCommand, TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(nextCommand);
            }
        };
    }

    public Route<Void, Void> getResetRoute() {
        return new Route<Void, Void>(route + "/reset", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevator) {
                    Integer lowerFloor = routeParams.getParam("lowerFloor") == null ? null : Integer.parseInt(routeParams.getParam("lowerFloor"));
                    Integer higherFloor = routeParams.getParam("higherFloor") == null ? null : Integer.parseInt(routeParams.getParam("higherFloor"));
                    Integer cabinZize = routeParams.getParam("cabinSize") == null ? null : Integer.parseInt(routeParams.getParam("cabinSize"));
                    elevator.reset(routeParams.getParam("cause"), lowerFloor, higherFloor, cabinZize);
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
                synchronized (elevator) {
                    elevator.call(Integer.parseInt(routeParams.getParam("atFloor")),
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
                synchronized (elevator) {
                    elevator.go(Integer.parseInt(routeParams.getParam("floorToGo")));
                }
                long endTime = System.nanoTime();
                logger.info("Call of go({}) : time({}us)", routeParams.getParam("floorToGo"), TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getUserHasEnteredRoute() {
        return new Route<Void, Void>(route + "/userHasEntered", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevator) {
                    elevator.userHasEntered();
                }
                long endTime = System.nanoTime();
                logger.info("Call of userHasEntered : time({}us)", TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
                return new Response<>(null);
            }
        };
    }

    public Route<Void, Void> getUserHasExitedRoute() {
        return new Route<Void, Void>(route + "/userHasExited", Void.class) {
            @Override
            public Response<Void> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                long startTime = System.nanoTime();
                synchronized (elevator) {
                    elevator.userHasExited();
                }
                long endTime = System.nanoTime();
                logger.info("Call of userHasExited: time({}us)", TimeUnit.NANOSECONDS.toMicros(endTime - startTime));
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
        if (elevator instanceof FastDeliverElevator) {
            get(new Route<Void, List<Integer>>("/stats", Void.class) {
                @Override
                public Response<List<Integer>> handle(Void param, RouteParameters routeParams) throws HttpErrorException {

                    return new Response<>(((FastDeliverElevator) elevator).getPeopleByTick());
                }
            });
            get(new Route<Void, Integer>("/currenttick", Void.class){

                @Override
                public Response<Integer> handle(Void param, RouteParameters routeParams) throws HttpErrorException {
                    return new Response<>(((FastDeliverElevator) elevator).getCurrentTick());
                }
            });
        }
    }
}
