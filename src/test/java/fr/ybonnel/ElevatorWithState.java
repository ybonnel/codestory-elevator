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

import fr.ybonnel.services.AlzheimerElevator;
import fr.ybonnel.services.AlzheimerFloorsByDirection;
import fr.ybonnel.services.Command;
import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.OptimizedAlzheimerElevator;
import fr.ybonnel.services.OptimizedAlzheimerFloorsByDirection;
import fr.ybonnel.services.State;
import fr.ybonnel.services.UpAndDownWithDirectionElevator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ElevatorWithState {


    private List<User> users = new ArrayList<>();

    private int score = 10;
    private int currentFloor = 0;
    private State currentState = State.CLOSE;
    private final Elevator elevator;
    private final int cabinSize;
    private final int lowerFloor;
    private final int higherFloor;

    public ElevatorWithState(Elevator elevator, int lowerFloor, int higherFloor, int cabinSize) {
        this.elevator = elevator;
        this.cabinSize = cabinSize;
        this.lowerFloor = lowerFloor;
        this.higherFloor = higherFloor;
        reset("START");
    }

    public void addUser(int tickEnterBuilding, int startFloor, int destinationFloor) {
        users.add(new User(tickEnterBuilding, startFloor, destinationFloor));
        elevator.call(startFloor, destinationFloor > startFloor ? "UP" : "DOWN");
    }

    public void oneTick(int tick) {
        Command command = elevator.nextCommand();
        if (command != Command.CLOSE && currentState == State.OPEN
                || command == Command.CLOSE && currentState != State.OPEN
                || command == Command.UP && currentFloor == higherFloor
                || command == Command.DOWN && currentFloor == lowerFloor) {
            reset("Incompatible command");
        } else {
            switch (command) {
                case OPEN:
                    currentState = State.OPEN;
                    break;
                case CLOSE:
                    currentState = State.CLOSE;
                    break;
                case UP:
                    currentFloor++;
                    break;
                case DOWN:
                    currentFloor--;
                    break;
                case FORCERESET:
                    reset("FORCERESET");
                    break;

            }
        }

        if (currentState == State.OPEN) {
            for (Iterator<User> iterator = users.iterator(); iterator.hasNext(); ) {
                User user = iterator.next();
                if (user.isInElevator()) {
                    if (currentFloor == user.getDestinationFloor()) {
                        score += user.exitElevatorAndComputeScore(tick);
                        elevator.userHasExited();
                        iterator.remove();
                    }
                } else {
                    if (currentFloor == user.getStartFloor()) {
                        user.enterElevator(tick);
                        elevator.userHasEntered();
                        elevator.go(user.getDestinationFloor());
                    }
                }
            }
        }
    }

    private void reset(String cause) {
        currentFloor = 0;
        currentState = State.CLOSE;
        score = score - 10;
        users.clear();
        elevator.reset(cause, lowerFloor, higherFloor, cabinSize);
    }


    public Elevator getElevator() {
        return elevator;
    }

    public int getScore() {
        return score;
    }

    public String getName() {
        String name = elevator.getClass().getSimpleName();
        if (elevator instanceof OptimizedAlzheimerElevator) {
            int nbMaxWaitWithNoOverPeople = ((OptimizedAlzheimerElevator) elevator).getNbMaxWaitWithNoOverPeople();
            int nbMaxWaitWithOverPeople = ((OptimizedAlzheimerElevator) elevator).getNbMaxWaitWithOverPeople();
            int peopleInElevatorForOverFlow = ((OptimizedAlzheimerElevator) elevator).getPeopleInElevatorForOverFlow();

            return name + "(" + nbMaxWaitWithNoOverPeople
                    + " - " + nbMaxWaitWithOverPeople
                    + " - " + peopleInElevatorForOverFlow
                    + ")";

        }
        return name;
    }
}
