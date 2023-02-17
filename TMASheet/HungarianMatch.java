import java.util.Arrays;
import java.util.ArrayList;

/*
 * Class to calculate an optimal alignment between expected and actual TMA grid locations
 * Based on the Hungarian aka Munkres algorithm.
 * Souces used:
 * https://www.vrcbuzz.com/hungarian-method-unbalanced-assignment-problem-examples/
 * https://brc2.com/the-algorithm-workshop/
 * https://en.wikipedia.org/wiki/Hungarian_algorithm
 * 
 * How to use:
 * Create object and pass into the class either a pre-calculated distance (aka cost) matrix:
 * - setMatrix(int[][] matrix)
 * Pass into the object two coordinate arrays and have the class calculate the distance:
 * - setDistances(int[][] matrix1, int[][] matrix2)
 * Add dummy entries if a matrix is missing entries so each entry can be matched up:
 * - addFakePoints()
 * Calculate the alignment:
 * - calcMatrices()
 * Alignment results can be read from the matchedArray within the class object.
 */

public class HungarianMatch {
    	// Data structures needed:
	int[][] distanceMatrix;	// Distance matrix (preserved incase needed for assignments in step 5)
	int[][] intersectionMatrix;	// Intersection matrix
	int[][] scoringMatrix;		// Scoring matrix
	int rowLength;			// Matrix rows
	int colLength;			// Matrix columns
	int[] matchedArray;		// Data structure with all the matched assignments (a to b)
	int[] colCovered;		// Arrays to keep track of which rows/columns are 'covered' by current assignment
	int[] rowCovered;
	int[][] zeroMatrix;		// Keep track of starred and primed zeros (starred = pre-assigned = 1, primed = alternate = 1)

