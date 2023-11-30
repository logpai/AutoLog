

/*
 * Created on Apr 25, 2005
 * Updated on May 2, 2013 (support for rectangular matrices)
 *
 * Konstantinos A. Nedas
 * Department of Spatial Information Science & Engineering
 * University of Maine, Orono, ME 04469-5711, USA
 * kostas@spatial.maine.edu
 * http://www.spatial.maine.edu/~kostas
 *
 * This Java class implements the Hungarian algorithm [a.k.a Munkres' algorithm,
 * a.k.a. Kuhn algorithm, a.k.a. Assignment problem, a.k.a. Marriage problem,
 * a.k.a. Maximum Weighted Maximum Cardinality Bipartite Matching].
 *
 * [It can be used as a method call from within any main (or other function).]
 * It takes two arguments:
 * a. A 2D array (could be rectangular or square) with all values >= 0.
 * b. A string ("min" or "max") specifying whether you want the min or max assignment.
 * [It returns an assignment matrix[min(array.length, array[0].length)][2] that contains
 * the row and col of the elements (in the original inputted array) that make up the
 * optimum assignment or the sum of the assignment weights, depending on which method
 * is used: hgAlgorithmAssignments or hgAlgorithm, respectively.]
 *
 * [This version contains only scarce comments. If you want to understand the
 * inner workings of the algorithm, get the tutorial version of the algorithm
 * from the same website you got this one (www.spatial.maine.edu/~kostas).]
 *
 * Any comments, corrections, or additions would be much appreciated.
 * Credit due to professor Bob Pilgrim for providing an online copy of the
 * pseudocode for this algorithm (http://216.249.163.93/bob.pilgrim/445/munkres.html)
 *
 * Feel free to redistribute this source code, as long as this header--with
 * the exception of sections in brackets--remains as part of the file.
 *
 * Note: Some sections in brackets have been modified as not to provide misinformation
 *       about the current functionality of this code.
 *
 *
 */
package similarity;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Math.floor;
import static java.lang.Math.round;

public class HungarianAlgorithm {

    //********************************//
    //METHODS FOR CONSOLE INPUT-OUTPUT//
    //********************************//

    public static int readInput(String prompt)    //Reads input,returns float.
    {
        Scanner in = new Scanner(System.in);
        System.out.print(prompt);
        return in.nextInt();
    }

    public static void printTime(float time)    //Formats time output.
    {
        String timeElapsed = "";
        int days = (int) floor(time) / (24 * 3600);
        int hours = (int) floor(time % (24 * 3600)) / (3600);
        int minutes = (int) floor((time % 3600) / 60);
        int seconds = (int) round(time % 60);

        if (days > 0)
            timeElapsed = days + "d:";
        if (hours > 0)
            timeElapsed = timeElapsed + hours + "h:";
        if (minutes > 0)
            timeElapsed = timeElapsed + minutes + "m:";

        timeElapsed = timeElapsed + seconds + "s";
        System.out.print("\nTotal time required: " + timeElapsed + "\n\n");
    }

    //*******************************************//
    //METHODS THAT PERFORM ARRAY-PROCESSING TASKS//
    //*******************************************//

