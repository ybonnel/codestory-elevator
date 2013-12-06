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
package fr.ybonnel;

import fr.ybonnel.services.ByUser2Elevators;
import fr.ybonnel.services.ByUserElevators;
import fr.ybonnel.services.Elevators;
import fr.ybonnel.services.model.Command;
import fr.ybonnel.services.model.Commands;
import fr.ybonnel.services.model.Direction;
import fr.ybonnel.services.model.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ElevatorsWithState {

    private static final Logger logger = LoggerFactory.getLogger(ElevatorsWithState.class);

    private List<fr.ybonnel.User> users = new ArrayList<>();

    private int score = 0;
    private int nextReset = 0;
    private List<Integer> currentFloors = new ArrayList<>();
    private List<State> currentStates = new ArrayList<>();
    private final Elevators elevators;
    private final int cabinSize;
    private final int lowerFloor;
    private final int higherFloor;
    private final int nbElevators;

    public ElevatorsWithState(Elevators elevators, int lowerFloor, int higherFloor, int cabinSize, int nbElevators) {
        this.elevators = elevators;
        this.cabinSize = cabinSize;
        this.lowerFloor = lowerFloor;
        this.higherFloor = higherFloor;
        this.nbElevators = nbElevators;
        for (int index = 0; index < nbElevators; index++) {
            currentStates.add(State.CLOSE);
            currentFloors.add(0);
        }
        reset("all elevators are at floor ");
    }

    public void addUser(int tickEnterBuilding, int startFloor, int destinationFloor) {
        users.add(new User(tickEnterBuilding, startFloor, destinationFloor));
        elevators.call(startFloor, destinationFloor > startFloor ? "UP" : "DOWN");
    }

    private void logState() {
        Map<Integer, Integer> nbUsersByFloor = new HashMap<>();
        List<Integer> nbUsersByElevator = new ArrayList<>(nbElevators);

        for (int elevator=0; elevator < nbElevators; elevator++) {
            nbUsersByElevator.add(0);
        }

        for (int floor = lowerFloor; floor <= higherFloor; floor++) {
            nbUsersByFloor.put(floor, 0);
        }

        for (User user : users) {
            if (user.isInOneElevator()) {
                nbUsersByElevator.set(user.getIndexElevator(), nbUsersByElevator.get(user.getIndexElevator()) + 1);
            } else {
                nbUsersByFloor.put(user.getStartFloor(), nbUsersByFloor.get(user.getStartFloor()) + 1);
            }
        }

        List<Integer> nbUsersForEachFloor = new ArrayList<>();
        for (int floor = lowerFloor; floor <= higherFloor; floor++) {
            nbUsersForEachFloor.add(nbUsersByFloor.get(floor));
        }

        logger.info("Elevators : {}, Building {}", nbUsersByElevator, nbUsersForEachFloor);
    }

    public void oneTick(int tick) {
        Commands commands = elevators.nextCommands();
        //logState();
        if (commands.getCommands().size() != nbElevators) {
            reset("Incompatible command");
            return;
        }
        int indexElevator = 0;
        for (Command command : commands.getCommands()) {
            if (command != Command.CLOSE && currentStates.get(indexElevator) == State.OPEN
                    || command == Command.CLOSE && currentStates.get(indexElevator) != State.OPEN
                    || command == Command.UP && currentFloors.get(indexElevator) == higherFloor
                    || command == Command.DOWN && currentFloors.get(indexElevator) == lowerFloor) {
                reset("Incompatible command");
                return;
            }
            indexElevator++;
        }

        indexElevator = 0;
        for (Command command : commands.getCommands()) {
            Direction directionOfOpen = null;

            switch (command) {
                case OPEN_DOWN:
                    directionOfOpen = Direction.DOWN;
                    currentStates.set(indexElevator,State.OPEN);
                    break;
                case OPEN_UP:
                    directionOfOpen = Direction.UP;
                    currentStates.set(indexElevator,State.OPEN);
                    break;
                case CLOSE:
                    currentStates.set(indexElevator,State.CLOSE);
                    break;
                case UP:
                    currentFloors.set(indexElevator, currentFloors.get(indexElevator) + 1);
                    break;
                case DOWN:
                    currentFloors.set(indexElevator, currentFloors.get(indexElevator) - 1);
                    break;
                case FORCERESET:
                    reset("FORCERESET");
                    break;

            }

            if (currentStates.get(indexElevator) == State.OPEN) {
                for (Iterator<User> iterator = users.iterator(); iterator.hasNext(); ) {
                    User user = iterator.next();
                    if (user.isInElevator(indexElevator)) {
                        if (currentFloors.get(indexElevator) == user.getDestinationFloor()) {
                            int scoreOfUser = user.exitElevatorAndComputeScore(tick);
                            score += scoreOfUser;
                            elevators.userHasExited(indexElevator);
                            iterator.remove();
                        }
                    }
                }
                for (User user : users) {
                    if (currentFloors.get(indexElevator) == user.getStartFloor()
                            && nbUserInElevator(indexElevator) < cabinSize
                            && !user.isInOneElevator()
                            && user.directionWished() == directionOfOpen) {
                        user.enterElevator(tick, indexElevator);
                        elevators.userHasEntered(indexElevator);
                        elevators.go(indexElevator, user.getDestinationFloor());
                    }
                }
            }
            indexElevator++;
        }
    }

    private int nbUserInElevator(int indexElevator) {
        int nbUsersInElevator = 0;

        for (User user : users) {
            if (user.isInElevator(indexElevator)) {
                nbUsersInElevator++;
            }
        }
        return nbUsersInElevator;
    }


    private void reset(String cause) {
        currentFloors.clear();
        currentStates.clear();
        for (int index = 0; index < nbElevators; index++) {
            currentStates.add(State.CLOSE);
            currentFloors.add(0);
        }
        score = score - nextReset;
        nextReset += 2;
        users.clear();
        elevators.reset(cause, lowerFloor, higherFloor, cabinSize, nbElevators);
    }

    public int getScore() {
        return score;
    }

    public String getName() {
        String name = elevators.getClass().getSimpleName();
        if (elevators instanceof ByUser2Elevators) {
            name += "(" + ((ByUser2Elevators)elevators).getMaxWaitingMean()
                    + ")";
        }
        return name;
    }
}
