n-puzzle-2
==========

The app is available in the play store here:
https://play.google.com/store/apps/details?id=com.steveinflow.n_puzzle

The N-Puzzle, otherwise known and as the Game-of-15 (or 8, 24, etc.) 
is the sliding tiles game you may have played as a kid.

The objective is to move tiles into the blank space in order to 
remake the correct picture.

This implementation includes a real-time solver.
The solver uses A* running in background threads 
to search for the path to get each tile into its correct location in order 
(with some choreography to insert tiles into the ends of rows and columns).

