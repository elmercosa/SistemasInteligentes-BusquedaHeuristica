package aima.core.search.local;

import aima.core.search.framework.Metrics;
import aima.core.util.Tasks;
import aima.core.util.Util;

import java.util.*;
import java.util.function.Predicate;

/**
 * Artificial Intelligence A Modern Approach (3rd Edition): Figure 4.8, page
 * 129.<br>
 * <br>
 * 
 * <pre>
 * function GENETIC-ALGORITHM(population, FITNESS-FN) returns an individual
 *   inputs: population, a set of individuals
 *           FITNESS-FN, a function that measures the fitness of an individual
 *           
 *   repeat
 *     new_population &lt;- empty set
 *     for i = 1 to SIZE(population) do
 *       x &lt;- RANDOM-SELECTION(population, FITNESS-FN)
 *       y &lt;- RANDOM-SELECTION(population, FITNESS-FN)
 *       child &lt;- REPRODUCE(x, y)
 *       if (small random probability) then child &lt;- MUTATE(child)
 *       add child to new_population
 *     population &lt;- new_population
 *   until some individual is fit enough, or enough time has elapsed
 *   return the best individual in population, according to FITNESS-FN
 * --------------------------------------------------------------------------------
 * function REPRODUCE(x, y) returns an individual
 *   inputs: x, y, parent individuals
 *   
 *   n &lt;- LENGTH(x); c &lt;- random number from 1 to n
 *   return APPEND(SUBSTRING(x, 1, c), SUBSTRING(y, c+1, n))
 * </pre>
 * 
 * Figure 4.8 A genetic algorithm. The algorithm is the same as the one
 * diagrammed in Figure 4.6, with one variation: in this more popular version,
 * each mating of two parents produces only one offspring, not two.
 * 
 * @author Ciaran O'Reilly
 * @author Mike Stampone
 * @author Ruediger Lunde
 * 
 * @param <A>
 *            the type of the alphabet used in the representation of the
 *            individuals in the population (this is to provide flexibility in
 *            terms of how a problem can be encoded).
 */
public class GeneticAlgorithm<A> {
	protected static final String POPULATION_SIZE = "populationSize";
	protected static final String ITERATIONS = "iterations";
	protected static final String TIME_IN_MILLISECONDS = "timeInMSec";
	//
	protected Metrics metrics = new Metrics();
	//
	protected int individualLength;
	protected List<A> finiteAlphabet;
	protected double mutationProbability;
	
	protected Random random;
	private List<ProgressTracker<A>> progressTrackers = new ArrayList<>();

	public GeneticAlgorithm(int individualLength, Collection<A> finiteAlphabet, double mutationProbability) {
		this(individualLength, finiteAlphabet, mutationProbability, new Random());
	}

	public GeneticAlgorithm(int individualLength, Collection<A> finiteAlphabet, double mutationProbability,
			Random random) {
		this.individualLength = individualLength;
		this.finiteAlphabet = new ArrayList<A>(finiteAlphabet);
		this.mutationProbability = mutationProbability;
		this.random = random;

		assert (this.mutationProbability >= 0.0 && this.mutationProbability <= 1.0);
	}

	/** Progress tracers can be used to display progress information. */
	public void addProgressTracer(ProgressTracker<A> pTracker) {
		progressTrackers.add(pTracker);
	}
	
	/**
	 * Starts the genetic algorithm and stops after a specified number of
	 * iterations.
	 */
	public Individual<A> geneticAlgorithm(Collection<Individual<A>> initPopulation,
			FitnessFunction<A> fitnessFn, final int maxIterations) {
		Predicate<Individual<A>> goalTest = state -> getIterations() >= maxIterations;
		return geneticAlgorithm(initPopulation, fitnessFn, goalTest, 0L);
	}
	
