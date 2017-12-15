This is a special version of tic-tac-toe played on a 6x6 board, where the goal is to make 4 in a row.

The AI uses a modified minimax algorithm along with alpha-beta pruning and iterative deepening search method, which keeps on searching deeper and deeper levels until it hits a specified time limit (determined by the coder), at which time the current search aborts and, unless the current search has had a chance to yield a better result than the previous COMPLETED level search, the result from the previous search is used to determine AI's next move.

The minimax algorithm is also designed to optimize search speed by starting its analysis with grids that are close to the human player's last-played grid, as opposed to always starting with the grid at row 0, col 0.

Furthermore, in each subsequent step of the iterative deepening process, all moves that have been determined in the previous level search to be "inferior" are eliminated from the search tree, meaning that in each subsequent iteration, the algorithm will often have fewer nodes that it has to search, resulting in a greater time efficiency.
 
The minimax algorithm optimizes speed further still by quitting the iterative deepening search process immediately in one of the following scenarios:
1) as soon as the fastest GUARANTEED winning move is found (i.e. a move that will guarantee that AI wins in the fewest possible moves),
2) as soon as the depth level exceeds the number of empty grids left (e.g. there is no reason to keep running minimax 11 levels deep if there are only 10 empty grids left),
3) as soon as it realizes that its BEST MOVE is still a GUARANTEED losing move (it's useless to search level 9, for example, if the AI realizes on level 8 that even its best move is a losing one), OR
4) as soon as the AI realizes that there is ONLY ONE non-losing move (this is limited to when the AI is searching level 2 depth).

To make the AI more intelligent, the algorithm determines the max / min "score" not just in terms of three values (e.g. -1, 0 and 1), but rather in terms of various values that differ depending on how quickly the AI can reach a guaranteed win condition (the quicker the AI can do this, the better score for AI), and/or how long the AI can stay alive in a guaranteed losing situation (the longer the AI can stay alive in this condition, the better score for AI).

To run, execute the .java file. By default, the game is launched with a scenario in which the AI has made 2 moves and the human 1. This scenario is designed to show the ways in which the AI takes advantage of a sure-win situation regardless of how the human plays.

The user can experiment with different scenarios by removing the comments on certain test cases in the code, or can play on a blank board by commenting out all test case scenarios. Searching for the text "TODO" in the code will highlight some simple adjustments that can be made in the code to allow for this (admittedly not the most user-friendly at the moment).
