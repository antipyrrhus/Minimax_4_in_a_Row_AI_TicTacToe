import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javax.swing.*;

/** 
 *  @author Yury Park (starter code provided by Dr. Shuting Xu)
 *  @version 1.0 <p>
 *  
 *  This class - A special version of tic-tac-toe where you must make 4 in a row.
 *  Played on a 6x6 board. This is a one-player version where the human player goes first
 *  and the AI goes second.
 *  
 *  The AI uses a modified minimax algorithm along with alpha-beta pruning and iterative
 *  deepening search method, which keeps on searching deeper and deeper levels until
 *  it hits a specified time limit (determined by the coder), at which time the current search
 *  aborts and, unless the current search has had a chance to yield a better result than
 *  the previous COMPLETED level search, the result from the previous search is used to determine
 *  AI's next move.
 *  
 *  The minimax algorithm is also designed to optimize search speed by
 *  starting its analysis with grids that are close to the human player's last-played grid,
 *  as opposed to always starting with the grid at row 0, col 0.
 *  
 *  Furthermore, in each subsequent step of the iterative deepening process, all moves that have
 *  been determined in the previous level search to be "inferior" are eliminated from the
 *  search tree, meaning that in each subsequent iteration, the algorithm will often have
 *  fewer nodes that it has to search, resulting in a greater time efficiency.
 *  
 *  The minimax algorithm optimizes speed further still by quitting the iterative deepening
 *  search process immediately in one of the following scenarios:
 *  
 *  1) as soon as the fastest GUARANTEED winning move is found (i.e. a move that will guarantee
 *     that AI wins in the fewest possible moves), OR
 *  2) as soon as the depth level exceeds the number of empty grids left (e.g. there is no reason to
 *     keep running minimax 11 levels deep if there are only 10 empty grids left), OR
 *  3) as soon as it realizes that its BEST MOVE is still a GUARANTEED losing move (it's useless
 *     to search level 9, for example, if the AI realizes on level 8 that even its best move is a
 *     losing one), OR
 *  4) as soon as the AI realizes that there is ONLY ONE non-losing move (this is limited to
 *     when the AI is searching level 2 depth, where it does NOT use alpha-beta pruning. See
 *     the part of the code beginning with:
 *     
 *     if(tempMaxLvl == 2) {
 *     
 *     and the associated comments for more details on this design decision.) 
 *  
 *  
 *  To make the AI more intelligent, the algorithm determines the max / min "score" not just
 *  in terms of three values (e.g. -1, 0 and 1), but rather in terms of various values that
 *  differ depending on how quickly the AI can reach a guaranteed win condition (the quicker
 *  the AI can do this, the better score for AI), and/or how long the AI can stay alive in a
 *  guaranteed losing situation (the longer the AI can stay alive in this condition, the
 *  better score for AI).
 *  
 *  Purpose - To test minimax, alpha-beta-pruning, iterative deepening search, and various
 *  other optimization methods in a special game of tic-tac-toe as described above.
 *  
 */
@SuppressWarnings("serial")
public class Game extends JFrame {
	// Named-constants for the game board
	public static final int ROWS = 6;  // TODO ROWS by COLS cells are adjustable.
	public static final int COLS = 6;

	// Named-constants of the various dimensions used for graphics drawing
	public static final int CELL_SIZE = 90; // cell width and height (square)
	public static final int CANVAS_WIDTH = CELL_SIZE * COLS;  // the drawing canvas
	public static final int CANVAS_HEIGHT = CELL_SIZE * ROWS;
	public static final int GRID_WIDTH = 5;                   // Grid-line's width
	public static final int GRID_WIDTH_HALF = GRID_WIDTH / 2; // Grid-line's half-width

	// Symbols (cross/nought) are displayed inside a cell, with padding from border
	public static final int CELL_PADDING = CELL_SIZE / 6;
	public static final int SYMBOL_SIZE = CELL_SIZE - CELL_PADDING * 2; // width/height
	public static final int SYMBOL_STROKE_WIDTH = 8; // pen's stroke width

	public static final int HOWMANY_IN_A_ROW_TO_WIN = 4;	//must have 4 in a row to win this game - TODO this is adjustable

	public static final String TITLE = "Tic Tac Toe";	//title to display on top of window

	/* debug mode. Set to false by default. If set to true, this will print a whole bunch
	 * of stuff to the console. (Not recommended for normal play) */
	public static final boolean debugOn = false;

	/* Debug 2 mode. Set to true by default. This will print some of the more important
	 * debugging information to the console. (OK for normal play) */
	public static final boolean debug2On = true;

	private int playersLastPlayedRow;	//saves the row / col of the human player's previously played grid.
	private int playersLastPlayedCol;

	private boolean winningMove = false;	//keeps track of whether AI's move is a guaranteed winning move.

	/* NEW optimization feature: a separate global var that keeps track of nodes that each subsequent iteration
	 * of minimax algorithm should explore. All nodes that have been determined in the previous level search to
	 * be "inferior" moves for the AI to make are eliminated from the search tree, and the remaining available
	 * nodes are saved in this var, such that in each subsequent iteration of minimax, the algorithm will often have
	 * fewer nodes that it has to search, resulting in a greater time efficiency. */
	private ArrayList<int[]> savedGridsToCheckForIterativeMinimax;

	//TODO this time limit is adjustable. This is in milliseconds.
	private final long TIMELIMIT = 70000;	//time limit (in milliseconds) allotted to AI's minimax method. 
	private boolean timeIsUp;	//keeps track of whether minimax is taking longer than the specified TIMELIMIT. If so, minimax will instantly abort.

	private int[] nextMove;		//The row and col of AI's next move, as determined by minimax
	private double bestMoveValue;	//the score value (as returned by minimax) of AI's best move.

	private boolean aiThinking; //set to true while minimax is running.		

	// Use an enumeration (inner class) to represent the various states of the game
	public enum GameState {
		PLAYING, DRAW, CROSS_WON, NOUGHT_WON
	}
	private GameState currentState;  // the current game state

	// Use an enumeration (inner class) to represent the seeds and cell contents
	public enum Seed {
		EMPTY, CROSS, NOUGHT
	}
	private Seed currentPlayer;  // the current player. Either Seed.CROSS or Seed.NOUGHT

	private Seed[][] board   ; // Game board of ROWS-by-COLS grids
	private DrawCanvas canvas; // Drawing canvas (JPanel) for the game board
	private JLabel statusBar;  // Status Bar