	/**
	 * Template method controlling search. It returns the best individual in the
	 * specified population, according to the specified FITNESS-FN and goal
	 * test.
	 * 
	 * @param initPopulation
	 *            a set of individuals
	 * @param fitnessFn
	 *            a function that measures the fitness of an individual
	 * @param goalTest
	 *            test determines whether a given individual is fit enough to
	 *            return. Can be used in subclasses to implement additional
	 *            termination criteria, e.g. maximum number of iterations.
	 * @param maxTimeMilliseconds
	 *            the maximum time in milliseconds that the algorithm is to run
	 *            for (approximate). Only used if > 0L.
	 * @return the best individual in the specified population, according to the
	 *         specified FITNESS-FN and goal test.
	 */
	// function GENETIC-ALGORITHM(population, FITNESS-FN) returns an individual
	// inputs: population, a set of individuals
	// FITNESS-FN, a function that measures the fitness of an individual
	public Individual<A> geneticAlgorithm(Collection<Individual<A>> initPopulation, FitnessFunction<A> fitnessFn,
										  Predicate<Individual<A>> goalTest, long maxTimeMilliseconds) {
		Individual<A> bestIndividual = null;

		// Create a local copy of the population to work with
		List<Individual<A>> population = new ArrayList<>(initPopulation);
		// Validate the population and setup the instrumentation
		validatePopulation(population);
		updateMetrics(population, 0, 0L);

		long startTime = System.currentTimeMillis();

		// repeat
		int itCount = 0;
		do {

			bestIndividual = retrieveBestIndividual(population, fitnessFn);
			population = nextGeneration(population, fitnessFn, bestIndividual);

//			monitorizar el fitnes medio y mejor
			System.out.println("\nGen: "+ itCount + " f_best: " + fitnessFn.apply(bestIndividual) + " f_average: " + averageFitness(population, fitnessFn));


			updateMetrics(population, ++itCount, System.currentTimeMillis() - startTime);

			// until some individual is fit enough, or enough time has elapsed
			if (maxTimeMilliseconds > 0L && (System.currentTimeMillis() - startTime) > maxTimeMilliseconds)
				break;
			if (Tasks.currIsCancelled())
				break;
		} while (!goalTest.test(bestIndividual));

		notifyProgressTrackers(itCount, population);
		// return the best individual in population, according to FITNESS-FN
		return bestIndividual;
	}

	private double averageFitness(List<Individual<A>> population, FitnessFunction<A> fitnessFn) {
		// Determine all of the fitness values
		double fValues = 0;
		for (Individual<A> aIndividual : population) {
			fValues += fitnessFn.apply(aIndividual);
		}
		return fValues/population.size();
	}

	public Individual<A> retrieveBestIndividual(Collection<Individual<A>> population, FitnessFunction<A> fitnessFn) {
		Individual<A> bestIndividual = null;
		double bestSoFarFValue = Double.NEGATIVE_INFINITY;

		for (Individual<A> individual : population) {
			double fValue = fitnessFn.apply(individual);
			if (fValue > bestSoFarFValue) {
				bestIndividual = individual;
				bestSoFarFValue = fValue;
			}
		}

		return bestIndividual;
	}

	/**
	 * Sets the population size and number of iterations to zero.
	 */
	public void clearInstrumentation() {
		updateMetrics(new ArrayList<Individual<A>>(), 0, 0L);
	}

	/**
	 * Returns all the metrics of the genetic algorithm.
	 * 
	 * @return all the metrics of the genetic algorithm.
	 */
	public Metrics getMetrics() {
		return metrics;
	}

	/**
	 * Returns the population size.
	 * 
	 * @return the population size.
	 */
	public int getPopulationSize() {
		return metrics.getInt(POPULATION_SIZE);
	}

	/**
	 * Returns the number of iterations of the genetic algorithm.
	 * 
	 * @return the number of iterations of the genetic algorithm.
	 */
	public int getIterations() {
		return metrics.getInt(ITERATIONS);
	}

	/**
	 * 
	 * @return the time in milliseconds that the genetic algorithm took.
	 */
	public long getTimeInMilliseconds() {
		return metrics.getLong(TIME_IN_MILLISECONDS);
	}

	/**
	 * Updates statistic data collected during search.
	 * 
	 * @param itCount
	 *            the number of iterations.
	 * @param time
	 *            the time in milliseconds that the genetic algorithm took.
	 */
	protected void updateMetrics(Collection<Individual<A>> population, int itCount, long time) {
		metrics.set(POPULATION_SIZE, population.size());
		metrics.set(ITERATIONS, itCount);
		metrics.set(TIME_IN_MILLISECONDS, time);
	}

