package similarity;

public class CombinationSelect {
    public float max = -1;
    int shorter;
    float[][] matrix;

    public CombinationSelect(float[][] matrix, int shorter, int longer) {
        this.shorter = shorter;
        this.matrix = matrix;

        int[] index = new int[longer];
        for (int i = 0; i < index.length; i++) {
            index[i] = i;
        }
        combinationSelect(index, shorter);
    }

    public void combinationSelect(int[] dataList, int n) {
        combinationSelect(dataList, 0, new int[n], 0);
    }

    public void combinationSelect(int[] dataList, int dataIndex,
                                  int[] resultList, int resultIndex) {
        int resultLen = resultList.length;
        int resultCount = resultIndex + 1;
        if (resultCount > resultLen) { // print
//            return resultList;
            float res = 0;
            for (int i = 0; i < this.shorter; i++) {
                res += matrix[i][resultList[i]];
            }
            if (max < res)
                max = res;
            return;
        }
        // select next
        for (int i = dataIndex; i < dataList.length + resultCount - resultLen; i++) {
            resultList[resultIndex] = dataList[i];
            combinationSelect(dataList, i + 1, resultList, resultIndex + 1);
        }
    }

    /**
     * compute C(n, m)
     *
     * @return return C(n, m)
     */
    public static int combination(int m, int n) {
        return m < n ? factorial(n) / (factorial(n - m) * factorial(m)) : 0;
    }

    /**
     * @return return n!
     */
    private static int factorial(int n) {
        int sum = 1;
        while (n > 0) {
            sum = sum * n--;
        }
        return sum;
    }

}