	// Construct the object and set/update the input values
	public HungarianMatch() {
	}
	// Inputs and outputs:
	public int[] getAssignments() {
		// Return the results
		return matchedArray;
	}
	public void setMatrix(int[][] matrix) {
		// Set a matrix to use
		this.distanceMatrix = matrix;
	}
	public void setDistances(int[][] matrix1,int[][] matrix2) {
		// Provide two sets of locations and use this to call the algorithm (NOTE: possibly better done as an extension to the class)
		// NOTE: Input is as a 2D array with X or Y as the first dimension and point number as the second
		double distance;

		// Calculate distance between 2 points:
		distanceMatrix = new int[matrix1[0].length][matrix2[0].length];	// Matrix with cell for for each point pairing
		//System.out.println("Distance matrix size: "+matrix1[0].length+","+matrix2[0].length);
		for (int i=0; i<distanceMatrix.length; i++){
			for (int j=0; j<distanceMatrix[0].length; j++){	// Iterate through every item in the distance matrix
				distance = Math.hypot(matrix1[0][i]-matrix2[0][j],matrix1[1][i]-matrix2[1][j]); 	// distance = Math.hypot(x1-x2, y1-y2)
				//System.out.println("xy1="+matrix1[0][i]+","+matrix1[1][i]+" xy2="+matrix2[0][j]+","+matrix2[1][j]+" Dist: "+distance);
				distanceMatrix[i][j]=(int)distance;
			}
		}

		// Test code to print the arrays:
/*		for (int i =0;i<matrix1[0].length;i++){
			System.out.println("Matrix 1 Index"+i+"="+matrix1[0][i]+","+matrix1[1][i]);
		}
		for (int i =0;i<matrix2[0].length;i++){
			System.out.println("Matrix 2 Index"+i+"="+matrix2[0][i]+","+matrix2[1][i]);
		}		*/

	}
	public void addFakePoints() {
		// Add fake points to the matrix to counter any imbalance. Each added row/column should contain all '0'.
		// Note: In theory these points should be the only match where there is no other possible alignments. But may want to ensure real points take priority.
		// Note2: Should still work without this adjustment if smallest of row/columns used in later step
		int matrixSize = Math.max(distanceMatrix.length,distanceMatrix[0].length);
		int[][] distanceMatrixNew = new int[matrixSize][matrixSize];	// Matrix with cell for for each point pairing
		//for (int[] row : distanceMatrixNew)
		//	Arrays.fill(row, Integer.MAX_VALUE);			// Set the distance to the maximum possible	
		// Add to new matrix values from the existing matrix:
		for (int i=0; i<distanceMatrix.length; i++) {
			for (int j=0; j<distanceMatrix[0].length; j++) {
				distanceMatrixNew[i][j] = distanceMatrix[i][j];
			}
		}
		distanceMatrix = distanceMatrixNew;
	}
	// DEBUG FUNCTIONS TO VIEW ARRAYS (Temp)
	public void printArray() {
		System.out.println("Distance matrix: ");
		for (int i=0; i<distanceMatrix.length; i++){
			System.out.print(i+": ");
			for (int j=0; j<distanceMatrix[0].length; j++){	// Iterate through every item in the distance matrix
				//System.out.print(" "+i+","+j+":"+distanceMatrix[i][j]);
				System.out.print(distanceMatrix[i][j]+" ");
			}
			System.out.print("\n");
		}
		if (scoringMatrix!=null) {
			System.out.println("Score matrix: ");
			for (int i=0; i<scoringMatrix.length; i++){
				System.out.print(i+": ");
				for (int j=0; j<scoringMatrix[0].length; j++){	// Iterate through every item in the matrix
					//System.out.print(" "+i+","+j+":"+scoringMatrix[i][j]);
					System.out.print(scoringMatrix[i][j]+" ");
				}
				System.out.print("\n");
			}
		}
		if (zeroMatrix!=null) {
			System.out.println("Zero matrix: ");
			for (int i=0; i<zeroMatrix.length; i++){
				System.out.print(i+": ");
				for (int j=0; j<zeroMatrix[0].length; j++){	// Iterate through every item in the matrix
					//System.out.print(" "+i+","+j+":"+zeroMatrix[i][j]);
					System.out.print(zeroMatrix[i][j]+" ");
				}
				System.out.print("\n");
			}
		}
	}
	public void calcMatrices() {
		// Initialise values:
		scoringMatrix = copyMatrices(distanceMatrix);
		int [][] intersectMatrix = new int[scoringMatrix.length][scoringMatrix[0].length];
		zeroMatrix = new int[scoringMatrix.length][scoringMatrix[0].length];
		intersectionMatrix = intersectMatrix;
		// Process matrix:
		rowReduction();
		colReduction();		        // The version of the algorithm followed in this version often omits this step
		// Score matrix:
		starZeros();			    // Produce an initial selection of 'starred' zeros
		int optimal = 0;
		optimal = optimalMatrix();	// If there is a 'starred' zero in each column matrix is complete
		int counter = 1000;		    // Failsafe counter to prevent never ending loops occuring in case of an issue with the code.
		while (optimal==0 && counter>0) {
			// If no zeros to prime - update matrix
			int[] curZero = findUncoveredZero();		// Returned array: [Row;Col;ZeroFoundFlag]
			if (curZero[2]==0) {
				// Update matrix
				updateMatrix();
				curZero = findUncoveredZero();	// The update should have created at least one zero
			}
			// Update assignment
			zeroMatrix[curZero[0]][curZero[1]]=2;		// Prime the current zero
			updateAssignment(curZero);
			// Retest the matrix
			optimal = optimalMatrix();
			//System.out.println("Here");	// Here to locate when infinate loop happens and where
			//printArray();
			counter--;
		}
		// Starred zeros indicate the optimal assignments, only needs to be translated
		matchSets();
		//printArray();	// DEBUG LINE
	}
	private int[][] copyMatrices(int[][] arrayIn) {
		// Tried using clone and arraycopy but without success - so writing our own code to copy the matrices
		int[][] arrayOut = new int[arrayIn.length][arrayIn[0].length];
		// Add to new matrix values from the source matrix:
		for (int i=0; i<arrayIn.length; i++) {
			for (int j=0; j<arrayIn[0].length; j++) {
				arrayOut[i][j] = arrayIn[i][j];
			}
		}
		return arrayOut;
	}
	// Create an initial selection of optimal assignments
	private void starZeros() {
		// colCovered and rowCovered - global arrays tracking the algorithm
		rowCovered = new int[scoringMatrix.length];
		colCovered = new int[scoringMatrix[0].length];
		// Row wise find the first uncovered zero and star it
		for (int i=0; i<scoringMatrix.length; i++){
			for (int j=0; j<scoringMatrix[0].length; j++){
				if (scoringMatrix[i][j]==0 && colCovered[j]==0 && rowCovered[i]==0) {
					zeroMatrix[i][j]=1;	// Keep track of zero in the matrix
					colCovered[j]=1;	// Update the rows/columns which already have an assignment
					rowCovered[i]=1;
				}
			}
		}
		Arrays.fill(rowCovered,0);			// Reset the row covered array
	}
	// Test for optimal assignment - all columns contain a stared zero
	private int optimalMatrix() {
		// Note that the covered columns going into this function is not the same as checking each column has a starred zero
		int[] colStarred = new int[colCovered.length];
		// Populate the starred column array
		for (int i=0; i<zeroMatrix.length; i++){
			for (int j=0; j<zeroMatrix[0].length; j++){
				if (zeroMatrix[i][j]==1) {
					colStarred[j]=1;
				}
			}
		}
		// Check if every colunm has been starred
		int finished = 1;
		for (int starred : colStarred) {
			if (starred==0) {
				finished=0;
			}
		}
		return finished;
	}
	// Updating matrix and trying different assignments to find the optimum solution
	// Steps to find the optimum assignment (if it exists)
	private void updateAssignment(int[] curZero) {
		ArrayList<int[]> zeroPath;
		// If no starred zero in the row - update the starred and primed zeros and return
		int[] starredZero = findStarredZeroRow(curZero[0]);	// Returns [row; col; success=1]
		if (starredZero[2]==0) {
			zeroPath = new ArrayList<int[]>();
			zeroPath.add(curZero);	// Start the path
			int[] starredZeroColumn = findStarredZeroCol(curZero[1]);
			while(starredZeroColumn[2]==1) {
				zeroPath.add(starredZeroColumn);		// Add to chain the starred zero
				int[] primedZero = findPrimedZeroRow(starredZeroColumn[0]);
				zeroPath.add(primedZero);			// Add to chain the primed zero in the row
				starredZeroColumn = findStarredZeroCol(primedZero[1]);
			}
			// No starred zero within the column. Path cannot be extended further.
			// Unstar all starred zeros, star all primed zeros WITHIN PATH
			for (int[] zero : zeroPath) {
				if (zeroMatrix[zero[0]][zero[1]]==1) {
					zeroMatrix[zero[0]][zero[1]]=0;
				}
				else if (zeroMatrix[zero[0]][zero[1]]==2) {
					zeroMatrix[zero[0]][zero[1]]=1;
				}
			}
			// Remove all primed zeros
			for (int i=0; i<zeroMatrix.length; i++){
				for (int j=0; j<zeroMatrix[0].length; j++){
					if (zeroMatrix[i][j]==2) {
						zeroMatrix[i][j]=0;
					}
				}
			}
			// Reset the covered/uncovered status
			Arrays.fill(rowCovered,0);
			Arrays.fill(colCovered,0);
			// I THINK COVER ALL COLS WITH STARRED ZEROS HERE +++++++++++++++
			for (int i=0; i<zeroMatrix.length; i++){
				for (int j=0; j<zeroMatrix[0].length; j++){
					if (zeroMatrix[i][j]==1 && colCovered[j]==0 && rowCovered[i]==0) {
						colCovered[j]=1;	// Cover columns with an assignment
					}
				}
			}
		}
		else {
			// Starred zero in the row - Update the covered/uncovered status
			rowCovered[starredZero[0]] = 1;	// Cover row
			colCovered[starredZero[1]] = 0;	// Uncover column
		}
	}
	// Steps to update the matrix if an optimum solution doesn't exist
	private void updateMatrix() {	
		// Changed to use the row/col covered arrays instead of the intersection matrix used previously
		// Find smallest uncovered value:
		int smallestValue = Integer.MAX_VALUE;
		for (int i=0; i<scoringMatrix.length; i++){
			for (int j=0; j<scoringMatrix[0].length; j++){
				if (scoringMatrix[i][j]<smallestValue && colCovered[j]==0 && rowCovered[i]==0) {
					smallestValue = scoringMatrix[i][j];
				}
			}
		}
		// Subtract/add the value to the score:
		for (int i=0; i<scoringMatrix.length; i++){
			for (int j=0; j<scoringMatrix[0].length; j++){
				if (colCovered[j]==0 && rowCovered[i]==0) {
					scoringMatrix[i][j] = scoringMatrix[i][j]-smallestValue;
				}
				else if (colCovered[j]==1 && rowCovered[i]==1) {
					scoringMatrix[i][j] = scoringMatrix[i][j]+smallestValue;
				}
			}
		}		
	}
	// Check if an uncovered zero exists (and if so return its location)
	private int[] findUncoveredZero() {
		int[] uncoveredZero = new int[3];
		for (int i=0; i<scoringMatrix.length; i++){
			for (int j=0; j<scoringMatrix[0].length; j++){
				if (scoringMatrix[i][j]==0 && colCovered[j]==0 && rowCovered[i]==0) {
					uncoveredZero[0]=i;
					uncoveredZero[1]=j;
					uncoveredZero[2]=1;
				}
			}
		}
		return uncoveredZero;			
	}
	// Check if a starred zero exists in a row (and if so return its location)
	private int[] findStarredZeroRow(int row) {
		int[] starredZero = new int[3];
		int i = row;
		for (int j=0; j<scoringMatrix[0].length; j++){
			if (scoringMatrix[i][j]==0 && zeroMatrix[i][j]==1) {
				starredZero[0]=i;
				starredZero[1]=j;
				starredZero[2]=1;
			}
		}
		return starredZero;			
	}
	// Check if a starred zero exists in a column (and if so return its location)
	private int[] findStarredZeroCol(int col) {
		int[] starredZero = new int[3];
		int j = col;
		for (int i=0; i<scoringMatrix.length; i++){
			if (scoringMatrix[i][j]==0 && zeroMatrix[i][j]==1) {
				starredZero[0]=i;
				starredZero[1]=j;
				starredZero[2]=1;
			}
		}
		return starredZero;			
	}
	// Check if a primed zero exists in a row (and if so return its location)
	private int[] findPrimedZeroRow(int row) {
		int[] primedZero = new int[3];
		int i = row;
		for (int j=0; j<scoringMatrix[0].length; j++){
			if (scoringMatrix[i][j]==0 && zeroMatrix[i][j]==2) {
				primedZero[0]=i;
				primedZero[1]=j;
				primedZero[2]=1;
			}
		}
		return primedZero;			
	}
		// Step 5 - Assign points and return the results
	private void matchSets() {
		// Store the optimal solution
		// - Return the matched point assignments
		matchedArray = new int[Math.max(scoringMatrix[0].length,scoringMatrix.length)];
		Arrays.fill(matchedArray, Integer.MAX_VALUE);
		// Matched array[rowNum] = colNum
		for (int i=0; i<zeroMatrix.length; i++){
			for (int j=0; j<zeroMatrix[0].length; j++){
				if (zeroMatrix[i][j]==1) {
					matchedArray[i]=j;
				}
			}
		}		
	}
	// Step 1 and 2 - calculating closest points from each set by row and column reduction:
	private void rowReduction() {
		// For each row determine the minimum value and subtract from all values in the row
		int rowMin = Integer.MAX_VALUE;
		for (int i=0; i<scoringMatrix.length; i++){
			// Find the minimum value
			rowMin = Integer.MAX_VALUE;
			for (int j=0; j<scoringMatrix[0].length; j++){
				if (rowMin>scoringMatrix[i][j]) {
					rowMin = scoringMatrix[i][j];
				}
			}
			//System.out.println(rowMin);
			// Subtract the minimum value
			for (int j=0; j<scoringMatrix[0].length; j++){
				scoringMatrix[i][j]=(scoringMatrix[i][j]-rowMin);
			}
		}
	}
	private void colReduction() {
		// For each column determine the minimum value and subtract from all values in the column
		int colMin = Integer.MAX_VALUE;
		for (int j=0; j<scoringMatrix[0].length; j++){
			// Find the minimum value
			colMin = Integer.MAX_VALUE;
			for (int i=0; i<scoringMatrix.length; i++){
				if (colMin>scoringMatrix[i][j]) {
					colMin = scoringMatrix[i][j];
				}
			}
			//System.out.println(colMin);
			// Subtract the minimum value
			for (int i=0; i<scoringMatrix.length; i++){
				scoringMatrix[i][j]=(scoringMatrix[i][j]-colMin);
			}
		}
	}	
}
