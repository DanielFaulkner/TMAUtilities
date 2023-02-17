// Processing steps:
// - Ask user for the folder of files.
// - Scan through the file names and parse the positions
// - Ask user for TMA dimensions
// - Determine the maximum/minimum boundaries
// -- Optional locate the corners and use geometry to readjust the grid
// - Calculate the 'expected positions' for each TMA.
// - For each expected position identify the closest available TMA point and store
// - Save the output as a .tsv sheet
// NOTE: Smallest top left, highest values bottom right

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.lang.Math.*;	// Needed for sin and cos functions for rotation


public class TMAsheet {
	File srcFolder;
	File saveFolder;
	JSpinner heightSpinner;
	JSpinner widthSpinner;
	JCheckBox sameSizeCheck;
	JCheckBox autoRotateCheck;
	JLabel statusLabel;
	JRadioButton exactRadio;
	JRadioButton hungarianRadio;
	HashMap<String,PointMatrix> TMAs;

	// Functions for a very minimalistic user interface (as windows doesn't show the console by default)
	public static File TMAsource() {
		File srcFolder = new File("PlaceHolder");
		// Prompt user for the location of the TMA images
		JFileChooser SrcPrompt = new JFileChooser();
		SrcPrompt.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		Integer SrcPromptVal = SrcPrompt.showOpenDialog(null);
		if (SrcPromptVal == JFileChooser.APPROVE_OPTION) {
	        srcFolder = SrcPrompt.getSelectedFile();
	        System.out.println("Setting source folder to: "+srcFolder);
	    } else {
	        System.out.println("Action cancelled by user");
	        //System.exit(0);
		}
		return srcFolder;
	}

	public static HashMap<String,PointMatrix> TMAsize(HashMap<String,PointMatrix> TMAs) {
		// Prompt the user for the TMA grid dimensions
		for (String i : TMAs.keySet()) {
			int Height = Integer.parseInt(JOptionPane.showInputDialog("TMA '"+i+"' height:",10));
			int Width = Integer.parseInt(JOptionPane.showInputDialog("TMA '"+i+"' width:",10));
			TMAs.get(i).setMatrix(Height,Width);
		}
		return TMAs;
	}

	public static File TMAsave() {
		File outFolder = new File("PlaceHolder");
		// Prompt the user for the location to save the files to
		JFileChooser OutPrompt = new JFileChooser();
		OutPrompt.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		Integer OutPromptVal = OutPrompt.showSaveDialog(null);
		if (OutPromptVal == JFileChooser.APPROVE_OPTION) {
	        outFolder = OutPrompt.getSelectedFile();
	        System.out.println("Setting output folder to: "+outFolder);
	    } else {
	        System.out.println("Action cancelled by user");
	        //System.exit(0);
		}
		return outFolder;
	}

	// Function(s) to perform the actual processing of the data
	public static HashMap<String,PointMatrix> parseFolder(File srcFolder) {
		// Scan the source folder and compile a datastructure to represent the locations and positions.
		String[] fileNames;
		int Xcoord;
		int Ycoord;
		String TMAname;
		HashMap<String,PointMatrix> TMAlist = new HashMap<String, PointMatrix>();
		Pattern pattern = Pattern.compile("\\[\\d*,\\d*\\]", Pattern.CASE_INSENSITIVE);
		// - Get all the file names with the following pattern: string[int,int]string
		fileNames = srcFolder.list();
		for (String FileName : fileNames) {
			Matcher matcher = pattern.matcher(FileName);
			boolean matchFound = matcher.find();
			//System.out.println(FileName+" valid TMA file? "+matchFound); // Uncomment to test the regular expression matching.
			if (matchFound){
				// Parse the filename if valid
				String[] TMAnameParts = FileName.split("\\[|\\]");
				TMAname = TMAnameParts[0];
				String[] TMAcoord = TMAnameParts[1].split(",");
				Xcoord= Integer.parseInt(TMAcoord[0]);
				Ycoord= Integer.parseInt(TMAcoord[1]);
				//System.out.println("TMA: "+TMAname+" X: "+Xcoord+" Y: "+Ycoord); // Uncomment to view the information parsed from the files
				// Store the values
				if (!TMAlist.containsKey(TMAname)){
					// Add TMA entry to HashMap
					PointMatrix newMatrix = new PointMatrix(TMAname);
					TMAlist.put(TMAname,newMatrix);
				}
				// Add values to the HashMap
				PointMatrix CurrentTMA = TMAlist.get(TMAname);
				CurrentTMA.addPoint(Xcoord,Ycoord);
			}
		}
		// Return the objects
		return TMAlist;
	}