	//
	// PROTECTED METHODS
	//
	// Note: Override these protected methods to create your own desired
	// behavior.
	//
	/**
	 * Primitive operation which is responsible for creating the next
	 * generation. Override to get progress information!
	 */
	protected List<Individual<A>> nextGeneration(List<Individual<A>> population, FitnessFunction<A> fitnessFn, Individual<A> bestBefore) {
		// new_population <- empty set
		List<Individual<A>> newPopulation = new ArrayList<>(population.size());
		// for i = 1 to SIZE(population) do
		for (int i = 0; i < population.size()-1; i++) { //-1 para el elitismo
			// x <- RANDOM-SELECTION(population, FITNESS-FN)
			Individual<A> x = randomSelection(population, fitnessFn);
			// y <- RANDOM-SELECTION(population, FITNESS-FN)
			Individual<A> y = randomSelection(population, fitnessFn);
			
			// child <- REPRODUCE(x, y)
			Individual<A> child = reproduce3(x, y);
			// if (small random probability) then child <- MUTATE(child)
			if (random.nextDouble() <= mutationProbability) {
				child = mutate(child);
			}
			// add child to new_population reemplazo incondicional
			newPopulation.add(child);
		}
		newPopulation.add(bestBefore);
		notifyProgressTrackers(getIterations(), population);
		return newPopulation;
	}

	// RANDOM-SELECTION(population, FITNESS-FN)
	protected Individual<A> randomSelection(List<Individual<A>> population, FitnessFunction<A> fitnessFn) {
		// Default result is last individual
		// (just to avoid problems with rounding errors)
		Individual<A> selected = population.get(population.size() - 1);

		// Determine all of the fitness values
		double[] fValues = new double[population.size()];
		double minFitness = fitnessFn.apply(population.get(0));
		for (int i = 0; i < population.size(); i++) {
			fValues[i] = fitnessFn.apply(population.get(i));
			if(minFitness > fitnessFn.apply(population.get(i))) {				
				minFitness = fitnessFn.apply(population.get(i));
			}
		}


		// Escalado del fitness. Le restamos el minFitness a todos
		for (int i = 0; i < population.size(); i++) {
			fValues[i] -= minFitness;
		}
		
		// Normalize the fitness values
		fValues = Util.normalize(fValues);

		double prob = random.nextDouble();
		double totalSoFar = 0.0;
		for (int i = 0; i < fValues.length; i++) {
			// Are at last element so assign by default
			// in case there are rounding issues with the normalized values
			totalSoFar += fValues[i];
			if (prob <= totalSoFar) {
				selected = population.get(i);
				break;
			}
		}

		selected.incDescendants();
		return selected;
	}

	// function REPRODUCE(x, y) returns an individual
	// inputs: x, y, parent individuals
	protected Individual<A> reproduce(Individual<A> x, Individual<A> y) {
		// n <- LENGTH(x);
		// Note: this is = this.individualLength
		// c <- random number from 1 to n
		int c = randomOffset(individualLength);
		// return APPEND(SUBSTRING(x, 1, c), SUBSTRING(y, c+1, n))
		List<A> childRepresentation = new ArrayList<A>();
		childRepresentation.addAll(x.getRepresentation().subList(0, c));
		childRepresentation.addAll(y.getRepresentation().subList(c, individualLength));
		
//		System.out.println("padre 1" + x.getRepresentation());
//		System.out.println("padre 2" + y.getRepresentation());
//		System.out.println("hijo   " + childRepresentation);
//		System.out.println("============================================================");

		return new Individual<A>(childRepresentation);
	}


