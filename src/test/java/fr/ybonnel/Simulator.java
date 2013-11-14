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
import fr.ybonnel.services.AlzheimerFastDeliverFloorsByDirection;
import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.FastDeliverElevator;
import fr.ybonnel.services.OptimizedAlzheimerElevator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Simulator {

    private static final int LOWER_FLOOR = 0;
    private static final int HIGHER_FLOOR = 19;
    private static final int CABIN_SIZE = 42;

    private int currentTick = 0;

    private final Random random;
    private final double chanceToCreateUser;

    private List<ElevatorWithState> elevatorWithStates = new ArrayList<>();

    public Simulator(double chanceToCreateUser, Elevator ... elevators) {
        random = new Random();
        for (Elevator elevator : elevators) {
            elevatorWithStates.add(new ElevatorWithState(elevator, LOWER_FLOOR, HIGHER_FLOOR, CABIN_SIZE));
        }
        this.chanceToCreateUser = chanceToCreateUser;
    }

    private void runOneTick() {
        for (ElevatorWithState elevatorWithState : elevatorWithStates) {
            elevatorWithState.oneTick(currentTick);
        }

        int nbRandom = (int) Math.ceil(chanceToCreateUser);

        double probability = chanceToCreateUser / ((double)nbRandom);

        for (int i = 0; i < nbRandom; i++) {
            if (random.nextDouble() <= probability) {
                int startFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) - LOWER_FLOOR;
                int destinationFloor;
                do {
                    destinationFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) - LOWER_FLOOR;
                } while (destinationFloor == startFloor);

                for (ElevatorWithState elevatorWithState : elevatorWithStates) {
                    elevatorWithState.addUser(currentTick, startFloor, destinationFloor);
                }

            }
        }

        currentTick++;
    }

    public static void main(String[] args) {

        FastDeliverElevator fastDeliverElevator = new FastDeliverElevator();
        Simulator simulator = new Simulator(2,
                new OptimizedAlzheimerElevator(),
                new AlzheimerElevator(),
                fastDeliverElevator);

        for (int i=0; i<5000; i++) {
            simulator.runOneTick();
        }

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates) {
            System.out.println("Elevator "
                    + elevatorWithState.getElevator().getClass().getSimpleName()
                    + " : "
                    + elevatorWithState.getScore());
        }

        System.out.println(fastDeliverElevator.getPeopleByTick());


    }


}
