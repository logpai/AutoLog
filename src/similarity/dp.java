package similarity;


public class dp {

    public static void main(String[] args) {

        float[][] test2 = {{1, 0, 0.8f, 0.7f},
                {0, 0.6f, 0.5f, 0.3f},
                {0.8f, 0.9f, 0.6f, 0.9f}};
        float res = dp_invoke(test2);
        CombinationSelect com = new CombinationSelect(test2, 3, 4);
        System.out.println(res);
        System.out.println(com.max);
    }

    static float dp_invoke(float[][] arr) {
        int shorter = arr.length;
        int longer = arr[0].length;
        float[][] res = new float[shorter + 1][longer + 1];

//        Arrays.fill(res[0],0f);

        for (int i = 1; i < shorter + 1; i++) {
            for (int j = i; j <= longer - shorter + i && j < longer + 1; j++) {
                res[i][j] = Math.max(
                        Math.max(res[i - 1][j - 1] + arr[i - 1][j - 1], res[i - 1][j]),
                        res[i][j - 1]);
            }
        }
        return res[shorter][longer];
    }
}