	public static void TMAwrite(String savePath, HashMap<String,PointMatrix> TMAs) {
		// Write out the TMAs as tsv files:
		for (String TMAname : TMAs.keySet()){
			String fileName = savePath+File.separator+TMAname+".tsv";
			// TODO: Check for existing file and prompt to continue
			try {
				FileWriter outputWriter = new FileWriter(fileName);
				//outputWriter.write("Test");
				int[][][] outputMatrix = TMAs.get(TMAname).getActual();
				int TMAHeight = TMAs.get(TMAname).getHeight();
				int TMAWidth = TMAs.get(TMAname).getWidth();
				for (int j=0; j<TMAHeight;j++){
					for (int i=0; i<TMAWidth;i++){
						outputWriter.write(outputMatrix[i][j][0]+","+outputMatrix[i][j][1]+"\t");
					}
					outputWriter.write("\n");
				}			
				outputWriter.close();
				System.out.println("Wrote: "+fileName);
			} catch (IOException e) {
				System.out.println("Unexpected error");
				e.printStackTrace();
			}
		}		
	}

    public void closeFunction(){
        System.exit(0);
    }

    public TMAsheet() {
        /*
		 * GUI layout:
		 * Select source
		 * Set options (Analysis type) (Seperate Panel)
		 * Create sheet (Also display progress) (Seperate Panel)
		 * Save sheet
		 */
		JFrame myFrame=new JFrame("TMA Spreadsheet Creator");
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myFrame.setSize(400,400);
		myFrame.setMinimumSize(new Dimension(400,300));

		JPanel mainPanel = new JPanel();
        //mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));   // Add to the bottom of a list (but does not fill grid)
        mainPanel.setLayout(new GridLayout(0,1));   // Equal sized elements in grid

        // Input:
        JButton srcButton=new JButton("Select folder with TMA files");
		mainPanel.add(srcButton);//adding button in JFrame

        // Options:
        JLabel optionsLabel=new JLabel("Options: ");
        JPanel sizePanel = new JPanel();
        JLabel sizeLabel=new JLabel("TMA Size (height,width): ");
        heightSpinner = new JSpinner(new SpinnerNumberModel(10,0,1000,1));  // Set TMA size
        widthSpinner = new JSpinner(new SpinnerNumberModel(10,0,1000,1));   // Set TMA size
        sizePanel.add(sizeLabel);
        sizePanel.add(heightSpinner);
        sizePanel.add(widthSpinner);
		sameSizeCheck = new JCheckBox("Use the same size for all TMAs", true);
		autoRotateCheck = new JCheckBox("Apply rotation correction to TMA", true);
        JPanel algorithmPanel = new JPanel();
        JLabel algorithmLabel=new JLabel("Matching algorithm: ");
        hungarianRadio=new JRadioButton("Munkres matching",true);   // Set matching algorithm        
        exactRadio=new JRadioButton("Exact matching");         // Set matching algorithm        
        ButtonGroup matchGroup=new ButtonGroup();
        algorithmPanel.add(hungarianRadio);
        algorithmPanel.add(exactRadio);
        matchGroup.add(hungarianRadio);
        matchGroup.add(exactRadio);
        mainPanel.add(optionsLabel);
        mainPanel.add(sizePanel);
		mainPanel.add(sameSizeCheck);
		mainPanel.add(autoRotateCheck);
        mainPanel.add(algorithmLabel);
        mainPanel.add(algorithmPanel);

        // Run:
        JButton runButton=new JButton("Convert files to TMA sheet");
        mainPanel.add(runButton);
        JPanel runPanel = new JPanel();
        JLabel progressLabel=new JLabel("Progess: ");
        statusLabel=new JLabel("Not started");
        runPanel.add(progressLabel);
        runPanel.add(statusLabel);
        mainPanel.add(runPanel);
		runButton.setEnabled(false);

        // Output:
        JButton saveButton=new JButton("Select folder to save results");
		saveButton.setEnabled(false);
        mainPanel.add(saveButton);//adding button in JFrame

        // Construct the main frame layout
        JButton closeButton=new JButton("Close");
        mainPanel.add(closeButton);//adding button in JFrame

        myFrame.add(mainPanel);
		myFrame.setVisible(true);

        // Add listeners to the various buttons:
        closeButton.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){closeFunction();}});
		srcButton.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){srcFolder = TMAsource();runButton.setEnabled(true);statusLabel.setText("Input set to "+srcFolder);}});
		runButton.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){statusLabel.setText("Processing...");calcFunction(); saveButton.setEnabled(true);}});
		saveButton.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){saveFolder = TMAsave();TMAwrite(saveFolder.getPath(),TMAs);statusLabel.setText("Output saved to "+saveFolder);}});
		sameSizeCheck.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(sameSizeCheck.isSelected()) {heightSpinner.setEnabled(true);widthSpinner.setEnabled(true);} else {heightSpinner.setEnabled(false);widthSpinner.setEnabled(false);}}});
		// Set tool tips:
		sameSizeCheck.setToolTipText("If unchecked you will be prompted for this information per TMA.");
		hungarianRadio.setToolTipText("Best possible overall match.");
		exactRadio.setToolTipText("Best match possible per core (error generated if multiple cores match to the same cell).");
    }

	public void calcFunction(){
		// - Return the TMA locations given in the folder
		TMAs = parseFolder(srcFolder);

		// Configure the matrix size
		for (String i : TMAs.keySet()) {
			if (!sameSizeCheck.isSelected()){
				int Height = Integer.parseInt(JOptionPane.showInputDialog("TMA '"+i+"' height:",10));
				int Width = Integer.parseInt(JOptionPane.showInputDialog("TMA '"+i+"' width:",10));
				TMAs.get(i).setMatrix(Height,Width);
			}
			else {
				TMAs.get(i).setMatrix((Integer)heightSpinner.getValue(),(Integer)widthSpinner.getValue());
			}
		}

		//for (String i : TMAs.keySet()){	// Uncomment to test HashMap object creation
		//	System.out.println(i);
		//	TMAs.get(i).printInput();
		//}

		// For each TMA calculate the positions in a matrix
		int counter=1;
		String failedTMAs = "";
		for (String i : TMAs.keySet()){
			statusLabel.setText("Processing TMA "+counter+" of "+TMAs.size()+" (may take a minute)");
			if (autoRotateCheck.isSelected()){TMAs.get(i).expectedMatrix(true);}
			else {TMAs.get(i).expectedMatrix(false);}
			//TMAs.get(i).printExpected();
			if (exactRadio.isSelected()){
				int failFlag = TMAs.get(i).TMApositionsExact();			// Exact matches (overwriting possible)
				if (failFlag>0){
					System.out.println("WARNING: Perfect match not possible for "+i+". Output contains ("+failFlag+") errors.");
					failedTMAs = i+" ";
				}
			}
			else {
				TMAs.get(i).TMApositionsHungarian();
			}
			//TMAs.get(i).printActual();
			counter++;
		}
		if (failedTMAs!=""){
			statusLabel.setText("WARNING - Alignment issues with "+failedTMAs);
		}
		else {
			statusLabel.setText("Processed all TMAs");
		}
	}

	// Main function
	public static void main(String[] args) {
		// Main function

		// +++++++++ Rewrite to include a more complete UI
		new TMAsheet();
		
	}
}

