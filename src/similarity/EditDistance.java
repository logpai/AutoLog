package similarity;

public class EditDistance {

    public static int dp(String str1, String str2) {
        int d;
        int[][] m = new int[str1.length() + 1][str2.length() + 1];
        m[0][0] = 0;
        for (int i = 1; i < str1.length() + 1; i++) {
            m[i][0] = i;
        }
        for (int j = 1; j < str2.length() + 1; j++) {
            m[0][j] = j;
        }
        for (int i = 1; i < str1.length() + 1; i++) {
            for (int j = 1; j < str2.length() + 1; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    d = 0;
                } else {
                    d = 1;
                }
                m[i][j] = Min(m[i - 1][j] + 1, m[i][j - 1] + 1, m[i - 1][j - 1] + d);
            }
        }
        return m[str1.length()][str2.length()];
    }

    public static int Min(int a, int b, int c) {
        return Math.min((Math.min(a, b)), c);
    }
}