	/** 
	 * Constructor to setup the game and the GUI components
	 * */
	public Game() {
		canvas = new DrawCanvas();  // Construct a drawing canvas (a JPanel)
		canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

		// The canvas (JPanel) fires a MouseEvent upon mouse-click
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {  // mouse-clicked handler
				/* Only do the following if AI is NOT thinking (that is, if minimax is NOT running) */
				if(!aiThinking) {
					int mouseX = e.getX();
					int mouseY = e.getY();
					// Get the row and column clicked
					int rowSelected = mouseY / CELL_SIZE;
					int colSelected = mouseX / CELL_SIZE;

					/* If game is not over AND if it's human's turn... */
					if (currentState == GameState.PLAYING && currentPlayer == Seed.CROSS) {
						//check that the mouseclick is within bounds and that the clicked grid is empty
						if (rowSelected >= 0 && rowSelected < ROWS && colSelected >= 0
								&& colSelected < COLS && board[rowSelected][colSelected] == Seed.EMPTY) {
							board[rowSelected][colSelected] = currentPlayer; // fill the grid

							/* save the row and col of the filled grid. Used in minimax. */
							playersLastPlayedRow = rowSelected;	
							playersLastPlayedCol = colSelected;

							updateGame(currentPlayer, rowSelected, colSelected); // update state

							//If the human's move above has not ended the game...
							if(currentState == GameState.PLAYING) {
								// then switch player
								currentPlayer = Seed.NOUGHT;
								/* Instruct player to click again to start AI's turn 
								 * (I put this in here because immediately starting AI's turn
								 * without such prompt somehow caused the GUI to not update. Weird.) */
								statusBar.setText("Click anywhere to begin AI's turn");
							}
						}
						/* Else, if the game is not over AND it's AI's turn... */
					} else if(currentState == GameState.PLAYING && currentPlayer == Seed.NOUGHT) {
						/* Set this to true so that any further clicks by human player will
						 * have no effect as long as minimax is running */
						aiThinking = true;
						statusBar.setText("AI is thinking...");	//why doesn't this work? UPDATE: we need a Timer or multithread to ensure that the GUI updates while AI is thinking.
						repaint();								//why doesn't this work?

						/* =========================================================================
						 * =========================================================================
						 * ============== Get ready for iterative deepening minimax! ===============
						 * =========================================================================
						 * =========================================================================*/
						int tempMaxLvl = 1;	//we begin by only searching up to the 1st level
						long startTime = System.currentTimeMillis();	//To keep track of time limit...
						long endTime;
						long elapsedTime = 0;
						timeIsUp = false;		//To keep track of whether time limit has been reached for AI

						/* This will save a backup of this.nextMove, which is AI's computed next move
						 * as determined by minimax.
						 * 
						 * This is necessary because if time runs out in the middle of minimax
						 * (which will mostly likely happen at some point during this
						 * iterative deepening process), then that minimax session will be aborted 
						 * instantly, and that session may not have had the chance to find a new best move. 
						 * So in that case, we need to use the saved backup of the previous
						 * minimax session's best move. */
						int[] nextMoveBackup = new int[2];	//To keep a backup of [row, col] of AI's best move
						double bestMoveValueBackup = Double.NEGATIVE_INFINITY;	//we also keep a backup of the score value of AI's best move

						/* We're ready to begin the iterative deepening search process, starting at maxLevel of 1.
						 * Before we do so, we'll call custom method getAvailableMoves and save an arraylist
						 * of AI's available moves. Minimax will access this global variable at root level 0,
						 * and then will dynamically modify it as necessary in order to eliminate moves that
						 * have been confirmed to be "inferior." The result is that subsequent iterations of
						 * the minimax algorithm may have fewer moves that it needs to search through, 
						 * thus saving time. */
						savedGridsToCheckForIterativeMinimax = getAvailableMoves(playersLastPlayedRow, playersLastPlayedCol);

						/* Keep up the iterative deepening minimax as long as time limit is not reached,
						 * AND as long as a definitive winning move for AI has not been found,
						 * AND as long as the level doesn't exceed the number of empty grids (e.g. there is no reason to
						 * run minimax 11 levels deep if there are only 10 empty grids left).
						 * 
						 *  All these conditions eliminate needless iterations of minimax and save time. */
						while(!timeIsUp && winningMove == false && tempMaxLvl <= getAvailableMoves().size()) {

							/* EXCEPTION: to save time when AI is facing a loss condition in the next two turns unless AI
							 * immediately defends against it (e.g. the human player has made 3 in a row already and will
							 * win unless AI blocks it), we will run the minimax algorithm WITHOUT alpha-beta pruning
							 * when the tempMaxLvl is at 2. That is, the AI will do a comprehensive check of every grid
							 * up to the next TWO levels and see if there is ONLY ONE MOVE AI can make that will prevent
							 * an immediate loss for the AI. If such ONE MOVE is found, then this while-loop will
							 * immediately terminate and no further iterative deepening search will be performed, because
							 * it's useless to search further. */
							if(tempMaxLvl == 2) {
								minimax(currentPlayer, 0, tempMaxLvl++, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, playersLastPlayedRow, playersLastPlayedCol, startTime, TIMELIMIT, false);
								//if ONE move is found (see above comment), terminate further search
								if(savedGridsToCheckForIterativeMinimax.size() == 1) timeIsUp = true;
							}
							/* Barring the above EXCEPTION, we can run minimax with alpha-beta pruning! */
							else {
								minimax(currentPlayer, 0, tempMaxLvl, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, playersLastPlayedRow, playersLastPlayedCol, startTime, TIMELIMIT, true);
							}

							/* this boolean variable will be modified and checked within minimax method.
							 * If time limit is up, minimax is instantly aborted. */
							if(timeIsUp) {
								/* The current minimax (iteration) session is aborted upon time limit reached.
								 * However, there is still a chance that it came up with a better move (before the abort) than
								 * the previous minimax iteration did. We check this by seeing what the current bestMoveValue is.
								 * If it is less than the backup, then there is no improvement, and we might as well
								 * use the backup. */
								if(debug2On) System.out.println("bestMoveValue: " + bestMoveValue + "; " + "bestMoveValueBackup: " + bestMoveValueBackup);

								endTime = System.currentTimeMillis();	//Time is up. calculate end time
								elapsedTime = endTime - startTime;		//calculate elapsed time
								if(debug2On) System.out.printf("Elapsed time: %s, aborted while on depth level: %s\n", elapsedTime, tempMaxLvl);

								if(bestMoveValue < bestMoveValueBackup) {
									if(debug2On) {
										System.out.println("Time is up! The current iteration of minimax didn't have the time to verify "
												+ "a better move than the previously saved backup. Loading from backup..."
												+ "\nnextMove will be " + Arrays.toString(nextMoveBackup));
									}
									nextMove[0] = nextMoveBackup[0];
									nextMove[1] = nextMoveBackup[1];
								}
								/* Else, if the current iteration DID come up with a better or equal move value before
								 * being aborted, then we'll go ahead and use the nextMove instead of the backup. */
								else {
									if(debug2On) {
										System.out.println("time is up! Fortunately, minimax had the chance to verify an equal or better move "
												+ "during this iteration than the previously saved backup."
												+ "\nnextMove will be " + Arrays.toString(nextMove));
									}
								}
								//end if(bestMoveValue < bestMoveValueBackup) / else
							}

							/* Else, if time is not up, then minimax has successfully been completed
							 * for this iteration. So save a backup of this.nextMove */
							else{	
								endTime = System.currentTimeMillis();	//A minimax session is complete, and we haven't yet reached the time limit. calculate end time.
								elapsedTime = endTime - startTime;		//calculate elapsed time
								if(debug2On) System.out.printf("Elapsed time: %s, currently on depth level: %s\n", elapsedTime, tempMaxLvl);

								nextMoveBackup[0] = nextMove[0];
								nextMoveBackup[1] = nextMove[1];
								bestMoveValueBackup = bestMoveValue;
								if(debug2On) System.out.println("bestMoveValueBackup has been updated: " + bestMoveValueBackup);

								/* If the BEST move found is a losing move, then it's useless to search deeper levels. */
								if(bestMoveValueBackup < 0) timeIsUp = true;
							}
							//end if(timeIsUp) / else..

							bestMoveValue = Double.NEGATIVE_INFINITY;	//reset this global value before the next while loop iteration
							tempMaxLvl++;	//increment the level and get ready for next iteration
						}
						//end while(!timeIsUp && winningMove == false && tempMaxLvl <= getAvailableMoves().size())

						/* Now that the iterative minimax is over, it's time to actually make
						 * AI's move and update the board. */
						int rowSelectedByAI = nextMove[0];
						int colSelectedByAI = nextMove[1];
						board[rowSelectedByAI][colSelectedByAI] = currentPlayer;
						updateGame(currentPlayer, rowSelectedByAI, colSelectedByAI); // update state

						/* Now that AI is done for this turn, we reset these variables */
						aiThinking = false;
						winningMove = false;

						/* Switch player back to human */
						currentPlayer = Seed.CROSS;

					} else {       // Else, it means the game is over.
						initGame(); // restart the game upon human's mouseclick
					}
					//end if (currentState == GameState.PLAYING && currentPlayer == Seed.CROSS) / else if...

					// Refresh the drawing canvas
					repaint();  // Call-back paintComponent().
				}
				//end if(!aiThinking)
			}
			//end public void mouseClicked(MouseEvent e)
		});
		//end canvas.addMouseListener

		// Setup the status bar (JLabel) to display status message
		statusBar = new JLabel("  ");		//initially there's no message on the statusbar
		statusBar.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 15));
		statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 4, 5));

		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(canvas, BorderLayout.CENTER);
		cp.add(statusBar, BorderLayout.PAGE_END); // same as SOUTH

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();  // pack all the components in this JFrame
		setTitle(TITLE);	//set title for window
		setVisible(true);  // show this JFrame

		board = new Seed[ROWS][COLS]; // allocate array
		initGame(); // initialize the game board contents and game variables
	}
	//end public TTTGraphics2P()

	/** 
	 * Method: initGame()
	 * Initialize the game-board contents and the status 
	 * */
	public void initGame() {
		for (int row = 0; row < ROWS; ++row) {
			for (int col = 0; col < COLS; ++col) {
				board[row][col] = Seed.EMPTY; // all cells are initially empty.
			}
		}

		//initialize [row, col] of the next move AI will make, as will be determined by minimax. 
		this.nextMove = new int[2];

		this.aiThinking = false;	//AI is not thinking at start because human plays first.

		savedGridsToCheckForIterativeMinimax = new ArrayList<>();	//initialize this arraylist at start of each game. See comments at the top of this file for more info.

		//TODO We can perform testing on non-blank boards too. Just fill up a few of these grids and run.

		//TEST CASE #1. Just a basic check that the AI will take an immediate win when possible, regardless of the board structure.
//				board[1][1] = Seed.NOUGHT;
//				board[2][1] = Seed.NOUGHT;
//				board[3][1] = Seed.NOUGHT;
//				board[2][2] = Seed.CROSS;
//				board[3][2] = Seed.CROSS;

		//TEST CASE #2. In this case, the AI goes first and the board structure favors the AI. In fact, the AI is GUARANTEED
		//to win no matter what the human does. So if the AI cannot win here EVERY TIME, then there's something wrong with AI.
				board[2][2] = Seed.NOUGHT;
				board[3][3] = Seed.CROSS;
				board[1][3] = Seed.NOUGHT;

		//TEST CASE #3. In this case, if human plays perfectly the AI has no chance to win. So the AI should try
		//to stay alive as long as possible, and if human makes any mistake along the way, should capitalize on the mistake.
//			board[2][2] = Seed.NOUGHT;
//			board[1][3] = Seed.NOUGHT;
//			board[2][3] = Seed.CROSS;
//			board[3][3] = Seed.CROSS;

		currentState = GameState.PLAYING; // ready to play
		currentPlayer = Seed.CROSS;  // The human player, aka cross ("X"), plays first. TODO Note: If you want, this can be changed any time to Seed.NOUGHT so that AI plays first
		//		currentPlayer = Seed.NOUGHT;

		playersLastPlayedRow = -1;		//initialize human player's last move (row and col) to -1, since human hasn't made a move.
		playersLastPlayedCol = -1;
	}
	//end public void initGame()


	/**
	 * Method: updateGame
	 * Update the currentState of the game after the player with "theSeed" has placed the
	 * Seed symbol on (rowSelected, colSelected).
	 * 
	 * @param theSeed either Seed.CROSS "X" (human player) or Seed.NOUGHT "O" (AI)
	 * @param rowSelected the selected row index
	 * @param colSelected the selected col index
	 */
	public void updateGame(Seed theSeed, int rowSelected, int colSelected) {
		if (hasWon(theSeed)) {  // Custom method to check for game winning conditions
			currentState = (theSeed == Seed.CROSS ? GameState.CROSS_WON : GameState.NOUGHT_WON);
		} else if (isDraw()) {  // Custom method to check whether the game has ended with a draw
			currentState = GameState.DRAW;
		}
		// Otherwise, no change to current state (still GameState.PLAYING).
	}
	//end public void updateGame

	/**
	 * Method: isDraw()
	 * Return true if the game has ended in a draw (i.e., no more empty cell)
	 * 
	 * Note: It is assumed that this method is invoked after having separately verified
	 * that the game does not yet have a winner. So all this method does is check
	 * whether all cells are occupied, in which case it immediately determines it's a draw.
	 * 
	 * @return true if the game is a draw, false otherwise
	 */
	public boolean isDraw() {
		for (int row = 0; row < ROWS; ++row) {
			for (int col = 0; col < COLS; ++col) {
				if (board[row][col] == Seed.EMPTY) {
					return false; // an empty cell found. Thus the game cannot be a draw. Exit
				}
			}
		}
		return true;  // no more empty cell, it's a draw
	}

	/** Return true if the player with "theSeed" has won after placing at
       (rowSelected, colSelected) 
       UPDATE: this method is replaced with the one below, because we're no longer checking
       for just three in a row...*/
	//   public boolean hasWon(Seed theSeed, int rowSelected, int colSelected) {
	//      return (board[rowSelected][0] == theSeed  // 3-in-the-row
	//            && board[rowSelected][1] == theSeed
	//            && board[rowSelected][2] == theSeed
	//       || board[0][colSelected] == theSeed      // 3-in-the-column
	//            && board[1][colSelected] == theSeed
	//            && board[2][colSelected] == theSeed
	//       || rowSelected == colSelected            // 3-in-the-diagonal
	//            && board[0][0] == theSeed
	//            && board[1][1] == theSeed
	//            && board[2][2] == theSeed
	//       || rowSelected + colSelected == 2  // 3-in-the-opposite-diagonal
	//            && board[0][2] == theSeed
	//            && board[1][1] == theSeed
	//            && board[2][0] == theSeed);
	//   }

	/**
	 * Method: hasWon
	 * Checks to see if the player represented by the given Seed has won the game.
	 * 
	 * @param theSeed will be either Seed.CROSS (human) or Seed.NOUGHT (AI)
	 * @return true if theSeed has won the game, false otherwise.
	 */
	public boolean hasWon(Seed theSeed) {
		int connectCounter = 1;	//initialize a var to keep track of how many connections in a row

		/* Check each row for win condition via nested loop */
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length - 1; j++) {
				Seed first = board[i][j];
				Seed second = board[i][j+1];
				if(first == second && second == theSeed) {	//if we have two in a row...
					connectCounter++;	//then increment counter
					if(connectCounter == HOWMANY_IN_A_ROW_TO_WIN) return true;	//if we have 4 in a row, return true
				}
				else connectCounter = 1;	//otherwise reset the counter
			}
			//end for j
			connectCounter = 1;	//after the inner loop is over, reset the counter before checking the next row 
		}
		//end for i

		/* If the above nested loop hasn't determined a winner, then it's time to check each column...
		 * The logic of the nested loop is similar to the one above */
		connectCounter = 1;
		for(int j = 0; j < board[0].length; j++) {
			for(int i = 0; i < board.length - 1; i++) {
				Seed first = board[i][j];
				Seed second = board[i+1][j];
				if(first == second && second == theSeed) {
					connectCounter++;
					if(connectCounter == HOWMANY_IN_A_ROW_TO_WIN) return true;
				}
				else connectCounter = 1;
			}
			connectCounter = 1;
		}

		/* Still no winner? Let's do NW diagonal search. Checks the upper-right corner of the board,
		 * including the longest diagonal in the middle of the board. */
		for(int j = 0; j < board[0].length; j++) {
			if(checkDiagonalNW(0, j, theSeed) == true) return true;	//custom method
		}

		/* NW diagonal search. Checks the lower-left corner of the board, EXCLUDING
		 * the longest diagonal in the middle of the board. */
		for(int i = 1; i < board.length; i++) {
			if(checkDiagonalNW(i, 0, theSeed) == true) return true; //custom method
		}

		/* NE diagonal search. Checks the upper-left corner of the board,
		 * including the longest diagonal in the middle of the board. */
		for(int j = board[0].length - 1; j >= 0; j--) {
			if(checkDiagonalNE(0, j, theSeed) == true) return true; //custom method
		}

		/* NE diagonal search. Checks the lower-right corner of the board,
		 * excluding the longest diagonal in the middle of the board. */
		for(int i = 1; i < board.length; i++) {
			if(checkDiagonalNE(i, board[i].length - 1, theSeed) == true) return true; //custom method
		}

		/* If NONE of the above tests has determined a winner, then, well, we don't have a winner yet. */
		return false;
	}

	/**
	 * Method: checkDiagonalNW
	 * Checks a single diagonal from the NW corner to the SE corner of the board, given the
	 * starting row and starting column indices.
	 * 
	 * @param startRow starting row index at which to begin the diagonal search
	 * @param startCol starting col index at which to begin the diagonal search
	 * @param theSeed the seed representing either human (Seed.CROSS) or AI player (Seed.NOUGHT)
	 * 
	 * @return true if the requisite number of connections in a row have been found
	 * in this diagonal direction for the given seed. False otherwise.
	 */
	public boolean checkDiagonalNW(int startRow, int startCol, Seed theSeed) {
		int i = startRow; int j = startCol;
		int connectCount = 1;
		while(i + 1 < board.length && j + 1 < board[i].length) {
			if(board[i][j] == board[i+1][j+1] && board[i][j] == theSeed) {
				connectCount++;
				if(connectCount == HOWMANY_IN_A_ROW_TO_WIN) return true;
			}
			else connectCount = 1;
			i++; j++;	//increment both the row and column indices since we're checking from NW to SE diagonal
		}
		return false;
	}

	/**
	 * Method: checkDiagonalNE
	 * Checks a single diagonal from the NE corner to the SW corner of the board, given 
	 * the starting row and starting column indices.
	 * 
	 * @param startRow starting row index at which to begin the diagonal search
	 * @param startCol starting col index at which to begin the diagonal search
	 * @param theSeed the seed representing either human (Seed.CROSS) or AI player (Seed.NOUGHT)
	 * 
	 * @return true if the requisite number of connections in a row have been found
	 * in this diagonal direction for the given seed. False otherwise.
	 */
	public boolean checkDiagonalNE(int startRow, int startCol, Seed theSeed) {
		int i = startRow; int j = startCol;
		int connectCount = 1;
		while(i + 1 < board.length && j - 1 >= 0) {
			if(board[i][j] == board[i+1][j-1] && board[i][j] == theSeed) {
				connectCount++;
				if(connectCount == HOWMANY_IN_A_ROW_TO_WIN) return true;
			}
			else connectCount = 1;
			i++; j--;	//increment row index but decrement the column index since we're checking from NE to SW diagonal
		}
		return false;
	}

	/**
	 *  Inner class DrawCanvas (extends JPanel) used for custom graphics drawing.
	 */
	class DrawCanvas extends JPanel {
		@Override
		public void paintComponent(Graphics g) {  // invoke via repaint()
			super.paintComponent(g);    // fill background
			setBackground(Color.WHITE); // set its background color

			// Draw the grid-lines
			g.setColor(Color.LIGHT_GRAY);
			for (int row = 1; row < ROWS; ++row) {
				g.fillRoundRect(0, CELL_SIZE * row - GRID_WIDTH_HALF,
						CANVAS_WIDTH-1, GRID_WIDTH, GRID_WIDTH, GRID_WIDTH);
			}
			for (int col = 1; col < COLS; ++col) {
				g.fillRoundRect(CELL_SIZE * col - GRID_WIDTH_HALF, 0,
						GRID_WIDTH, CANVAS_HEIGHT-1, GRID_WIDTH, GRID_WIDTH);
			}

			// Draw the Seeds of all the cells if they are not empty
			// Use Graphics2D which allows us to set the pen's stroke
			Graphics2D g2d = (Graphics2D)g;
			g2d.setStroke(new BasicStroke(SYMBOL_STROKE_WIDTH, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));  // Graphics2D only
			for (int row = 0; row < ROWS; ++row) {
				for (int col = 0; col < COLS; ++col) {
					int x1 = col * CELL_SIZE + CELL_PADDING;
					int y1 = row * CELL_SIZE + CELL_PADDING;
					if (board[row][col] == Seed.CROSS) {
						g2d.setColor(Color.RED);
						int x2 = (col + 1) * CELL_SIZE - CELL_PADDING;
						int y2 = (row + 1) * CELL_SIZE - CELL_PADDING;
						g2d.drawLine(x1, y1, x2, y2);
						g2d.drawLine(x2, y1, x1, y2);
					} else if (board[row][col] == Seed.NOUGHT) {
						g2d.setColor(Color.BLUE);
						g2d.drawOval(x1, y1, SYMBOL_SIZE, SYMBOL_SIZE);
					}
				}
			}

			// Print status-bar message
			if (currentState == GameState.PLAYING) {
				statusBar.setForeground(Color.BLACK);
				if(aiThinking) {
					statusBar.setText("AI is thinking...");
				}
				else if (currentPlayer == Seed.CROSS) {
					statusBar.setText("X's Turn. Make your move");
				} else {
					statusBar.setText("Click anywhere to begin AI's Turn");
				}
			} else if (currentState == GameState.DRAW) {
				statusBar.setForeground(Color.RED);
				statusBar.setText("It's a Draw! Click to play again.");
			} else if (currentState == GameState.CROSS_WON) {
				statusBar.setForeground(Color.RED);
				statusBar.setText("'X' Won! Click to play again.");
			} else if (currentState == GameState.NOUGHT_WON) {
				statusBar.setForeground(Color.RED);
				statusBar.setText("'O' Won! Click to play again.");
			}
		}
	}
	//end class DrawCanvas

	/**
	 * Method: getAvailableMoves
	 * @return an Arraylist containing int[] arrays consisting of all the index no's
	 * of the board grids [row, col] that are empty. Used by AI in minimax() method.
	 * 
	 * UPDATE: for greater efficiency, the AI's minimax no longer uses this method.
	 * Instead, see the OVERLOADED method below.
	 */
	public ArrayList< int[] > getAvailableMoves() {
		ArrayList< int[] > ret = new ArrayList<>();
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				if(board[i][j] == Seed.EMPTY) {
					//					int[] temp = new int[2];
					//					temp[0] = i; temp[1] = j;
					//					ret.add(temp);
					ret.add(new int[]{i, j});
				}
			}
		}
		return ret;
	}
	//end public ArrayList< ArrayList<Integer> > getAvailableMoves

	/**
	 * Method: getAvailableMoves (overloaded method)
	 * 
	 * @param row the row index of the human player's previously played grid
	 * @param col the col index of the human player's previously played grid
	 * 
	 * @return an Arraylist containing int[] Arrays consisting of all the index no's
	 * of the board grids [row, col] that are empty. The inner int[] Arrays are SORTED
	 * in ascending order, starting with the grids CLOSEST to the human's previously played grid.
	 * This improves the average efficiency of the minimax method. See the following example:
	 * 
	 * | | | | | | |
	 * | | | | | | |
	 * | | | | | | |
	 * | | | | | | |
	 * | | | | |X| |
	 * | | | | | | |
	 * 
	 * Assuming the human player made the first move as seen above, this algorithm will first check the immediately surrounding
	 * areas and save any empty grids, as shown....
	 * | | | | | | |
	 * | | | | | | |
	 * | | | | | | |
	 * | | | |*|*|*|
	 * | | | |*|X|*|
	 * | | | |*|*|*|
	 * 
	 * ...then it'll branch out to the next immediately surrounding area and save any empty grids....
	 * 
	 * | | | | | | |
	 * | | | | | | |
	 * | | |*|*|*|*|
	 * | | |*| | | |
	 * | | |*| |X| |
	 * | | |*| | | |
	 * 
	 * ...then the next area...
	 * 
	 * | | | | | | |
	 * | |*|*|*|*|*|
	 * | |*| | | | |
	 * | |*| | | | |
	 * | |*| | |X| |
	 * | |*| | | | |
	 * 
	 * ...then the next...
	 * 
	 * |*|*|*|*|*|*|
	 * |*| | | | | |
	 * |*| | | | | |
	 * |*| | | | | |
	 * |*| | | |X| | 
	 * |*| | | | | |
	 *  
	 * ...and it's done saving any and all empty grids, in the order shown above.
	 * 
	 * This method is invoked by AI in minimax() method.
	 */
	public ArrayList< int[] > getAvailableMoves(int row, int col) {
		ArrayList< int[] > ret = new ArrayList<>();

		/* we'll begin with all empty grids that are a distance of 1 grid apart from the given row, col. 
		 * A diagonal distance of 1 still counts as 1. See the example illustration given above. 
		 * */
		int spacing = 1;	
		boolean keepGoing = true;	//sentinel value to check whether the while loop below should continue

		/* Begin while loop */
		while(keepGoing) {
			int topRow = row - spacing;
			int leftCol = col - spacing;
			int rightCol = col + spacing;
			int botRow = row + spacing;

			/* if all 4 sides to check are out of bounds, then the while loop will end. */
			if(topRow < 0 && rightCol >= this.board[0].length && botRow >= this.board.length && leftCol < 0) {
				keepGoing = false;
				continue;	//skip to the end of this loop
			}

			if(topRow >= 0) {	//if the top row is within bounds...
				/* ...check all the columns starting from leftCol to rightCol (provided that they're not out of bounds.
				 * Uses conditional statement.*/
				for(int j = (leftCol < 0 ? 0 : leftCol); j <= (rightCol >= this.board[0].length ? this.board[0].length - 1 : rightCol); j++) {
					if(board[topRow][j] == Seed.EMPTY) { 	//add any empty grids to the arraylist that will be returned
						//						int[] temp = new int[2];
						//						temp[0] = topRow; temp[1] = j;
						//						ret.add(temp);
						ret.add(new int[]{topRow, j});
					}
				}
			}

			if(rightCol < this.board[0].length) {	//if the right column is within bounds...
				/* ...check all the rows starting from the topRow + 1 (the +1 is because we presumably already checked
				 * the top row in the immediately above loop) -- or, if topRow is out of bounds, then begin with row index 0.
				 * Check all the way to the botRow, provided it's not out of bounds. */
				for(int i = (topRow < 0 ? 0 : topRow + 1); i <= (botRow >= this.board.length ? this.board.length - 1 : botRow); i++) {
					if(board[i][rightCol] == Seed.EMPTY) {	//add any empty grids to the arraylist that will be returned
						//						int[] temp = new int[2];
						//						temp[0] = i; temp[1] = rightCol;
						//						ret.add(temp);
						ret.add(new int[]{i, rightCol});
					}
				}
			}

			/* The rest of these loops follow a similar logic as the loops above. */
			if(botRow < this.board.length) {
				for(int j = (leftCol < 0 ? 0 : leftCol); j <= (rightCol >= this.board[0].length ? this.board[0].length - 1 : rightCol - 1); j++) {
					if(board[botRow][j] == Seed.EMPTY) {
						//						int[] temp = new int[2];
						//						temp[0] = botRow; temp[1] = j;
						//						ret.add(temp);
						ret.add(new int[]{botRow, j});
					}
				}
			}

			if(leftCol >= 0) {
				for(int i = (topRow < 0 ? 0 : topRow + 1); i <= (botRow >= this.board.length ? this.board.length - 1 : botRow - 1); i++) {
					if(board[i][leftCol] == Seed.EMPTY) {
						//						int[] temp = new int[2];
						//						temp[0] = i; temp[1] = leftCol;
						//						ret.add(temp);
						ret.add(new int[]{i, leftCol});
					}
				}
			}

			spacing++;	//increment spacing and get ready for the next while loop iteration
		}

		//		System.out.println(ret);	//testing

		return ret;
	}

	/**
	 * Method: randomize
	 * Chooses a random index among the number of available moves as given by the parameter. Used by minimax method.
	 * 
	 * @param numAvailMoves an int representing the total number of available moves.
	 * @return a random int representing the index no. of the available moves array that is used by minimax.
	 */
	public int randomize(int numAvailMoves) {
		//		Random rand = new Random();
		//		int index = rand.nextInt(numAvailMoves); 
		//		return index;
		return new Random().nextInt(numAvailMoves);
	}

	/**
	 * The minimax algorithm. The heart of AI's thought process.
	 * 
	 * @param currSeed the Seed symbol representing either the human (Seed.CROSS) or AI (Seed.NOUGHT).
	 * @param level the current level of the branching depth at which this method is being run. Root level is assumed to be 0.
	 *        Successive recursive calls will occur at level 1, level 2, level 3...and so on.
	 * @param maxLevel the maximum level of the branching depth which the AI is allowed to explore.
	 *        Can range from 0 to whatever positive number. Note that a large positive number might cause
	 *        this method to take too much time...
	 * @param alpha the alpha value. Used for alpha-beta pruning.
	 * @param beta the beta value. Used for alpha-beta pruning.
	 * @param row an int representing the row index of the previous player's (either human's or AI's) chosen grid.
	 * @param col an int representing the col index of the previous player's (either human's or AI's) chosen grid.
	 * @param startTime the time at which this minimax method was first invoked at the root level. Used to keep track of time limit.
	 * @param timeLimit the maximum time limit allowed for this minimax to run. If time limit is reached at any point during
	 *        this method or any recursions thereof, then the entire method and any recursions thereof are instantly aborted.
	 * @param abPruningisOn a boolean value that checks whether Alpha-Beta pruning is on. If set to true, then this minimax
	 *        method will use alpha-beta pruning to prune unnecessary branches (and return a default numerical value of
	 *        -Infinity for pruned nodes), thereby improving the time efficiency at the cost of not assigning a definite numerical value
	 *        to every grid. If set to false, this method will do a comprehensive check of every grid (at a much greater time-cost)
	 *        and will come up with a definite numerical value for every possible move.
	 * @return either the min. score value if it's the player's turn, or max score value if it's the AI's turn.
	 */
	public double minimax(Seed currSeed, int level, int maxLevel, double alpha, double beta, int row, int col, long startTime, long timeLimit, boolean abPruningisOn) {
		if(debugOn) System.out.printf("Checking level %s (%s)\n", level, (currSeed == Seed.CROSS ? "MIN(player)" : "MAX(AI)"));  //testing

		/* Base case where it's known that time limit has been reached. */
		if(timeIsUp) return Double.NEGATIVE_INFINITY;	//return dummy value if time limit has been reached

		/* Base case where we check if time limit has been reached */
		//		long currentTime = System.currentTimeMillis();
		if(System.currentTimeMillis() - startTime > timeLimit) {	//if elapsed time is over the time limit...
			/* set boolean value to true so that all other branches of this recursive method can instantly return dummy value
			 * as seen above without re-calculating the elasped time. */
			timeIsUp = true;	
			return Double.NEGATIVE_INFINITY;	//dummy value
		}

		/* Base case where the human player wins. */
		if(this.hasWon(Seed.CROSS)) {	//human wins
			if(debugOn) {	//debug mode only
				System.out.println("PLAYER WINS! " + (-1.0/level));
				print(board);
			}

			/* Although this result is bad for AI, we'll assume that the longer the AI stayed alive, the better.
			 * We'll stipulate that the more negative the score, the worse it is for AI, and vice versa.
			 * Note: the return value will ALWAYS be negative in this case. */
			return -1.0 / level;	//the greater the level depth, the better it is for AI (since AI stayed alive longer) and the "less negative" the return value is.
		}

		/* Base case where AI wins. */
		if(this.hasWon(Seed.NOUGHT)) {	//AI wins
			if(debugOn) {	//testing
				System.out.println("AI WINS! " + (double)maxLevel / level);	//for debug mode only
				print(board);
			}
			/* This is a great result for AI. We'll assume that the faster the AI gets to a winning position, the better.
			 * So if the AI can reach this winning position in the shortest level depth possible, the better (more positive)
			 * the return value will be.
			 * Note: the return value will ALWAYS be 2.0 or greater in this case. */
			return (level > 0 ? 2.0 + (double)maxLevel / level : 2.0);
		}

		/* Create an arraylist that will contain int[] Arrays representing the (row, col) values of
		 * available moves left on the board (that is, empty grids on the board) */
		ArrayList< int[] > availMovesArr = new ArrayList<>();

		/* If we're at the root level, we will save the list of available moves for AI to a local variable.
		 * The global variable savedGridsToCheckForIterativeMinimax is initialized and populated
		 * in advance, outside of this method. The global var may or may not contain ALL empty grids.
		 * See comments accompanying this global variable near the top of this file for more info. */
		if(level == 0) {
			for(int[] e : savedGridsToCheckForIterativeMinimax) {
				availMovesArr.add(e);
			}
			/* Now that the contents of this global var has been saved to local var, we will reset the global var
			 * so that it can be dynamically re-populated within this minimax method as appropriate. The
			 * purpose of doing this is to cull confirmed inferior nodes and re-populate the global var
			 * with a smaller total number of nodes, such that a subsequent iterative deepening minimax session
			 * at a higher maxLevel may have fewer total nodes to search through, thus saving time. */
			savedGridsToCheckForIterativeMinimax = new ArrayList<>();
		}
		/* Else, if we're NOT at the root level...then we just need to get all empty grids. */
		else {	
			/* Custom method to get all the (row, col) values of empty grids, sorted in such a way that the grid
			 * CLOSEST to the previous player's last played grid appears first in the Arraylist availMovesArr.
			 * See the comments in the getAvailableMoves() method for more details on how it works. */
			availMovesArr = getAvailableMoves(row, col);
		}

		if(level == 0 && debug2On) {	//testing
			System.out.printf("===================\nStarting minimax. maxLevel set to %s. \nAvailable moves:", maxLevel);
			printAL(availMovesArr);
		}

		/* Base case where there remain no empty grids, and we have a draw. 
		 * We'll again assume that the longer AI can play on in this situation, the better the result will be.
		 * Note: the return value will ALWAYS be between 0 and 1.0, inclusive. */
		if(availMovesArr.size() == 0) {
			if(debugOn) {	//testing
				System.out.println("Draw!");
				print(board);
			}
			return (maxLevel > 0 ? (double)level / maxLevel : 0);
		}

		/* Base case where this method has reached the maximum depth it is allowed to explore, and
		 * it's thus unknown whether AI would've won or lost had it been allowed to explore deeper. */
		if(level == maxLevel) {
			/* If this is the root level (maxLevel == 0), it means that AI won't be doing
			 * any thinking whatsoever. We still have to pick the next move for AI though. We'll just set it randomly
			 * via custom method call. */
			if(maxLevel == 0) {
				int index = this.randomize(availMovesArr.size());
				//				this.nextMove = availMovesArr.get(index);
				this.nextMove[0] = availMovesArr.get(index)[0];
				this.nextMove[1] = availMovesArr.get(index)[1];
				if(debug2On) System.out.println("this.nextMove (AI's next move): " + Arrays.toString(this.nextMove));
			}
			if(debugOn) {
				System.out.println("Unknown!");
				print(board);
			}
			/* We reached the max. level to which AI is allowed to go without finding a definite winning or losing position.
			   We will just return 1 in this case.*/
			return 1;
		}

		/* With the base cases taken care of as per above, we now initialize max and min, one of which we'll eventually return.
		 * We initialize max and min as the smallest possible and largest possible double value, respectively,
		 * so that whatever valid move we find below can instantly replace one of them.
		 * If this is AI's turn, we want to maximize the score for AI and we'll return max.
		 * If this is Human's turn, we want to minimize the score for AI and we'll return min. */
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		double current;	//this is the current score we'll get which may or may not be assigned to max or min.

		/* We'll keep an array list of the best (or the least bad) moves that AI can make, and
		 * we'll initialize a double value to keep track of the score value of the best move found so far. */
		ArrayList<int[]> bestChoices = new ArrayList<>();
		double bestChoiceSoFar = Double.NEGATIVE_INFINITY;

		/* AI now goes thru every available move and evaluates it */
		for(int i = 0; i < availMovesArr.size(); i++) {
			int rowSelected = availMovesArr.get(i)[0];	//get the row index
			int colSelected = availMovesArr.get(i)[1];	//get the col index
			if(debugOn) System.out.printf("Level: %s. Now checking node %s...alpha: %s, beta: %s\n", level, availMovesArr.get(i), alpha, beta);

			/* If it's the human's turn, we want to minimize AI's score, aka human wants the minimum score. */
			if(currSeed == Seed.CROSS) {	//MIN (human player's turn)
				board[rowSelected][colSelected] = currSeed; // Make the human player's move (this will be undone later)
				if(debugOn) this.print(board);	//debug mode only

				//recursively call minimax (increment level depth, toggle AI's turn)
				current = minimax(Seed.NOUGHT, level + 1, maxLevel, alpha, beta, rowSelected, colSelected, startTime, timeLimit, abPruningisOn);
				beta = Math.min(beta, current);	//keep track of beta
				if(debugOn) System.out.printf("beta for level %s, node %s is now set to %s.\n", level, availMovesArr.get(i), beta);
				if(current < min) min = current;	//Re-assign min if appropriate

				/* Alpha-beta pruning to increase time efficiency 
				 * Note: See http://cs.ucla.edu/~rosen/161/notes/alphabeta.html 
				 * if you forgot how alpha-beta pruning works.*/
				if(beta <= alpha && abPruningisOn) {
					currSeed = Seed.NOUGHT;	// toggle player turn
					if(debugOn) System.out.println("BETA <= ALPHA. Pruning during human's turn. Break!");
					board[rowSelected][colSelected] = Seed.EMPTY; // Undo the last move since we'll be breaking out of loop
					break;	//break out immediately because we can "prune" the rest of the branches (as in, we don't need to explore these branches)
				}
				//end if(beta <= alpha)
			}
			/* Else, if it's the AI's turn, we want to maximize AI's score. */
			else {	//MAX (ai's turn)
				board[rowSelected][colSelected] = Seed.NOUGHT; // Make the AI's move (this will be undone later)
				if(debugOn) this.print(board);	//debug mode only

				//recursively call minimax (increment level depth, toggle human turn)
				current = minimax(Seed.CROSS, level + 1, maxLevel, alpha, beta, rowSelected, colSelected, startTime, timeLimit, abPruningisOn);
				if(current > max) max = current;	//re-assign max if appropriate.
				alpha = Math.max(alpha, current);	//assign alpha value as appropriate.
				if(debugOn) System.out.printf("alpha for level %s, node %s is now set to %s.\n", level, availMovesArr.get(i), alpha);

				/* Alpha-beta pruning to increase time efficiency */
				if(alpha >= beta && abPruningisOn) {
					currSeed = Seed.CROSS;	// toggle player's turn
					if(debugOn) System.out.println("ALPHA >= BETA. Pruning during AI's turn. Break!");
					board[rowSelected][colSelected] = Seed.EMPTY; // Undo last move
					break;	//and break out
				}
				//end if(alpha >= beta)

				/* if we're at the root level of this method... */
				if(level == 0) {	
					if(debug2On) { 
						System.out.println("Value assigned to the best move AI can make so far: " + bestChoiceSoFar);	//...then print out the result for testing purposes
						System.out.printf("current value for grid column # %s : %s\n", Arrays.toString(availMovesArr.get(i)), current);
						System.out.println("current: " + current + " bestChoiceSoFar: " + bestChoiceSoFar);
					}

					/* update the best move variable as well as the bestChoices arraylist as appropriate. */
					if(current > bestChoiceSoFar) {
						if(current > 1.0) winningMove = true;	//any score above 1 is a GUARANTEED winning move for AI. Save it as this will save time.
						else winningMove = false;

						bestChoiceSoFar = current;	//save local variable bestChoiceSoFar. Every recursive branch level of this function needs its own local variable.
						bestMoveValue = current;	//save global variable bestMoveValue. This value will be reset outside of this function before this function is invoked at root level.
						if(debug2On) System.out.println("bestMoveValue updated: " + bestMoveValue);

						savedGridsToCheckForIterativeMinimax.clear();
						savedGridsToCheckForIterativeMinimax.add(availMovesArr.get(i));

						/* We want the bestChoices array to contain ONLY the best choices. So if we have a new bestChoiceSoFar,
						 * then we clear the arraylist of any previous elements and add the current move. */
						bestChoices.clear();
						bestChoices.add(availMovesArr.get(i));
					}

					/* Else, if we have a current result that's just as good as bestChoiceSoFar, then we just add the corresponding
					 * move to the arraylist without clearing the arraylist in advance. 
					 * As for the global variable savedGridsToCheckForIterativeMinimax, if alpha beta pruning is on,
					 * then we must assume that those values of -Infinity returned is due to pruning and NOT
					 * due to those grids being necessarily inferior, so we'll add the grids to the global variable regardless.
					 * Otherwise, if alpha beta pruning is off, then we'll add the grid to the global variable only if
					 * its value is EQUAL to the best choice so far. */
					else {
						if(abPruningisOn) savedGridsToCheckForIterativeMinimax.add(availMovesArr.get(i));
						else {
							if(current == bestChoiceSoFar) savedGridsToCheckForIterativeMinimax.add(availMovesArr.get(i));
						}
						if(current == bestChoiceSoFar) bestChoices.add(availMovesArr.get(i));
					}
					//end if(current > bestChoiceSoFar) / else...
				}
				//end if(level == 0)
			}
			//end if(currSeed == Seed.CROSS) / else

			board[rowSelected][colSelected] = Seed.EMPTY; // Undo last move (reset board) and get ready for next iteration
			if(debugOn) this.print(board);	//debug mode only
		}
		//end for

		/* After going thru the above big for loop, if we're at the root level (level == 0), 
		 * then it's time to actually assign AI's next move. We should NOT
		 * call this method at any of the recursively invoked levels. */
		if(level == 0) {
			if(debug2On) {
				System.out.print("bestChoices: ");
				printAL(bestChoices);
			}
			int index = this.randomize(bestChoices.size());	//if we have more than one best choice, then AI will pick a random one (custom method)
			this.nextMove[0] = bestChoices.get(index)[0];	//assign what AI's next move will be
			this.nextMove[1] = bestChoices.get(index)[1];
			if(debug2On) System.out.println("this.nextMove (AI's best move found by minimax): " + Arrays.toString(this.nextMove));
		}

		/* Finally, it's time to return min or max depending on whose turn it is */
		if(currSeed == Seed.CROSS) {	//if this is human's turn..
			if(debugOn) System.out.println("It's human's turn now, returning " + min);
			return min;
		}
		else {	//else, if this is AI's turn..
			if(debugOn) System.out.println("It's AI's turn now, returning " + max);
			return max;
		}
	}
	//end public double minimax

	/**
	 * Method: printBoard
	 * Prints the board to the console. Used for debugging purposes
	 * @param board the 2-dimensional array representing the game board.
	 */
	public void print(Seed[][] board) {
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				System.out.printf("|%1s", this.board[i][j] == Seed.CROSS ? "X": this.board[i][j] == Seed.NOUGHT ? "O" : " ");
			}
			System.out.println("|");
		}
	}
	//end public void print

	/**
	 * Method: printAL
	 * Prints the given arraylist to the console. Each element is an int[] array with exactly 2 elements. Used for debugging purposes.
	 * @param al given arraylist consisting of of int[2] elements.
	 */
	public static void printAL(ArrayList<int[]> al) {
		for(int[] a : al) {
			System.out.print("[" + a[0] + "," + a[1] + "] ");
		}
		System.out.println();
	}

	/**
	 * Method: main
	 * @param args
	 */
	public static void main(String[] args) {
		// Run GUI codes in the Event-Dispatching thread for thread safety
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Game(); // Let the constructor do the job
			}
		});
	}
}
