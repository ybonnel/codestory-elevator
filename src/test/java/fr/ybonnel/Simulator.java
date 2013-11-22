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

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.ybonnel.services.AlzheimerElevator;
import fr.ybonnel.services.ByUserElevator;
import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.FastDeliverElevator;
import fr.ybonnel.services.OptimizedAlzheimerElevator;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Simulator {

    private static final int LOWER_FLOOR = -13;
    private static final int HIGHER_FLOOR = 27;
    private static final int CABIN_SIZE = 60;

    private int currentTick = 0;

    private final Random random;

    private List<ElevatorWithState> elevatorWithStates = new ArrayList<>();
    private final List<Integer> arrivals;

    public Simulator(List<Integer> arrivals, Elevator ... elevators) {
        this.arrivals = arrivals;
        random = new Random();
        for (Elevator elevator : elevators) {
            elevatorWithStates.add(new ElevatorWithState(elevator, LOWER_FLOOR, HIGHER_FLOOR, CABIN_SIZE));
        }
    }

    private void runOneTick() {
        for (ElevatorWithState elevatorWithState : elevatorWithStates) {
            elevatorWithState.oneTick(currentTick);
        }

        for (int i = 0; i < arrivals.get(currentTick % arrivals.size()); i++) {
            int startFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) + LOWER_FLOOR;
            int destinationFloor;
            do {
                destinationFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) + LOWER_FLOOR;
            } while (destinationFloor == startFloor);

            for (ElevatorWithState elevatorWithState : elevatorWithStates) {
                elevatorWithState.addUser(currentTick, startFloor, destinationFloor);
            }
        }

        currentTick++;
    }

    public static List<Elevator> generatorByUsersElevators() {
        List<Elevator> elevators = new ArrayList<>();
        for (int tickBeforeReset = 100; tickBeforeReset <= 150; tickBeforeReset++) {
            elevators.add(new ByUserElevator(tickBeforeReset));
        }
        return elevators;
    }


    public static void main(String[] args) {
        List<Integer> arrivals = new GsonBuilder().create().fromJson(
                new InputStreamReader(Simulator.class.getResourceAsStream("/repartition.json")),
                new TypeToken<List<Integer>>(){}.getType());

        /*Simulator simulator = new Simulator(arrivals,
                new OptimizedAlzheimerElevator(),
                new AlzheimerElevator(),
                new FastDeliverElevator(),
                new ByUserElevator());



        for (int i=0; i<arrivals.size()*2; i++) {
            simulator.runOneTick();
        }

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates) {
            System.out.println("Elevator "
                    + elevatorWithState.getName()
                    + " : "
                    + elevatorWithState.getScore());
        }*/



        List<Elevator> elevators = generatorByUsersElevators();

        System.out.println(elevators.size());

        Simulator simulator = new Simulator(arrivals, elevators.toArray(new Elevator[elevators.size()]));

        long startTime = System.nanoTime();

        for (int i=0; i<arrivals.size() * 4; i++) {
            if (i%1000 == 0) {
                System.out.println(i);
                System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            }
            simulator.runOneTick();
        }

        Collections.sort(simulator.elevatorWithStates,
                new Comparator<ElevatorWithState>() {
                    @Override
                    public int compare(ElevatorWithState o1, ElevatorWithState o2) {
                        return new Integer(o2.getScore()).compareTo(o1.getScore());
                    }
                });



        int bestScore = 0;
        ElevatorWithState bestElevator = null;
        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates.subList(0, 10)) {
            System.out.println("Elevator "
                    + elevatorWithState.getName()
                    + " : "
                    + elevatorWithState.getScore());
            if (bestScore < elevatorWithState.getScore()) {
                bestElevator = elevatorWithState;
                bestScore = elevatorWithState.getScore();
            }
        }

        System.out.println("Elevator "
                + bestElevator.getName()
                + " : "
                + bestElevator.getScore());

    }

}
