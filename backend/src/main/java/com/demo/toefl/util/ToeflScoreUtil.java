package com.demo.toefl.util;

public class ToeflScoreUtil {

    // 阅读原始分 → 0-30（TPO标准映射，可微调）
    public static int calcReadScore(int raw) {
        if(raw <= 0) return 0;
        if(raw == 1) return 1;
        if(raw == 2) return 2;
        if(raw == 3) return 4;
        if(raw == 4) return 6;
        if(raw == 5) return 8;
        if(raw == 6) return 10;
        if(raw == 7) return 12;
        if(raw == 8) return 14;
        if(raw == 9) return 16;
        if(raw == 10) return 18;
        if(raw == 11) return 20;
        if(raw == 12) return 22;
        if(raw == 13) return 26;
        return 30;
    }

    // 听力原始分 → 0-30
    public static int calcListenScore(int raw) {
        if(raw <= 0) return 0;
        if(raw <=2) return 3;
        if(raw <=4) return 6;
        if(raw <=6) return 9;
        if(raw <=8) return 12;
        if(raw <=10) return 15;
        if(raw <=12) return 18;
        if(raw <=14) return 22;
        if(raw <=16) return 27;
        return 30;
    }
}