    public static void generateRandomArray    //Generates random 2-D array.
    (float[][] array, String randomMethod) {
        Random generator = new Random();
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                if (randomMethod.equals("random")) {
                    array[i][j] = generator.nextFloat();
                }
                if (randomMethod.equals("gaussian")) {
                    array[i][j] = (float) (generator.nextGaussian() / 4);        //range length to 1.
                    if (array[i][j] > 0.5) {
                        array[i][j] = 0.5f;
                    }        //eliminate outliers.
                    if (array[i][j] < -0.5) {
                        array[i][j] = -0.5f;
                    }    //eliminate outliers.
                    array[i][j] = array[i][j] + 0.5f;                //make elements positive.
                }
            }
        }
    }

    public static float findLargest        //Finds the largest element in a 2D array.
    (float[][] array) {
        float largest = Float.NEGATIVE_INFINITY;
        for (float[] doubles : array) {
            for (float aDouble : doubles) {
                if (aDouble > largest) {
                    largest = aDouble;
                }
            }
        }

        return largest;
    }

    public static float[][] transpose        //Transposes a float[][] array.
    (float[][] array) {
        float[][] transposedArray = new float[array[0].length][array.length];
        for (int i = 0; i < transposedArray.length; i++) {
            for (int j = 0; j < transposedArray[i].length; j++) {
                transposedArray[i][j] = array[j][i];
            }
        }
        return transposedArray;
    }

    public static float[][] copyOf            //Copies all elements of an array to a new array.
    (float[][] original) {
        float[][] copy = new float[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            //Need to do it this way, otherwise it copies only memory location
            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
        }

        return copy;
    }

    public static float[][] copyToSquare    //Creates a copy of an array, made square by padding the right or bottom.
    (float[][] original, float padValue) {
        int rows = original.length;
        int cols = original[0].length;    //Assume we're given a rectangular array.
        float[][] result = null;

        if (rows == cols)    //The matrix is already square.
        {
            result = copyOf(original);
        } else if (rows > cols)    //Pad on some extra columns on the right.
        {
            result = new float[rows][rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < rows; j++) {
                    if (j >= cols)    //Use the padValue to fill the right columns.
                    {
                        result[i][j] = padValue;
                    } else {
                        result[i][j] = original[i][j];
                    }
                }
            }
        } else {    // rows < cols; Pad on some extra rows at the bottom.
            result = new float[cols][cols];
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < cols; j++) {
                    if (i >= rows)    //Use the padValue to fill the bottom rows.
                    {
                        result[i][j] = padValue;
                    } else {
                        result[i][j] = original[i][j];
                    }
                }
            }
        }

        return result;
    }

    //**********************************//
    //METHODS OF THE HUNGARIAN ALGORITHM//
    //**********************************//

    //Core of the algorithm; takes required inputs and returns the assignments
    public static int[][] hgAlgorithmAssignments(float[][] array, String sumType) {
        //This variable is used to pad a rectangular array (so it will be picked all last [cost] or first [profit])
        //and will not interfere with final assignments.  Also, it is used to flip the relationship between weights
        //when "max" defines it as a profit matrix instead of a cost matrix.  Float.MAX_VALUE is not ideal, since arithmetic
        //needs to be performed and overflow may occur.
        float maxWeightPlusOne = findLargest(array) + 1;

        float[][] cost = copyToSquare(array, maxWeightPlusOne);    //Create the cost matrix

        if (sumType.equalsIgnoreCase("max"))    //Then array is a profit array.  Must flip the values because the algorithm finds lowest.
        {
            for (int i = 0; i < cost.length; i++)        //Generate profit by subtracting from some value larger than everything.
            {
                for (int j = 0; j < cost[i].length; j++) {
                    cost[i][j] = (maxWeightPlusOne - cost[i][j]);
                }
            }
        }

        int[][] mask = new int[cost.length][cost[0].length];    //The mask array.
        int[] rowCover = new int[cost.length];                    //The row covering vector.
        int[] colCover = new int[cost[0].length];                //The column covering vector.
        int[] zero_RC = new int[2];                                //Position of last zero from Step 4.
        int[][] path = new int[cost.length * cost[0].length + 2][2];
        int step = 1;
        boolean done = false;
        while (!done)    //main execution loop
        {
            switch (step) {
                case 1:
                    step = hg_step1(step, cost);
                    break;
                case 2:
                    step = hg_step2(step, cost, mask, rowCover, colCover);
                    break;
                case 3:
                    step = hg_step3(step, mask, colCover);
                    break;
                case 4:
                    step = hg_step4(step, cost, mask, rowCover, colCover, zero_RC);
                    break;
                case 5:
                    step = hg_step5(step, mask, rowCover, colCover, zero_RC, path);
                    break;
                case 6:
                    step = hg_step6(step, cost, rowCover, colCover);
                    break;
                case 7:
                    done = true;
                    break;
            }
        }//end while

        int[][] assignments = new int[array.length][2];    //Create the returned array.
        int assignmentCount = 0;    //In a input matrix taller than it is wide, the first
        //assignments column will have to skip some numbers, so
        //the index will not always match the first column ([0])
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[i].length; j++) {
                if (i < array.length && j < array[0].length && mask[i][j] == 1) {
                    assignments[assignmentCount][0] = i;
                    assignments[assignmentCount][1] = j;
                    assignmentCount++;
                }
            }
        }

        return assignments;
    }

    //Calls hgAlgorithmAssignments and getAssignmentSum to compute the
    //minimum cost or maximum profit possible.
    public static float hgAlgorithm(float[][] array, String sumType) {
        float[][] newArray = transfer(array);
        return getAssignmentSum(newArray, hgAlgorithmAssignments(newArray, sumType));
    }

    public static float getAssignmentSum(float[][] array, int[][] assignments) {
        //Returns the min/max sum (cost/profit of the assignment) given the
        //original input matrix and an assignment array (from hgAlgorithmAssignments)
        float sum = 0;
        for (int[] assignment : assignments) {
//            System.out.print(array[assignment[0]][assignment[1]] + "\t");
            sum = sum + array[assignment[0]][assignment[1]];
        }
        return sum;
    }

    public static int hg_step1(int step, float[][] cost) {
        //What STEP 1 does:
        //For each row of the cost matrix, find the smallest element
        //and subtract it from from every other element in its row.

        float minval;

        for (int i = 0; i < cost.length; i++) {
            minval = cost[i][0];
            for (int j = 0; j < cost[i].length; j++)    //1st inner loop finds min val in row.
            {
                if (minval > cost[i][j]) {
                    minval = cost[i][j];
                }
            }
            for (int j = 0; j < cost[i].length; j++)    //2nd inner loop subtracts it.
            {
                cost[i][j] = cost[i][j] - minval;
            }
        }

        step = 2;
        return step;
    }

    public static int hg_step2(int step, float[][] cost, int[][] mask, int[] rowCover, int[] colCover) {
        //What STEP 2 does:
        //Marks uncovered zeros as starred and covers their row and column.

        for (int i = 0; i < cost.length; i++) {
            for (int j = 0; j < cost[i].length; j++) {
                if ((cost[i][j] == 0) && (colCover[j] == 0) && (rowCover[i] == 0)) {
                    mask[i][j] = 1;
                    colCover[j] = 1;
                    rowCover[i] = 1;
                }
            }
        }

        clearCovers(rowCover, colCover);    //Reset cover vectors.

        step = 3;
        return step;
    }

    public static int hg_step3(int step, int[][] mask, int[] colCover) {
        //What STEP 3 does:
        //Cover columns of starred zeros. Check if all columns are covered.

        //Cover columns of starred zeros.
        for (int[] ints : mask) {
            for (int j = 0; j < ints.length; j++) {
                if (ints[j] == 1) {
                    colCover[j] = 1;
                }
            }
        }

        int count = 0;
        //Check if all columns are covered.
        for (int i : colCover) {
            count = count + i;
        }

        if (count >= mask.length)    //Should be cost.length but ok, because mask has same dimensions.
        {
            step = 7;
        } else {
            step = 4;
        }

        return step;
    }

    public static int hg_step4(int step, float[][] cost, int[][] mask, int[] rowCover, int[] colCover, int[] zero_RC) {
        //What STEP 4 does:
        //Find an uncovered zero in cost and prime it (if none go to step 6). Check for star in same row:
        //if yes, cover the row and uncover the star's column. Repeat until no uncovered zeros are left
        //and go to step 6. If not, save location of primed zero and go to step 5.

        int[] row_col = new int[2];    //Holds row and col of uncovered zero.
        boolean done = false;
        while (!done) {
            findUncoveredZero(row_col, cost, rowCover, colCover);
            if (row_col[0] == -1) {
                done = true;
                step = 6;
            } else {
                mask[row_col[0]][row_col[1]] = 2;    //Prime the found uncovered zero.

                boolean starInRow = false;
                for (int j = 0; j < mask[row_col[0]].length; j++) {
                    if (mask[row_col[0]][j] == 1)        //If there is a star in the same row...
                    {
                        starInRow = true;
                        row_col[1] = j;        //remember its column.
                    }
                }

                if (starInRow) {
                    rowCover[row_col[0]] = 1;    //Cover the star's row.
                    colCover[row_col[1]] = 0;    //Uncover its column.
                } else {
                    zero_RC[0] = row_col[0];    //Save row of primed zero.
                    zero_RC[1] = row_col[1];    //Save column of primed zero.
                    done = true;
                    step = 5;
                }
            }
        }

        return step;
    }

    public static int[] findUncoveredZero    //Aux 1 for hg_step4.
    (int[] row_col, float[][] cost, int[] rowCover, int[] colCover) {
        row_col[0] = -1;    //Just a check value. Not a real index.
        row_col[1] = 0;

        int i = 0;
        boolean done = false;
        while (!done) {
            int j = 0;
            while (j < cost[i].length) {
                if (cost[i][j] == 0 && rowCover[i] == 0 && colCover[j] == 0) {
                    row_col[0] = i;
                    row_col[1] = j;
                    done = true;
                }
                j = j + 1;
            }//end inner while
            i = i + 1;
            if (i >= cost.length) {
                done = true;
            }
        }//end outer while

        return row_col;
    }

    public static int hg_step5(int step, int[][] mask, int[] rowCover, int[] colCover, int[] zero_RC, int[][] path) {
        //What STEP 5 does:
        //Construct series of alternating primes and stars. Start with prime from step 4.
        //Take star in the same column. Next take prime in the same row as the star. Finish
        //at a prime with no star in its column. Unstar all stars and star the primes of the
        //series. Erasy any other primes. Reset covers. Go to step 3.

        int count = 0;                                        //Counts rows of the path matrix.
        //int[][] path = new int[(mask[0].length + 2)][2];	//Path matrix (stores row and col).
        path[count][0] = zero_RC[0];                        //Row of last prime.
        path[count][1] = zero_RC[1];                        //Column of last prime.

        boolean done = false;
        while (!done) {
            int r = findStarInCol(mask, path[count][1]);
            if (r >= 0) {
                count = count + 1;
                path[count][0] = r;                    //Row of starred zero.
                path[count][1] = path[count - 1][1];    //Column of starred zero.
            } else {
                done = true;
            }

            if (!done) {
                int c = findPrimeInRow(mask, path[count][0]);
                count = count + 1;
                path[count][0] = path[count - 1][0];    //Row of primed zero.
                path[count][1] = c;                    //Col of primed zero.
            }
        }//end while

        convertPath(mask, path, count);
        clearCovers(rowCover, colCover);
        erasePrimes(mask);

        step = 3;
        return step;

    }

    public static int findStarInCol            //Aux 1 for hg_step5.
    (int[][] mask, int col) {
        int r = -1;    //Again this is a check value.
        for (int i = 0; i < mask.length; i++) {
            if (mask[i][col] == 1) {
                r = i;
            }
        }

        return r;
    }

    public static int findPrimeInRow        //Aux 2 for hg_step5.
    (int[][] mask, int row) {
        int c = -1;
        for (int j = 0; j < mask[row].length; j++) {
            if (mask[row][j] == 2) {
                c = j;
            }
        }

        return c;
    }

    public static void convertPath            //Aux 3 for hg_step5.
    (int[][] mask, int[][] path, int count) {
        for (int i = 0; i <= count; i++) {
            if (mask[path[i][0]][path[i][1]] == 1) {
                mask[path[i][0]][path[i][1]] = 0;
            } else {
                mask[path[i][0]][path[i][1]] = 1;
            }
        }
    }

    public static void erasePrimes            //Aux 4 for hg_step5.
    (int[][] mask) {
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[i].length; j++) {
                if (mask[i][j] == 2) {
                    mask[i][j] = 0;
                }
            }
        }
    }

    public static void clearCovers            //Aux 5 for hg_step5 (and not only).
    (int[] rowCover, int[] colCover) {
        Arrays.fill(rowCover, 0);
        Arrays.fill(colCover, 0);
    }

    public static int hg_step6(int step, float[][] cost, int[] rowCover, int[] colCover) {
        //What STEP 6 does:
        //Find smallest uncovered value in cost: a. Add it to every element of covered rows
        //b. Subtract it from every element of uncovered columns. Go to step 4.

        float minval = findSmallest(cost, rowCover, colCover);

        for (int i = 0; i < rowCover.length; i++) {
            for (int j = 0; j < colCover.length; j++) {
                if (rowCover[i] == 1) {
                    cost[i][j] = cost[i][j] + minval;
                }
                if (colCover[j] == 0) {
                    cost[i][j] = cost[i][j] - minval;
                }
            }
        }

        step = 4;
        return step;
    }

    public static float findSmallest        //Aux 1 for hg_step6.
    (float[][] cost, int[] rowCover, int[] colCover) {
        float minval = Float.POSITIVE_INFINITY;    //There cannot be a larger cost than this.
        for (int i = 0; i < cost.length; i++)        //Now find the smallest uncovered value.
        {
            for (int j = 0; j < cost[i].length; j++) {
                if (rowCover[i] == 0 && colCover[j] == 0 && (minval > cost[i][j])) {
                    minval = cost[i][j];
                }
            }
        }

        return minval;
    }

    public static void set(float[][] arr, int i, int j, float v) {
        arr[i][j] = v;
    }

    //***********//
    //MAIN METHOD//
    //***********//

    public static float[][] transfer(float[][] sim) { // make row < col
        int row = sim.length;
        int col = sim[0].length;

        if (row <= col)
            return sim;
        float[][] newSim = new float[col][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                newSim[j][i] = sim[i][j];
            }
        }
        return newSim;
    }

    public static void main(String[] args) {
        System.out.println("Running two tests on three arrays:\n");

        // Square
        float[][] test1 = {{10, 19, 8, 15},
                {10, 18, 7, 17},
                {13, 16, 9, 14},
                {14, 17, 10, 19}};
        // Tall
        float[][] test2 = {{10, 19, 8, 15},
                {11, 18, 7, 21},
                {13, 16, 9, 14},
                {12, 22, 8, 18},
                {14, 17, 10, 20}};
        // Wide
        float[][] test3 = {{10, 19, 8, 15, 14},
                {10, 18, 7, 17, 20},
                {13, 16, 9, 14, 11},
                {12, 19, 8, 18, 19}};

//        System.out.println(hgAlgorithm(test1, "min"));
//        System.out.println(hgAlgorithm(test1, "max"));
        System.out.println(hgAlgorithm(transfer(test2), "min"));
        System.out.println(hgAlgorithm(transfer(test2), "max"));
        System.out.println(hgAlgorithm(test3, "min"));
        System.out.println(hgAlgorithm(test3, "max"));
    }

}
