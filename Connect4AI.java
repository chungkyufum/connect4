package com.kyuhum.connect4;

import java.util.*;
import java.security.DrbgParameters.NextBytes;
public class Connect4AI {

    // ===== 盤面設定 =====
    static final int WIDTH = 7;
    static final int HEIGHT = 6;
    static final int DEFAULT_DEPTH = 11;

    static final int EMPTY = 0x00;
    static final int COM = 0x01;
    static final int MAN = 0x02;
    static final int HIGHLIGHT_COM = 0x20;
    static final int HIGHLIGHT_MAN = 0x40;
    static final int HIGHLIGHT_WIN = 0x80;
    static final int STONE_MASK = MAN | COM;

    static final int WIN_SCORE = 100_000_000;
    static final int FORK_SCORE = 1_000_000;
    static final int LOSS4_PENALTY = 500_000;
    static final int LOSS33_PENALTY = 200_000;
    static final int THREE_SCORE = 10_000;
    static final int TWO_SCORE = 300;
    static final int CENTER_SCORE = 200;
    static final int PARITY_SCORE = 150;
    static final int MOBILITY_SCORE = 50;
    static final int THREAT_SCORE = 200;

    static final int[] ORDERED_COLS = { 3, 2, 4, 1, 5, 0, 6 };
    static final int[][] DIRECTIONS = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };
    static final Random rand = new Random();
    static int[] forbidden = new int[WIDTH]; // 禁止列のマーク

    // piece-square table方式(駒位置の重み)
    static final int[][] POSITION_SCORE = {
            { 3, 4, 5, 5, 4, 3 }, // x=0
            { 4, 6, 8, 8, 6, 4 }, // x=1
            { 5, 8, 11, 11, 8, 5 }, // x=2
            { 7, 10, 13, 13, 10, 7 }, // x=3
            { 5, 8, 11, 11, 8, 5 }, // x=4
            { 4, 6, 8, 8, 6, 4 }, // x=5
            { 3, 4, 5, 5, 4, 3 } // x=6
    };

    static final boolean DEBUG = false;
    static final boolean DEBUG1 = false;
    static long nodeCount = 0;

    // ===== Window（4マス連続パターン）の事前計算 =====
    static class Window {
        int[][] pos = new int[4][2];

        Window(int x, int y, int dx, int dy) {
            for (int i = 0; i < 4; i++) {
                pos[i][0] = x + dx * i;
                pos[i][1] = y + dy * i;
            }
        }
    }

    static final List<Window> ALL_WINDOWS = new ArrayList<>();
    static List<Window>[][] WINDOWS_FROM_CELL;

    static {
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x <= WIDTH - 4; x++)
                ALL_WINDOWS.add(new Window(x, y, 1, 0)); // 横
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y <= HEIGHT - 4; y++)
                ALL_WINDOWS.add(new Window(x, y, 0, 1)); // 縦
        for (int x = 0; x <= WIDTH - 4; x++)
            for (int y = 0; y <= HEIGHT - 4; y++)
                ALL_WINDOWS.add(new Window(x, y, 1, 1)); // 斜め右上
        for (int x = 0; x <= WIDTH - 4; x++)
            for (int y = 3; y < HEIGHT; y++)
                ALL_WINDOWS.add(new Window(x, y, 1, -1)); // 斜め右下

        WINDOWS_FROM_CELL = new ArrayList[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++)
                WINDOWS_FROM_CELL[x][y] = new ArrayList<>();

        for (Window w : ALL_WINDOWS)
            for (int[] p : w.pos)
                WINDOWS_FROM_CELL[p[0]][p[1]].add(w);
    }

    // ===== AI思考エンジン =====
    public static int chooseBestMove(int[][] board, int depth, int first) {
        nodeCount = 0;

        // ⑦ minimax で評価（禁止列を避ける）
        int bestScore = Integer.MIN_VALUE;
        List<Integer> bestMoves = new ArrayList<>();
        int[] scores = new int[WIDTH]; // ← デバッグ用スコア保存
        // boolean noPlace = false; // 置く場所なし
        int score = 0;

        for (int x : ORDERED_COLS) {
            int y = nextEmptyY(board, x);
            if (y < 0) {
                continue;
            } else {
                board[x][y] = COM;
                boolean is4 = check4(board, x, y, COM);
                if (is4 == true) {
                    score = (WIN_SCORE + depth);
                } else {
                    score = minimax(board, depth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE, first);
                }
                board[x][y] = EMPTY;
            }
            // noPlace = true; // 置く場所あり
            scores[x] = score; // ← スコア保存
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(x);
            } else if (score == bestScore) {
                bestMoves.add(x);
            }
        }

        // ★ デバッグ表示
        System.out.println("=== AI思考結果 ===");
        System.out.print("各列スコア: ");
        for (int x = 0; x < WIDTH; x++) {
            System.out.printf("%d:%6d ", x, scores[x]);
        }
        System.out.println();
        System.out.println("最善手: " + bestMoves + " (スコア: " + bestScore + ")");

        // 勝ち確定判定
        if (bestScore > WIN_SCORE / 2) {
            if (bestScore >= WIN_SCORE) {
                System.out.println("★★★ 勝ち確定！ ★★★");
            } else {
                System.out.println("★ 勝ちに近い局面（評価値 " + bestScore + "）");
            }
        } else if (bestScore < -WIN_SCORE / 2) {
            if (bestScore <= -WIN_SCORE) {
                System.out.println("▼▼▼ 負け確定...▼▼▼");
            } else {
                System.out.println("▼ 負けに近い局面（評価値 " + bestScore + "）");
            }
        } else if (bestScore > FORK_SCORE / 2) {
            System.out.println("→ 有利な局面（評価値 " + bestScore + "）");
        } else if (bestScore < -FORK_SCORE / 2) {
            System.out.println("← 不利な局面（評価値 " + bestScore + "）");
        }
        System.out.println("==================");
        System.out.println("nodes=" + nodeCount);
        // if (noPlace == false) { // 置く場所なし
        // return evaluatePosition(board, COM, first);
        // }
        // 置く場所あり
        return bestMoves.isEmpty() ? -1 : bestMoves.get(rand.nextInt(bestMoves.size()));
    }

    // ===== Minimax =====
    static int minimax(int[][] board, int depth, boolean isMax, int alpha, int beta, int first) {
        int player = isMax ? COM : MAN;
        int best = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int score = 0;

        if (depth == 0)
            return evaluatePosition(board, COM, first);

        boolean hasMove = false;
        for (int x : ORDERED_COLS) {
            int y = nextEmptyY(board, x);
            if (y < 0) {
                continue;
            }
            hasMove = true;
            board[x][y] = player;
            boolean is4 = check4(board, x, y, player);
            if (is4 == true) {
                board[x][y] = EMPTY;
                return (player == COM) ? (WIN_SCORE + depth) : (-WIN_SCORE - depth);
            } else {
                score = minimax(board, depth - 1, !isMax, alpha, beta, first);
            }
            board[x][y] = EMPTY;

            if (isMax) {
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, score);
                beta = Math.min(beta, best);
            }
            if (beta <= alpha)
                break;
        }
        if (!hasMove)
            return 0; // draw
        return best;
    }

    // ===== 評価関数 =====
    static int evaluatePosition(int[][] board, int player, int first) {

        int opponent = (player == COM) ? MAN : COM;

        int score = 0, com_score = 0, man_score = 0;
        int[] res = new int[2];
        nodeCount++;

        int win33 = checkWin33(board, player);
        if (win33 >= 0) {
            // Main.displayBoard(board);
            com_score += FORK_SCORE;
        }
        int los33 = checkWin33(board, opponent);
        if (los33 >= 0) {
            // Main.displayBoard(board);
            man_score += FORK_SCORE;
        }

        // 評価ループ
        for (Window w : ALL_WINDOWS) {
            // 自分の窓を評価
            analyzeWindow(board, w, player, res);
            com_score += getWindowScore(board, w, res[0], res[1], player, first);

            // 相手の窓を評価
            analyzeWindow(board, w, opponent, res);
            man_score += getWindowScore(board, w, res[0], res[1], opponent, first);
        }

        // 中央列加点
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int stone = board[x][y] & STONE_MASK;
                if (stone == COM)
                    com_score += POSITION_SCORE[x][y];
                else if (stone == MAN)
                    man_score += POSITION_SCORE[x][y];
            }
        }

        score = com_score - (man_score * 2);
        if (DEBUG1) {
            System.out.println(String.format("score =%6d(%6d %6d)", score, com_score, man_score));
        }
        return score;
    }

    // Windowの解析：4マスに、自石の数、空マスの数、敵石があるかを判定する関数です。
    private static void analyzeWindow(int[][] board, Window w, int player, int[] results) {
        int mine = 0, empty = 0;
        for (int[] p : w.pos) { // 4マスを順番に見る。
            int cell = board[p[0]][p[1]] & STONE_MASK;
            if (cell == player) // 自石の数
                mine++;
            else if (cell == EMPTY) // 空マスの数
                empty++;
            else { // 敵石があった(評価対象外)
                results[0] = -1;
                results[1] = -1;
                return;
            }
        }
        results[0] = mine;
        results[1] = empty;
    }

    private static int getWindowScore(int[][] board, Window w, int mine, int empty, int player, int first) {

        if (mine == 4) {
            // Main.displayBoard(board); // debug
            return WIN_SCORE;
        }
        if (mine == 3 && empty == 1) {
            // Main.displayBoard(board); // debug
            int[] e = findSingleEmpty(board, w);
            if (e == null)
                return THREE_SCORE;

            int ex = e[0]; // ３の空マスの x
            int ey = e[1]; // ３の空マスの y
            int nextY = nextEmptyY(board, ex); // 次に置ける y

            int base = THREE_SCORE;

            // ３の空マスの y = 次に置ける y か？
            if (nextY == ey) {
                base /= 2; // 即置ける３は即止められる
            } else if (nextY < ey) {
                int distance = ey - nextY;
                if (distance == 1)
                    base *= 3; // 1段浮き
                else
                    base *= 2; // 2段以上浮き

                // playerが先手か？
                boolean isFirst = (player == first);

                // 偶数段か？
                boolean evenMoves = (ey % 2 == 0);

                // 先手が偶数段に打てる（有利）
                if (isFirst && evenMoves) {
                    base *= 2;
                }

                // 後手が奇数段に打てる（有利）
                if (!isFirst && !evenMoves) {
                    base *= 2;
                }
            }
            return base;
        }

        if (mine == 2 && empty == 2)
            return TWO_SCORE;

        return 0;
    }

    // (x, y) を中心に4方向について正方向＋逆方向に連続石を数え合計が4以上かを判定
    static boolean check4(int[][] b, int x, int y, int p) {
        for (int[] d : DIRECTIONS) {
            if (1 + countDir(b, x, y, d[0], d[1], p)
                    + countDir(b, x, y, -d[0], -d[1], p) >= 4)
                return true;
        }
        return false;
    }

    // 一方向に連続石を数える
    static int countDir(int[][] b, int x, int y, int dx, int dy, int p) {
        int c = 0, nx = x + dx, ny = y + dy;
        while (inBoard(nx, ny) && (b[nx][ny] & STONE_MASK) == p) {
            c++;
            nx += dx;
            ny += dy;
        }
        return c;
    }

    private static int[] findSingleEmpty(int[][] board, Window w) {
        for (int[] p : w.pos) {
            if ((board[p[0]][p[1]] & STONE_MASK) == EMPTY)
                return p; // {x,y}
        }
        return null;
    }

    // ユーティリティ
    static int nextEmptyY(int[][] b, int x) {
        for (int y = 0; y < HEIGHT; y++)
            if ((b[x][y] & STONE_MASK) == EMPTY)
                return y;
        return -1;
    }

    // ユーティリティ
    static boolean inBoard(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    // 勝敗判定(Main.java)
    static boolean hasAnyMove(int[][] board) {
        for (int x = 0; x < WIDTH; x++)
            if (nextEmptyY(board, x) >= 0)
                return true;
        return false;
    }

    // 表示用ハイライト(Main.java)
    static boolean checkWinAndMark(int[][] display, int player) {
        boolean won = false;
        int[] res = new int[2];
        for (Window w : ALL_WINDOWS) {
            analyzeWindow(display, w, player, res);
            if (res[0] == 4) {
                for (int[] p : w.pos)
                    display[p[0]][p[1]] |= HIGHLIGHT_WIN;
                won = true;
            }
        }
        return won;
    }

    // ハイライト更新
    static void markAllThreateningThree(int[][] src, int[][] dst, int player, int flag) {
        int[] res = new int[2];
        for (Window w : ALL_WINDOWS) {
            analyzeWindow(src, w, player, res);
            if (res[0] == 3 && res[1] == 1) {
                for (int[] p : w.pos)
                    if ((src[p[0]][p[1]] & STONE_MASK) == player)
                        dst[p[0]][p[1]] |= flag;
            }
        }
    }

    // 現在の合計手数をカウントする(Main.java)
    static int countTotalStones(int[][] board) {
        int count = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if ((board[x][y] & STONE_MASK) != EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    // 置ける列の数をカウントする(Main.java)
    static int countPlayableCols(int[][] board) {
        int count = 0;
        for (int x = 0; x < WIDTH; x++) {
            if (nextEmptyY(board, x) != -1) {
                count++;
            }
        }
        return count;
    }

    // 動的に深さを決定する(Main.java)
    static int getDynamicDepth(int[][] board) {
        int totalStones = countTotalStones(board); // 置いた石の合計
        int remaining = WIDTH * HEIGHT - totalStones; // 残りのマス
        int playableCols = countPlayableCols(board); // 空マスがある列の数
        int baseDepth; // 深度

        // 置ける列が少ない程、depthを深くする(0,2,4,...)
        baseDepth = (7 - playableCols);
        // 手数により深くする
        if (totalStones <= 7) { // 初盤（〜7手）
            baseDepth += 6;
        } else if (totalStones <= 14) { // 前盤（〜14手）
            baseDepth += 7;
        } else if (totalStones <= 21) { // 中盤（〜21手）
            baseDepth += 8;
        } else { // 終盤（22手〜）
            baseDepth = remaining; // 残りを全て読む(max=20)
        }
        // 偶数化（COM手番評価固定）
        baseDepth &= ~1;
        return baseDepth;
    }

    // 全列のどこかに４ができるかを判定
    static int checkWin4(int[][] board, int p) {
        for (int x : ORDERED_COLS) {
            int y = nextEmptyY(board, x);
            if (y < 0)
                continue;

            board[x][y] = p;
            boolean is4 = check4(board, x, y, p);
            board[x][y] = EMPTY;
            if (is4 == true) {
                return x;
            }
        }
        return -1;
    }

    // 全列のどこかに３３ができるかを判定
    static int checkWin33(int[][] board, int p) {
        int o = (p == COM) ? MAN : COM;
        for (int x : ORDERED_COLS) {
            int y = nextEmptyY(board, x);
            if (y < 0)
                continue;
            boolean isWin33 = true;
            board[x][y] = p; // 自分が仮置き
            for (int x2 : ORDERED_COLS) {
                int y2 = nextEmptyY(board, x2);
                if (y2 < 0)
                    continue;
                board[x2][y2] = o; // 相手が仮置き
                int isWin4 = checkWin4(board, p); // 自分に４ができるか？
                board[x2][y2] = EMPTY;
                if (isWin4 == -1) {
                    isWin33 = false;
                    break;
                }
            }
            board[x][y] = EMPTY;
            if (isWin33 == true) {
                return x;
            }
        }
        return -1;
    }

    // // ４の禁止列をマーク（置くと相手が４になる列）
    // static void forbidden4(int[][] board) {
    // Arrays.fill(forbidden, EMPTY);
    // for (int p : new int[] { COM, MAN }) {
    // int o = (p == COM) ? MAN : COM;
    // for (int x : ORDERED_COLS) {
    // int y = nextEmptyY(board, x);
    // if (y < 0) {
    // // forbidden[x] |= p;
    // continue;
    // }
    // board[x][y] = p;
    // int isWin = checkWin4(board, o);
    // if (isWin != -1) {
    // forbidden[x] |= p;
    // }
    // board[x][y] = EMPTY;
    // }
    // }
    // }

    // // ３３の禁止列をマーク（置くと相手が３３になる列）
    // static void forbidden33(int[][] board) {
    // for (int p : new int[] { COM, MAN }) {
    // int o = (p == COM) ? MAN : COM;
    // for (int x : ORDERED_COLS) {
    // int y = nextEmptyY(board, x);
    // if (y < 0) {
    // // forbidden[x] |= p;
    // continue;
    // }
    // board[x][y] = p;
    // int isWin = checkWin33(board, o);
    // if (isWin != -1)
    // forbidden[x] |= p;
    // board[x][y] = EMPTY;
    // }
    // }
    // }

    // // 配列が すべて 0 かを判定
    // static boolean isAllZero(int[] arr) {
    // for (int v : arr) {
    // if (v != 0)
    // return false;
    // }
    // return true;
    // }

    // // 置くと自分に４ができる列をマーク
    // static int[] win4 = new int[WIDTH];

    // static void win4(int[][] board) {
    // Arrays.fill(win4, EMPTY);
    // for (int p : new int[] { COM, MAN }) {
    // for (int x : ORDERED_COLS) {
    // int y = nextEmptyY(board, x);
    // if (y < 0)
    // continue;
    // board[x][y] = p;
    // boolean is4 = check4(board, x, y, p);
    // board[x][y] = EMPTY;
    // if (is4 == true) {
    // win4[x] |= p;
    // }
    // }
    // }
    // }

    // // 置くと相手に４ができる列をマーク
    // static int[] loss4 = new int[WIDTH];

    // static void loss4(int[][] board) {
    // Arrays.fill(loss4, EMPTY);
    // for (int p : new int[] { COM, MAN }) {
    // int o = (p == COM) ? MAN : COM;
    // for (int x : ORDERED_COLS) {
    // int y = nextEmptyY(board, x);
    // if (y < 0)
    // continue;
    // boolean is4 = false;
    // board[x][y] = p;
    // for (int x2 : ORDERED_COLS) {
    // int y2 = nextEmptyY(board, x2);
    // if (y2 < 0)
    // continue;
    // board[x2][y2] = o;
    // is4 = check4(board, x2, y2, o);
    // board[x2][y2] = EMPTY;
    // if (is4 == true) {
    // break;
    // }
    // }
    // board[x][y] = EMPTY;
    // if (is4 == true) {
    // loss4[x] |= p;
    // }
    // }
    // }
    // }

    // // 置くと自分に３３ができる列をマーク
    // static int[] win33 = new int[WIDTH];

    // static void win33(int[][] board) {
    // Arrays.fill(win33, EMPTY);
    // for (int p : new int[] { COM, MAN }) {
    // for (int x : ORDERED_COLS) {
    // int y = nextEmptyY(board, x);
    // if (y < 0)
    // continue;
    // int count = 0;
    // board[x][y] = p;
    // for (int x2 : ORDERED_COLS) {
    // int y2 = nextEmptyY(board, x2);
    // if (y2 < 0)
    // continue;
    // board[x2][y2] = p;
    // boolean is4 = check4(board, x2, y2, p);
    // board[x2][y2] = EMPTY;
    // if (is4 == true) {
    // count++;
    // }
    // }
    // board[x][y] = EMPTY;
    // if (count >= 2) {
    // win33[x] |= p;
    // }
    // }
    // }
    // }

    // // 置くと相手に３３ができる列をマーク
    // static int[] loss33 = new int[WIDTH];

    // static void loss33(int[][] board) {
    // Arrays.fill(loss33, EMPTY);
    // for (int p : new int[] { COM, MAN }) {
    // int o = (p == COM) ? MAN : COM;
    // for (int x : ORDERED_COLS) {
    // int y = nextEmptyY(board, x);
    // if (y < 0)
    // continue;
    // int count = 0;
    // board[x][y] = p;
    // for (int x2 : ORDERED_COLS) {
    // int y2 = nextEmptyY(board, x2);
    // if (y2 < 0)
    // continue;
    // board[x2][y2] = o;
    // count = 0;
    // for (int x3 : ORDERED_COLS) {
    // int y3 = nextEmptyY(board, x3);
    // if (y3 < 0)
    // continue;
    // board[x3][y3] = o;
    // boolean is4 = check4(board, x3, y3, o);
    // board[x3][y3] = EMPTY;
    // if (is4 == true) {
    // count++;
    // }
    // }
    // board[x2][y2] = EMPTY;
    // if (count >= 2) {
    // break;
    // }
    // }
    // board[x][y] = EMPTY;
    // if (count >= 2) {
    // loss33[x] |= p;
    // }
    // }
    // }
    // }
}
