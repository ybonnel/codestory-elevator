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
            int startFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) - LOWER_FLOOR;
            int destinationFloor;
            do {
                destinationFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) - LOWER_FLOOR;
            } while (destinationFloor == startFloor);

            for (ElevatorWithState elevatorWithState : elevatorWithStates) {
                elevatorWithState.addUser(currentTick, startFloor, destinationFloor);
            }
        }

        currentTick++;
    }

    public static List<Elevator> generatorOptimizedAlzheimerElevators() {
        List<Elevator> elevators = new ArrayList<>();
        for (int nbMaxWait = 10; nbMaxWait <= 15; nbMaxWait+=1) {
            for (int nbMaxWaitOver = 5; nbMaxWaitOver <= 15; nbMaxWaitOver+=1) {
                for (int over = 20; over <= 30; over+=1) {
                    elevators.add(new OptimizedAlzheimerElevator(
                            nbMaxWait,
                            nbMaxWaitOver,
                            over
                    ));
                }
            }
        }
        return elevators;
    }

    public static List<Elevator> generateElevators(int currentPeople, Map<Integer, Integer> currentNbMaxWait, Map<Integer, Integer> currentNbMaxWaitElevator) {
        if (currentPeople > CABIN_SIZE) {
            return Arrays.asList((Elevator)new AlzheimerElevator(currentNbMaxWait, currentNbMaxWaitElevator));
        } else {
            List<Elevator> elevators = new ArrayList<>();
            int minNbMaxWait = currentPeople <= 10
                    ? 18
                    : currentPeople <= 30
                    ? 13
                    : currentPeople <= 50
                    ? 13
                    : 8;
            int maxNbMaxWait = currentPeople <= 10
                    ? 22
                    : currentPeople <= 30
                    ? 17
                    : currentPeople <= 50
                    ? 17
                    : 12;
            int minNbMaxWaitInElevator = currentPeople <= 10
                    ? 23
                    : currentPeople <= 30
                    ? 13
                    : currentPeople <= 50
                    ? 23
                    : 23;
            int maxNbMaxWaitInElevator = currentPeople <= 10
                    ? 27
                    : currentPeople <= 30
                    ? 17
                    : currentPeople <= 50
                    ? 27
                    : 27;

            for (int nbMaxWait = minNbMaxWait; nbMaxWait<= maxNbMaxWait; nbMaxWait = nbMaxWait + 2) {
                for (int nbMaxWaitInElevator = minNbMaxWaitInElevator; nbMaxWaitInElevator<= maxNbMaxWaitInElevator; nbMaxWaitInElevator = nbMaxWaitInElevator + 2) {
                    Map<Integer, Integer> nbMaxWaits = new HashMap<>(currentNbMaxWait);
                    nbMaxWaits.put(currentPeople, nbMaxWait);
                    Map<Integer, Integer> nbMaxWaitInElevators = new HashMap<>(currentNbMaxWaitElevator);
                    nbMaxWaitInElevators.put(currentPeople, nbMaxWaitInElevator);
                    elevators.addAll(generateElevators(currentPeople + 20, nbMaxWaits, nbMaxWaitInElevators));
                }
            }
            System.out.println("" + currentPeople + " - " + elevators.size());
            return elevators;
        }
    }

    public static void main(String[] args) {
        List<Integer> arrivals = new GsonBuilder().create().fromJson(
                new InputStreamReader(Simulator.class.getResourceAsStream("/repartition.json")),
                new TypeToken<List<Integer>>(){}.getType());

        Simulator simulator = new Simulator(arrivals,
                new OptimizedAlzheimerElevator(),
                new OptimizedAlzheimerElevator(14, 12, 24),
                new AlzheimerElevator(),
                new FastDeliverElevator());

        for (int i=0; i<arrivals.size()*2; i++) {
            simulator.runOneTick();
        }

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates) {
            System.out.println("Elevator "
                    + elevatorWithState.getName()
                    + " : "
                    + elevatorWithState.getScore());
        }



        /*List<Elevator> elevators = generatorOptimizedAlzheimerElevators();

        System.out.println(elevators.size());

        simulator = new Simulator(arrivals, elevators.toArray(new Elevator[elevators.size()]));

        for (int i=0; i<arrivals.size(); i++) {
            if (i%1000 == 0) {
                System.out.println(i);
            }
            simulator.runOneTick();
        }

        int bestScore = 0;
        ElevatorWithState bestElevator = null;

        Collections.sort(simulator.elevatorWithStates,
                new Comparator<ElevatorWithState>() {
                    @Override
                    public int compare(ElevatorWithState o1, ElevatorWithState o2) {
                        return new Integer(o2.getScore()).compareTo(o1.getScore());
                    }
                });

        elevators.clear();

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates.subList(0, 100)) {
            OptimizedAlzheimerElevator oldElevator = (OptimizedAlzheimerElevator) elevatorWithState.getElevator();
            elevators.add(new OptimizedAlzheimerElevator(oldElevator.getNbMaxWaitWithNoOverPeople(), oldElevator.getNbMaxWaitWithOverPeople(), oldElevator.getPeopleInElevatorForOverFlow()));
        }


        simulator = new Simulator(arrivals, elevators.toArray(new Elevator[elevators.size()]));


        for (int i=0; i<arrivals.size()*10; i++) {
            if (i%10000 == 0) {
                System.out.println(i);
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


        elevators.clear();

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates.subList(0, 10)) {
            OptimizedAlzheimerElevator oldElevator = (OptimizedAlzheimerElevator) elevatorWithState.getElevator();
            elevators.add(new OptimizedAlzheimerElevator(oldElevator.getNbMaxWaitWithNoOverPeople(), oldElevator.getNbMaxWaitWithOverPeople(), oldElevator.getPeopleInElevatorForOverFlow()));
        }

        simulator = new Simulator(arrivals, elevators.toArray(new Elevator[elevators.size()]));


        for (int i=0; i<arrivals.size()*100; i++) {
            if (i%100000 == 0) {
                System.out.println(i);
            }
            simulator.runOneTick();
        }


        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates) {
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
                + bestElevator.getScore()); */

    }

}
