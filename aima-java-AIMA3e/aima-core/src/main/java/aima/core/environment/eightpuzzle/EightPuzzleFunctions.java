package aima.core.environment.eightpuzzle;

import aima.core.agent.Action;
import aima.core.search.framework.Node;
import aima.core.util.datastructure.XYLocation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Useful functions for solving EightPuzzle problems.
 * 
 * @author Ruediger Lunde
 */
public class EightPuzzleFunctions {

	// public static final EightPuzzleBoard GOAL_STATE = new EightPuzzleBoard(new
	// int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });

	// public static final EightPuzzleBoard GOAL_STATE = new EightPuzzleBoard(new
	// int[] { 1, 2, 3, 8, 0, 4, 7, 6, 5 });
	//
	public static final EightPuzzleBoard GOAL_STATE = new EightPuzzleBoard(new int[] { 1, 2, 3, 8, 0, 4, 7, 6, 5 });

	public static List<Action> getActions(EightPuzzleBoard state) {
		return Stream.of(EightPuzzleBoard.UP, EightPuzzleBoard.DOWN, EightPuzzleBoard.LEFT, EightPuzzleBoard.RIGHT)
				.filter(state::canMoveGap).collect(Collectors.toList());
	}

	public static EightPuzzleBoard getResult(EightPuzzleBoard state, Action action) {
		EightPuzzleBoard result = state.clone();

		if (result.canMoveGap(action)) {
			if (action == EightPuzzleBoard.UP)
				result.moveGapUp();
			else if (action == EightPuzzleBoard.DOWN)
				result.moveGapDown();
			else if (action == EightPuzzleBoard.LEFT)
				result.moveGapLeft();
			else if (action == EightPuzzleBoard.RIGHT)
				result.moveGapRight();
		}
		return result;
	}

	public static double stepCostFunction(EightPuzzleBoard state, Action action, EightPuzzleBoard resultState) {

		XYLocation gapPos = state.getLocationOf(0);
		int ficha = resultState.getValueAt(gapPos);
		return Math.pow(2, ficha);


	}

	// h = 0

	public static double nullheuristic(Node<EightPuzzleBoard, Action> node) {
		return 0.0;
	}

	// SE MULTIPLICA POR LA POTENCIA PARA QUE EL ALGORITMO ESTÉ MAS INFORMADO

	// h1
	public static int getWeightNumberOfMisplacedTiles(Node<EightPuzzleBoard, Action> node) {
		EightPuzzleBoard currState = node.getState();
		int result = 0;
		for (int val = 1; val <= 8; val++)
			if (!(currState.getLocationOf(val).equals(GOAL_STATE.getLocationOf(val))))
				result += Math.pow(2, val); // sumamos 2^ficha
		return result;
	}

	// h2
	public static double getWeightManhattanDistance(Node<EightPuzzleBoard, Action> node) {
		EightPuzzleBoard currState = node.getState();
		int result = 0;
		for (int val = 1; val <= 8; val++) {
			XYLocation locCurr = currState.getLocationOf(val);
			XYLocation locGoal = GOAL_STATE.getLocationOf(val);
			result += (Math.abs(locGoal.getX() - locCurr.getX())) * Math.pow(2, val); // multiplicamos 2^ficha
			result += (Math.abs(locGoal.getY() - locCurr.getY())) * Math.pow(2, val); // multiplicamos 2^ficha
		}
		return result;
	}

	// h3

	public static double nonConsistentHeuristic(Node<EightPuzzleBoard, Action> node) {

		EightPuzzleBoard currState = node.getState();
		int result = 0;
		for (int val = 1; val <= 8; val++) {
			XYLocation locCurr = currState.getLocationOf(val);
			XYLocation locGoal = GOAL_STATE.getLocationOf(val);
			int distance = Math.abs(locGoal.getX() - locCurr.getX()) + Math.abs(locGoal.getY() - locCurr.getY());
			if (distance == 2)
				result += distance * Math.pow(2, val);
		}
		return result;
	}

	// PEA
	public static double getEpsilonWeigthedManhattanDistance(Node<EightPuzzleBoard, Action> node) {
		double epsilon = 0.1; // >= 0
		return getWeightManhattanDistance(node) * (1 + epsilon);
	}

	public static double getManhattanDistance(Node<EightPuzzleBoard, Action> node) {
		EightPuzzleBoard currState = node.getState();
		int result = 0;
		for (int val = 1; val <= 8; val++) {
			XYLocation locCurr = currState.getLocationOf(val);
			XYLocation locGoal = GOAL_STATE.getLocationOf(val);
			result += Math.abs(locGoal.getX() - locCurr.getX());
			result += Math.abs(locGoal.getY() - locCurr.getY());
		}
		return result;
	}

	public static int getNumberOfMisplacedTiles(Node<EightPuzzleBoard, Action> node) {
		EightPuzzleBoard currState = node.getState();
		int result = 0;
		for (int val = 1; val <= 8; val++)
			if (!(currState.getLocationOf(val).equals(GOAL_STATE.getLocationOf(val))))
				result++;
		return result;
	}

}