	protected Individual<A> reproduceOX(Individual<A> x, Individual<A> y) {
		// n <- LENGTH(x);
		// Note: this is = this.individualLength
		// c <- random number from 1 to n

		int individualLength = x.length()-1;

		int p1 = randomOffset(individualLength);
		int p2 = randomOffset(individualLength);
		while(p2 < p1){
			p2 = randomOffset(individualLength);
		}
		System.out.println(p1);
		System.out.println(p2);
		List<A> xArray = x.getRepresentation();
		List<A> yArray = y.getRepresentation();
		List<A> offArray = new ArrayList<A>(xArray);

		HashMap<Integer,A> positions = new HashMap<>();

		for (int i = p1; i < p2; i++) {
			A aux = xArray.get(i);
			positions.put(yArray.indexOf(aux), aux);
		}

		for (int i = 0; i < individualLength; i++) {
			if(positions.containsKey(i)){
				offArray.set(p1, positions.get(i));
				positions.remove(i);
				p1++;
			}
		}
		System.out.println("padre 1" + x.getRepresentation());
		System.out.println("padre 2" + y.getRepresentation());
		System.out.println("hijo   " + offArray);
		System.out.println("============================================================");

		return new Individual<A>(offArray);
	}

	public Individual<A> reproduce3(Individual<A> firstParent, Individual<A> secondParent) {
		// n <- LENGTH(x);
		// Note: this is = this.individualLength
		// c <- random number from 1 to n

		int individualLength = firstParent.length();

		int p1 = randomOffset(individualLength);
		int p2 = randomOffset(individualLength);
		
		System.out.println(p1);
		System.out.println(p2);
		List<A> xArray = firstParent.getRepresentation();
		List<A> yArray = secondParent.getRepresentation();
		List<A> offArray = new ArrayList<A>(xArray);

		// Keep the substring from p1 to p2-1 to the offspring, order and
		// position
		// The remaining genes, p2 to p1-1, from the second parent, relative
		// order
		int k = p2;
		for (int i = 0; i < individualLength; i++) {
			int j = p1;
			while (j < p2 + (p2 <= p1 ? individualLength : 0) && yArray.get(i) != xArray.get(j % individualLength))
				j++;
			if (j == p2 + (p2 <= p1 ? individualLength : 0)) { // yArray[i] is
				// not in
				// offArray from
				// p1 to p2-1
				offArray.set(k % individualLength, yArray.get(i));
				k++;
			}
		}
		System.out.println("padre 1" + xArray);
		System.out.println("padre 2" + yArray);
		System.out.println("hijo   " + offArray);
		System.out.println("============================================================");
		return new Individual<A>(offArray);
	}

	protected Individual<A> mutate(Individual<A> child) {
		int mutateOffset = randomOffset(individualLength);
		int alphaOffset = randomOffset(finiteAlphabet.size());

		List<A> mutatedRepresentation = new ArrayList<A>(child.getRepresentation());

		mutatedRepresentation.set(mutateOffset, finiteAlphabet.get(alphaOffset));

		return new Individual<A>(mutatedRepresentation);
}

	protected Individual<A> mutate2(Individual<A> child) {
		int individualLength = child.length();
		int p = randomOffset(individualLength - 1);
		int c = randomOffset(individualLength - 1);
		ArrayList<A> mutatedRepresentation = new ArrayList<A>(child.getRepresentation());
		A temp = mutatedRepresentation.get(p);
		mutatedRepresentation.set(p, mutatedRepresentation.get(c));
		mutatedRepresentation.set(c, temp);
		return (new Individual<A>(mutatedRepresentation));
	}

	protected int randomOffset(int length) {
		return random.nextInt(length);
	}

	protected void validatePopulation(Collection<Individual<A>> population) {
		// Require at least 1 individual in population in order
		// for algorithm to work
		if (population.size() < 1) {
			throw new IllegalArgumentException("Must start with at least a population of size 1");
		}
		// String lengths are assumed to be of fixed size,
		// therefore ensure initial populations lengths correspond to this
		for (Individual<A> individual : population) {
			if (individual.length() != this.individualLength) {
				throw new IllegalArgumentException("Individual [" + individual
						+ "] in population is not the required length of " + this.individualLength);
			}
		}
	}
	
	private void notifyProgressTrackers(int itCount, Collection<Individual<A>> generation) {
		for (ProgressTracker<A> tracer : progressTrackers)
			tracer.trackProgress(getIterations(), generation);
	}
	
	/**
	 * Interface for progress tracers.
	 * 
	 * @author Ruediger Lunde
	 */
	public interface ProgressTracker<A> {
		void trackProgress(int itCount, Collection<Individual<A>> population);
	}
}