// Class to store the point cloud and related methods
class PointMatrix{
	// Edit default matrix size for object here:
	String TMAname = "Default";
	int TMAWidth = 10;
	int TMAHeight = 10;
	ArrayList<int[]> TMAinput = new ArrayList<int[]>();
	int[][][] TMAexpected = new int[TMAWidth][TMAHeight][2];
	int[][][] TMAactual = new int[TMAWidth][TMAHeight][2];

	// Construct the object and set/update the input values
	public PointMatrix() {
	}
	public PointMatrix(String name) {
		TMAname = name;
	}
	// Add data to the object
	public void setMatrix(int Width, int Height) {
		TMAWidth=Width;
		TMAHeight=Height;		
	}
	public void addPoint(int X, int Y) {
		TMAinput.add(new int[] {X, Y});
	}
	// Ouput data from the object
	public void printInput() {
		System.out.println("TMA name: "+TMAname);	// Values input into the object		
		System.out.println("TMA size: "+TMAWidth+" by "+TMAHeight);
		System.out.println("Input points:");
		for (int[] point : TMAinput){
			System.out.println(Arrays.toString(point));
		}
	}
	public void printExpected() {
		System.out.println("Expected TMA layout: ");	// Calculated 'expected' positions
		for (int j=0; j<TMAHeight;j++){
			for (int i=0; i<TMAWidth;i++){
				System.out.print(" "+i+","+j+":"+TMAexpected[i][j][0]+","+TMAexpected[i][j][1]);
			}
			System.out.print("\n");
		}
	}
	public void printActual() {
		System.out.println("Actual TMA layout: ");	// Matched actual positions
		for (int j=0; j<TMAHeight;j++){
			for (int i=0; i<TMAWidth;i++){
				System.out.print(" "+i+","+j+":"+TMAactual[i][j][0]+","+TMAactual[i][j][1]);
			}
			System.out.print("\n");
		}
	}
	public int[][][] getActual() {
		return TMAactual;
	}
	public int getHeight() {
		return TMAHeight;		
	}
	public int getWidth() {
		return TMAWidth;
	}
	// Calculations on the object
	public void expectedMatrix(boolean autoRotate) {
		// TODO: Apply some form of rotation correction and geometric correction based on the points given.
		// - Calculate RMSE
		// - Apply rotation (both directions)
		// - Check RMSE - if improved apply rotation.
		// From the max and min X and Y values calculate the expected position of each TMA core
		// Find the highest and lowest values:
		ArrayList<Integer> xValues = new ArrayList<Integer>();
		ArrayList<Integer> yValues = new ArrayList<Integer>();
		for (int[] points : TMAinput) {
			xValues.add(points[0]);
			yValues.add(points[1]);
		}
		int minX = Collections.min(xValues);
		int maxX = Collections.max(xValues);
		int minY = Collections.min(yValues);
		int maxY = Collections.max(yValues);
		// Create a representation of the TMA and plot expected values:
		//ArrayList<int[]> row = new ArrayList<int[]>();
		//ArrayList<int[]> matrix = new ArrayList<int[]>();
		int[][][] matrix = new int[TMAWidth][TMAHeight][2];
		int curX;
		int curY;
		for (int i=0; i<TMAWidth;i++){
			// Calculate the X value
			curX = minX+(i*((maxX-minX)/(TMAWidth-1)));
			for (int j=0; j<TMAHeight;j++){
				// Calculate the Y value
				curY = minY+(j*((maxY-minY)/(TMAHeight-1)));
				// Add X and Y to matrix
				//System.out.println(i+","+j+"="+curX+","+curY); // Uncomment to test X/Y calculations
				matrix[i][j][0] = curX;
				matrix[i][j][1] = curY;
			}
		}
		if (autoRotate){
			int[][][] matrix2 = autoRotate(matrix);
			TMAexpected = matrix2;
		}
	}
	// Adjust matrix to correct for minor rotations:
	private int[][][] autoRotate(int[][][] estimateMatrix){
		// Automatically rotate the matrix to get the best fit.
		int[][][] rotatedMatrix = estimateMatrix;
		ArrayList<int[]> rotatedActual = TMAinput;
		int currentSize;
		int newSize;
		float degrees = 0;	// Correction amount required (can also be used to indicate current direction of rotation)
		float change = 0.1f;		// Change to make to the current rotation
		do {
			// Calculate the actual TMA dimensions
			ArrayList<Integer> xValues = new ArrayList<Integer>();
			ArrayList<Integer> yValues = new ArrayList<Integer>();
			for (int[] points : rotatedActual) {
				xValues.add(points[0]);
				yValues.add(points[1]);
			}
			int minX = Collections.min(xValues);
			int maxX = Collections.max(xValues);
			int minY = Collections.min(yValues);
			int maxY = Collections.max(yValues);
			int sizeX = maxX-minX;	// Size needed for checking suitability of fit later.
			int sizeY = maxY-minY;
			currentSize = (sizeX+sizeY);
			System.out.println("Current "+sizeX+" "+sizeY);
			// Rotate clockwise (unless already started a rotation anticlockwise)
			newSize=Integer.MAX_VALUE;
			if (degrees>=0){	// Adjust clockwise
				// Rotate clockwise
				ArrayList<int[]> rotatedActualNew = rotate(rotatedActual,change);
				// Calculate new size
				int[] gridSize = arraySize(rotatedActualNew);
				System.out.println("CW "+gridSize[0]+" "+gridSize[1]);
				newSize=gridSize[2];
				if (newSize<currentSize){
					degrees=degrees+change;
					rotatedActual = rotatedActualNew;
				}
			}
			// Rotate anticlockwise (unless already started a rotation clockwise)
			if (degrees<=0){
				// Rotate clockwise
				ArrayList<int[]> rotatedActualNew  = rotate(rotatedActual,-change);
				// Calculate new size
				int[] gridSize = arraySize(rotatedActualNew);
				System.out.println("AC "+gridSize[0]+" "+gridSize[1]);
				newSize=gridSize[2];
				if (newSize<currentSize){
					degrees=degrees-change;
					rotatedActual = rotatedActualNew;
				}
			}
			System.out.println("- "+currentSize+" "+newSize);
		} while(currentSize>newSize);	// Do while the new size is smaller than the previous size
		System.out.println("TMA is rotated by: "+(-degrees)+" degrees.");
		System.out.println("Applying correction to estimated positions");
		// Calculate the starting estimate grid dimensions
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (int i=0; i<TMAWidth;i++){
			for (int j=0; j<TMAHeight;j++){
				// Get the smallest X and Y values
				if (estimateMatrix[i][j][0]<minX){minX=estimateMatrix[i][j][0];}
				if (estimateMatrix[i][j][1]<minY){minY=estimateMatrix[i][j][1];}
				if (estimateMatrix[i][j][0]>maxX){maxX=estimateMatrix[i][j][0];}
				if (estimateMatrix[i][j][1]>maxY){maxY=estimateMatrix[i][j][1];}
			}
		}
		int sizeX = maxX-minX;
		int sizeY = maxY-minY;
		int originX = minX+(sizeX/2);
		int originY = minY+(sizeY/2);
		System.out.println("Estimate grid starting dimensions: "+sizeX+":"+sizeY+" with centre at "+originX+":"+originY);
		// Make the correction
		// Rotate:
		double angle = Math.toRadians(-degrees);
		for (int i=0; i<TMAWidth;i++){
			for (int j=0; j<TMAHeight;j++){
				double newX = estimateMatrix[i][j][0]-originX;							// Move point to the origin
				double newY = estimateMatrix[i][j][1]-originY;
				double newXb = (newX*Math.cos(angle))-(newY*Math.sin(angle));		// Rotate point 
				double newYb = (newX)*Math.sin(angle)+(newY)*Math.cos(angle);
				newX = newXb+originX;									// Add the origin to the points
				newY = newYb+originY;
				rotatedMatrix[i][j][0]=(int)Math.round(newX);			
				rotatedMatrix[i][j][1]=(int)Math.round(newY);
				System.out.print((int)newX+":"+(int)newY+" ");
			}
			System.out.print("\n");
		}
		// Recheck the estimate size
		// TODO
		// Return
		return rotatedMatrix;
	}
	private ArrayList<int[]> rotate(ArrayList<int[]> points, float degrees){
		ArrayList<int[]> rotatedArray = new ArrayList<int[]>();
		// Calculate the dimensions
		ArrayList<Integer> xValues = new ArrayList<Integer>();
		ArrayList<Integer> yValues = new ArrayList<Integer>();
		for (int[] point : points) {
			xValues.add(point[0]);
			yValues.add(point[1]);
		}
		int minX = Collections.min(xValues);
		int maxX = Collections.max(xValues);
		int minY = Collections.min(yValues);
		int maxY = Collections.max(yValues);
		int sizeX = maxX-minX;
		int sizeY = maxY-minY;
		// - Calculate the point to rotate around:
		double originX = (sizeX/2)+minX;
		double originY = (sizeY/2)+minY;
		System.out.println("Origin: "+originX+","+originY);
		// Rotate:
		//double angle = degrees*(Math.PI/180);
		double angle = Math.toRadians(degrees);
		//double angle = degrees;
		for (int[] point : points){
			double newX = point[0]-originX;							// Move point to the origin
			double newY = point[1]-originY;
			double newXb = (newX*Math.cos(angle))-(newY*Math.sin(angle));		// Rotate point 
			double newYb = (newX)*Math.sin(angle)+(newY)*Math.cos(angle);
			newX = newXb+originX;									// Add the origin to the points
			newY = newYb+originY;
			rotatedArray.add(new int[] {(int)Math.round(newX), (int)Math.round(newY)});	// Without Math.round typecast will always round down (causing small changes to Origin)
		}
		return rotatedArray;
	}
	private int[] arraySize(ArrayList<int[]> points){
		// Calculate the actual TMA dimensions
		int[] size = new int[3];
		ArrayList<Integer> xValues = new ArrayList<Integer>();
		ArrayList<Integer> yValues = new ArrayList<Integer>();
		for (int[] point : points) {
			xValues.add(point[0]);
			yValues.add(point[1]);
		}
		int minX = Collections.min(xValues);
		int maxX = Collections.max(xValues);
		int minY = Collections.min(yValues);
		int maxY = Collections.max(yValues);
		int sizeX = maxX-minX;	// Size needed for checking suitability of fit later.
		int sizeY = maxY-minY;
		size[0] = sizeX;
		size[1] = sizeY;
		size[2] = sizeX+sizeY;
		return size;
	}
	// Some TMAs are distorted such that multiple cores share the same closest expected point.
	// The hungarian algorithm is one solution which (while more complicated) may if implemented come up with an optimal solution.
	public int TMApositionsExact() {
		// Calculate distance between 2 points:
		int[][][] matrix = new int[TMAWidth][TMAHeight][2];
		int failFlag = 0;
		for (int[] curPoint :TMAinput){
			// Find closest point
			int ClosestX = Integer.MAX_VALUE;
			int ClosestY = Integer.MAX_VALUE;
			double distanceCur = Integer.MAX_VALUE;	// Start with furthest possible distance
			double distanceNew;		// We are using large enough numbers that rounding from double to int should be fine
			for (int i=0; i<TMAHeight;i++){
				for (int j=0; j<TMAWidth;j++){
					distanceNew = Math.hypot(curPoint[0]-TMAexpected[i][j][0],curPoint[1]-TMAexpected[i][j][1]); // distance = Math.hypot(x1-x2, y1-y2)
					//System.out.println("Testing: "+curPoint[0]+":"+curPoint[1]+" is "+distanceNew+" from "+TMAexpected[i][j][0]+":"+TMAexpected[i][j][1]+" Postion = "+i+":"+j);
					if (distanceNew<distanceCur){
						distanceCur = distanceNew;
						ClosestX = i;
						ClosestY = j;
					}					
				}
			}
			// Store the result
			if (matrix[ClosestX][ClosestY][0]!=0 || matrix[ClosestX][ClosestY][1]!=0){
				failFlag++;
			}
			matrix[ClosestX][ClosestY][0]=curPoint[0];	
			matrix[ClosestX][ClosestY][1]=curPoint[1];

		}
		//if (failFlag>0){
		//	System.out.println("WARNING: Perfect match not possible. Output contains errors. ("+failFlag+")");
		//}
		// Return the updated matrix
		TMAactual = matrix;
		return failFlag;
	}
	// Alternative to the above function which uses the hungarian algorithm to make the assignments
	public void TMApositionsHungarian() {
		// Calculate distance between 2 points:
		HungarianMatch pointCloud = new HungarianMatch();
		int[] matches;			// Returned value in the format of [a]=b (where a/b is the order of the input, i/j)
		int[][] actualPoints;		// Arrays in the format of [X][Y] ie. {{x1,x2,x3},{y1,y2,y3}}
		int[][] estimatedPoints;
		
		// Reformat ArrayList<int[]> TMAinput to actualPoints
		actualPoints = new int[2][TMAinput.size()];
		int counter = 0;
		for (int[] point : TMAinput){
			actualPoints[0][counter]=point[0];	// Add X
			actualPoints[1][counter]=point[1];	// Add Y
			counter++;
		}
		
		// Reformat int[][][] TMAexpected to estimatedPoints - TMAexpected[i][j][0/1] (X/Y)
		estimatedPoints = new int[2][TMAWidth*TMAHeight];
		counter = 0;
		for (int i=0;i<TMAHeight;i++){
			for (int j=0;j<TMAWidth;j++){
				// Current position = ((i+1)*(j+1))-1
				estimatedPoints[0][counter]=TMAexpected[i][j][0];
				estimatedPoints[1][counter]=TMAexpected[i][j][1];
				counter++;
			}		
		}
		
		pointCloud.setDistances(estimatedPoints,actualPoints);		// Takes 2 arrays with lists of x/y co-ordinates
		pointCloud.addFakePoints();		// Algorithm works and produces the same results without this
		pointCloud.calcMatrices();						// Calculate the matrices
		matches = pointCloud.getAssignments();				// Return the assignments
		
		// Convert the assignments back into a matrix:
		// - Dimension 1 order is the same as the input array list (Can use newX[matches[i]] = TMAinput.indexOf(i))
		// - Dimension 2 order is row wise top left to bottom right (Can use newY[matches[i]] = TMAinput.indexOf(i))
		int[][][] matrix = new int[TMAWidth][TMAHeight][2];
		counter = 0;
		for (int i=0;i<TMAHeight;i++){
			for (int j=0;j<TMAWidth;j++){
				if (matches[counter]==Integer.MAX_VALUE) {
					// No assignment made - Due to matrix size difference
					//System.out.println("No assignment");
				}
				else if (matches[counter]>=actualPoints[0].length) {
					// Matched to dummy worker which doesn't exist
					//System.out.println("Dummy line ignored");
				}
				else {		// If the values are the maximum then no match was found - Ignore
					matrix[i][j][0]=actualPoints[0][matches[counter]];	// Look up the values in the actualPoints array
					matrix[i][j][1]=actualPoints[1][matches[counter]];
				}
				counter++;
			}		
		}		
		
		// Return the updated matrix
		TMAactual = matrix;
	}
}
