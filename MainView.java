package com.kyuhum.connect4;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.MotionEvent;
import java.util.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainView extends View {
    private Connect4AI ai = new Connect4AI(); // ← 追加

    Paint paint = new Paint();
    // Timer 処理用のハンドラ
    android.os.Handler handler = new android.os.Handler();
    // 画像読み込み
    Resources res = this.getContext().getResources();
//    Bitmap background = BitmapFactory.decodeResource(res, R.drawable.background18);
    Bitmap background = BitmapFactory.decodeResource(res, R.drawable.background17_5);
    Bitmap back2 = BitmapFactory.decodeResource(res, R.drawable.back4_7);
    Bitmap board = BitmapFactory.decodeResource(res, R.drawable.board5_6);
    Bitmap blue = BitmapFactory.decodeResource(res, R.drawable.yellow9_50);
    Bitmap red = BitmapFactory.decodeResource(res, R.drawable.red9_50);
    Bitmap select = BitmapFactory.decodeResource(res, R.drawable.select_buttom3);
    Bitmap select1 = BitmapFactory.decodeResource(res, R.drawable.select_buttom2);
    Bitmap next_end = BitmapFactory.decodeResource(res, R.drawable.next_end);
    Bitmap title = BitmapFactory.decodeResource(res, R.drawable.title);
    Bitmap cm1 = BitmapFactory.decodeResource(res, R.drawable.cm1);
    Bitmap cm2 = BitmapFactory.decodeResource(res, R.drawable.cm2);
    Bitmap cm3 = BitmapFactory.decodeResource(res, R.drawable.cm3);

    // 定数定義
    private final int KOMA_SIZE = 50;
    //    private final int BOARDX_SIZE = 360;
    private final int BOARDX_SIZE = 416;
    private final int SELECTX_SIZE = 460;
    private final int HEAD_Y_SIZE = 0;
    private final int SPACE0_Y_SIZE = 43;
    private final int SELECT_COM_Y_SIZE = 75;
    private final int SPACE1_Y_SIZE = 79;
//    private final int BOARDY_SIZE = 330;
    private final int BOARDY_SIZE = 346;
    private final int SPACE2_Y_SIZE = 60;
    private final int SELECT_MAN_Y_SIZE = SELECT_COM_Y_SIZE;
    private final int SPACE3_Y_SIZE = 20;
    private final int CM_Y_SIZE = 75;

    private final int HEAD_Y_TOP = 0;
    private final int SPACE0_Y_TOP = HEAD_Y_TOP + HEAD_Y_SIZE;
    private final int SELECT_COM_Y_TOP = SPACE0_Y_TOP + SPACE0_Y_SIZE;
    private final int SPACE1_Y_TOP = SELECT_COM_Y_TOP + SELECT_COM_Y_SIZE;
    private final int BOARDY_TOP = SPACE1_Y_TOP + SPACE1_Y_SIZE;
    private final int SPACE2_Y_TOP = BOARDY_TOP + BOARDY_SIZE;
    private final int SELECT_MAN_Y_TOP = SPACE2_Y_TOP + SPACE2_Y_SIZE;
    private final int SPACE3_Y_TOP = SELECT_MAN_Y_TOP + SELECT_MAN_Y_SIZE;
    private final int CM_Y_TOP = SPACE3_Y_TOP + SPACE3_Y_SIZE;

    private final int YOKO = 7;
    private final int TATE = 6;
    // モード
    private final int RESET = 0;
    private final int TURN = 1;
    private final int SELECT = 2;
    private final int DROP = 3;
    private final int CHECK = 4;
    private final int RESULT = 5;
    private final int UNDO = 6;
    private final int BURU = 7;
    private final int NEXT = 8;
    private final int SELECT2 = 9;
    private final int BACK = 10;
    private final int TITLE_PAGE = 11;
    // PLAYER
    private final int SPACE = 0;
    private final int COM = 1;
    private final int MAN = 2;
    // BOARD
    private final int BOARDX = (480 - BOARDX_SIZE) / 2;
    private final int KOMAX = BOARDX + 35;
    private final int POINTY = (800 - 75 - 75 - BOARDY_SIZE) / 2;
    // SELECT
    private final int SELECT_X_START =  (480 - SELECTX_SIZE) / 2;
    private final int SELECT_Y_START = BOARDY_TOP + BOARDY_SIZE + 100;
    private final int SELECT_Y_STOP = SELECT_Y_START + 100;

    private final int VS_COM = 0;
    private final int VS_MAN = 1;

    private final int MAKE = 0x04;
    private final int KATI = 0x08;

    private final int GUSU = 0;
    private final int KISU = 1;

    // 変数定義
    private int yoko, tate, yoko_disp, tate_disp;
    private int TouchX, TouchY;
    private int page = TITLE_PAGE;
    private int[][] pos = new int[YOKO][TATE];
    private int[][] log = new int[YOKO * TATE][2];
    private int count;
    private int select_on = 0;
    private int mode = VS_COM;
    private Timer timer;
    private int first;
    private int player;
    private int mask = 0xff;
    private int[][] temp = new int[YOKO + 3 + 3][TATE + 4 + 3];
    private int random = 0;
    private int x, y, cancel_req, undo;
    private Random rand = new Random();
    private int next, buru, buru2;
    private static int com_win;
    private static int man_win;
    private int cm, dare;
    List<Integer> list;
    private int[] junban = new int[7];
    private int y3, t3, num1, wake;
    int dynamicDepth = 7;
    private boolean isThinking = false; // AIが思考中かどうかのフラグ
    private int animDir = 1;            // アニメーションの移動方向（1:右, -1:左）
    // **************************************************************************************************
    // * 初期化処理
    // **************************************************************************************************
    public MainView(Context context) {
        super(context);
        timer = new Timer();
        shuffle();
        // 最初はタイトル画面なのでアクションバーを消す
        setActionBarVisible(false);
        junban[0] = 3;
        junban[1] = 2;
        junban[2] = 4;
        junban[3] = 1;
        junban[4] = 5;
        junban[5] = 0;
        junban[6] = 6;
    }

    // *****************************************************************************
    //
    // *****************************************************************************
    public void set_yoko() {
        if (TouchX < KOMAX) {
            yoko = 0xff;
            yoko_disp = 0;
        } else if (TouchX < KOMAX + (KOMA_SIZE * 7)) {
            yoko = (TouchX - KOMAX) / KOMA_SIZE;
            yoko_disp = yoko + 1;
        } else {
            yoko = 0xff;
            yoko_disp = 8;
        }
    }

    public void set_back() {
        if (count > 0) {
            if (mode != VS_MAN) {
                if (count == 1) {
                    undo = 1;
                } else {
                    undo = 2;
                }
            } else {
                undo = 1;
            }
            if (player == COM) {
                player = MAN;
                yoko_disp = 8;
            } else {
                player = COM;
                yoko_disp = 0;
            }
            count--;
            tate_disp = (log[count][1] + 1) * 2;
            pos[log[count][0]][log[count][1]] = SPACE;
            page = UNDO;
            // Timer の設定をする
            timer.schedule(new DropTask(), 0, 40);
        }
    }

    public void shuffle() {
        // 操作の対象となるコレクションをArrayListから用意する
        list = new ArrayList<Integer>();
        List collection = list;
        // 0 ～ 6 の数値が入ったリストを作成
        for (int i = 0; i < 7; i++) {
            list.add(i);
        }
        // コレクションの順序をかき混ぜる
        Collections.shuffle(list);
    }

    // **************************************************************************************************
    // * タイマー処理
    // **************************************************************************************************
    class DropTask extends TimerTask {
        public void run() {
            switch (page) {
                case SELECT:
                    page = SELECT2;
                    this.cancel();
                    handler.post(new SubThread());
                    break;
                case DROP:
                    tate_disp--;
                    if (log[count][1] >= (tate_disp / 2)) {
                        page = CHECK;
                        this.cancel();
                    }
                    handler.post(new SubThread());
                    break;
                case UNDO:
                    tate_disp++;
                    if (6 * 2 < tate_disp) {
                        if (undo > 1) {
                            undo--;
                            if (player == COM) {
                                player = MAN;
                                yoko_disp = 8;
                            } else {
                                player = COM;
                                yoko_disp = 0;
                            }
                            count--;
                            tate_disp = (log[count][1] + 1) * 2;
                            pos[log[count][0]][log[count][1]] = SPACE;
                        } else {
                            page = SELECT;
                            this.cancel();
                        }
                    }
                    handler.post(new SubThread());
                    break;
                case BACK:
                    if (cancel_req == 1) {
                        cancel_req = 0;
                        this.cancel();
                        handler.post(new SubThread());
                    }
                    break;
                case RESULT:
                    if (cancel_req == 1) {
                        cancel_req = 0;
                        buru = 10;
                        mask = 0x7f;
                        page = BURU;
                        this.cancel();
                        timer.schedule(new DropTask(), 500, 20);
                    } else {
                        mask ^= 0x80;
                        handler.post(new SubThread());
                    }
                    break;
                case BURU:
                    if (cancel_req == 1) {
                        cancel_req = 0;
                        next = 5;
                        page = NEXT;
                        this.cancel();
                        timer.schedule(new DropTask(), 0, 100);
                    }
                    handler.post(new SubThread());
                    break;
                case NEXT:
                    if (cancel_req == 1) {
                        cancel_req = 0;
                        page = TURN;
                        this.cancel();
                    }
                    handler.post(new SubThread());
                    break;
            }
        }
    }

    class SubThread implements Runnable {
        public void run() {
            invalidate();
        }
    }

    // **************************************************************************************************
    // * 描画処理
    // **************************************************************************************************
    @Override
    public void onDraw(Canvas c) {

        // 利用可能な高さを計算
        int availableHeight = getHeight(); // ActionBarは既に引かれている

        // スケール計算
        float scaleX = (float) getWidth() / 480f;
        float scaleY = (float) getHeight() / 800f;
        float scale = Math.min(scaleX, scaleY);

        // 2. 中央に寄せるためのオフセット（余白）を計算
        float offsetX = (getWidth() - (480f * scale)) / 2;
        float offsetY = (getHeight() - (800f * scale)) / 2 + 0;

        // 3. Canvasの状態を保存して、移動と拡大を適用
        c.save();
        c.translate(offsetX, offsetY); // 中央へ移動
        c.scale(scale, scale); // 480:800の世界へスケール

        if (page == TITLE_PAGE) {
            // オープニング画面（background18）を不透明で描画
            paint.setAlpha(255);
            c.drawBitmap(background, 0, 0, paint);

            // 「TAP TO START」などの文字を出すと親切です
            paint.setTextSize(24);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
//            c.drawText("TAP THE BOARD TO START", 190, 720, paint);

            c.restore();
            return; // タイトル画面の時はここで描画終了
        }

        // --- ここから下は10年前のコードのままでOK ---
        // 例: canvas.drawBitmap(background, 0, 0, paint); は
        // 自動的に「中央の480x800の枠内」に描画されます
        int i, k;
        // 背景を表示
        paint.setAlpha(255);  // 半透明にする
//        c.drawBitmap(background, 0, 0, paint);
        c.drawBitmap(back2, 0, 30, paint);
        paint.setAlpha(255);  // 元に戻す（次の描画のため）
        // SELETを表示
        if (page == RESULT) {
            c.drawBitmap(next_end, SELECT_X_START, SELECT_MAN_Y_TOP, paint);
        } else {
            c.drawBitmap(select, SELECT_X_START, SELECT_MAN_Y_TOP, paint);
        }
        // CMを表示
//        if (cm == 0) {
//            c.drawBitmap(cm1, 0, CM_Y_TOP + 1, paint);
//        } else if (cm == 1) {
//            c.drawBitmap(cm2, 0, CM_Y_TOP + 1, paint);
//        } else {
//            c.drawBitmap(cm3, 0, CM_Y_TOP + 1, paint);
//        }
        // タイトルを表示
        if (mode == VS_MAN) {
            c.drawBitmap(select1, SELECT_X_START, SELECT_COM_Y_TOP + 29, paint);
        } else {
//            c.drawBitmap(title, SELECT_X_START, SELECT_COM_Y_TOP + 29, paint);
        }

        switch (page) {
            case RESET:
                wake = 0;
                com_win = 0;
                man_win = 0;
                first = COM;
                page = TURN;
                // break;
            case TURN:
                if (first == COM) {
                    first = MAN;
                    yoko_disp = 8;
                } else {
                    first = COM;
                    yoko_disp = 0;
                }
                player = first;

                for (yoko = 0; yoko < YOKO; yoko++) {
                    for (tate = 0; tate < TATE; tate++) {
                        pos[yoko][tate] = SPACE;
                    }
                }
                for (count = 0; count < YOKO * TATE; count++) {
                    log[count][0] = SPACE;
                    log[count][1] = SPACE;
                }
                count = 0;
                page = SELECT;
                aiMove();      // 列を決める
                // break;
            case SELECT:
                if ((player == COM) && (mode == VS_COM)) {
                    timer.schedule(new DropTask(), 1000, 1000);
                } else {
                    page = SELECT2;
                }
                // 開始時の駒表示
                if (player == COM) {
                    c.drawBitmap(blue, BOARDX - 15 + yoko_disp * KOMA_SIZE, BOARDY_TOP - KOMA_SIZE, paint);
                } else {
                    c.drawBitmap(red, BOARDX - 15 + yoko_disp * KOMA_SIZE, BOARDY_TOP - KOMA_SIZE, paint);
                }
                break;
            case SELECT2:
//                if (mode == VS_COM) {
//                    aiMove();
//                }
                // 選択中の駒表示
                if (player == COM) {
                    c.drawBitmap(blue, BOARDX - 15 + yoko_disp * KOMA_SIZE, BOARDY_TOP - KOMA_SIZE, paint);
                } else {
                    c.drawBitmap(red, BOARDX - 15 + yoko_disp * KOMA_SIZE, BOARDY_TOP - KOMA_SIZE, paint);
                }
                break;
            case DROP:
            case UNDO:
                // 落下中の駒表示
                if (player == COM) {
                    c.drawBitmap(blue,KOMAX + log[count][0] * KOMA_SIZE,
                            BOARDY_TOP + BOARDY_SIZE - 35 - tate_disp * KOMA_SIZE / 2, paint);
                } else {
                    c.drawBitmap(red, KOMAX + log[count][0] * KOMA_SIZE,
                            BOARDY_TOP + BOARDY_SIZE - 35 - tate_disp * KOMA_SIZE / 2, paint);
                }
                break;
            case CHECK:
                int lastX = log[count][0];
                int lastY = log[count][1];
                pos[lastX][lastY] = player;
                count++;
                if (Connect4AI.check4(pos, lastX, lastY, player)) {
                    // 勝った場合のみ、演出用の 0x80 フラグを立てる
                    markWinningLine(lastX, lastY, player);
                    if (player == COM) {
                        player = MAN;
                        com_win++;
                    } else {
                        player = COM;
                        man_win++;
                    }
                    wake = 0;
                    page = RESULT;
                    timer.schedule(new DropTask(), 0, 200);
                } else {
                    if (count < (YOKO * TATE)) {
                        if (player == COM) {
                            player = MAN;
                            yoko_disp = 8;
                        } else {
                            player = COM;
                            yoko_disp = 0;

                            if (mode == VS_COM) {
                                page = SELECT;
                                aiMove();   // ← ここだけ
                            }
                        }
                        page = SELECT;
                    } else {
                        wake = 1;
                        page = RESULT;
                        timer.schedule(new DropTask(), 0, 200);
                    }
                }
//                if (player == COM) {
//                    player = MAN;
//                    yoko_disp = 8;
//                } else {
//                    player = COM;
//                    yoko_disp = 0;
//                }
                invalidate();
                break;
            case BACK:
                // BACK
                for (yoko = 0; yoko < YOKO; yoko++) {
                    for (tate = 0; tate < TATE; tate++) {
                        pos[yoko][tate] &= 0x03;
                    }
                }
                if (wake == 0) {
                    if (player == MAN) {
                        com_win--;
                    } else {
                        man_win--;
                    }
                }
                set_back();
                break;
            case RESULT:
                break;
            case BURU:
                if ((buru % 2) == 0) {
                    buru2 = -2;
                } else {
                    buru2 = 2;
                }
                buru--;
                if (buru == 0) {
                    cancel_req = 1;
                }
                break;
            case NEXT:
                for (i = 0; i < YOKO; i++) {
                    for (k = 0; k < TATE - 1; k++) {
                        pos[i][k] = pos[i][k + 1];
                    }
                    pos[i][k] = 0;
                }
                next--;
                if (next == 0) {
                    cm = (cm + 1) % 2;
                    cancel_req = 1;
                }
                break;
        }
        // 駒を表示
        for (i = 0; i < YOKO; i++) {
            for (k = 0; k < TATE; k++) {
                if ((pos[i][k] & mask) == MAN) {
                    c.drawBitmap(red, buru2 + KOMAX + (i * KOMA_SIZE),
                            BOARDY_TOP + BOARDY_SIZE - KOMA_SIZE - 36 - (k * KOMA_SIZE), paint);
                } else if ((pos[i][k] & mask) == COM) {
                    c.drawBitmap(blue, buru2 + KOMAX + (i * KOMA_SIZE),
                            BOARDY_TOP + BOARDY_SIZE - KOMA_SIZE - 36 - (k * KOMA_SIZE), paint);
                }
            }
        }
        // ボードの表示
        c.drawBitmap(board, buru2 + BOARDX, BOARDY_TOP, paint);
        // 点数の表示
        paint.setTextSize(48);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setColor(Color.YELLOW);
        c.drawText(String.valueOf(com_win), 180, SPACE2_Y_TOP + 40, paint);
        // 標準の書体（太字ではない）に戻す
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.WHITE);
        c.drawText("vs", 242, SPACE2_Y_TOP + 40, paint);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(Color.RED);
        c.drawText(String.valueOf(man_win), 300, SPACE2_Y_TOP + 40, paint);
        // デバッグ表示
//        paint.setTextSize(24);
//         for(i=0; i<YOKO; i++){
//         for(k=0; k<TATE; k++){
//         String hex = Integer.toHexString(pos[i][k]);
//         c.drawText(hex, 87+((KOMA_SIZE+1)*i),
//         SPACE2_Y_TOP-KOMA_SIZE+3-((KOMA_SIZE-1)*k), paint);
//         }
//         }
//         c.drawText(String.valueOf(dynamicDepth), 80, 70, paint);
//         for(x=0; x<YOKO; x++){
//         c.drawText(String.valueOf(Connect4AI.forbidden[x]), 90+KOMA_SIZE*x, 232, paint);
//         }
        //
        random = rand.nextInt(7);
        c.restore(); // 状態を戻す
    }

    // **************************************************************************************************
    // * タッチ入力処理
    // **************************************************************************************************
    public boolean onTouchEvent(MotionEvent event) {
        float scaleX = (float) getWidth() / 480f;
        float scaleY = (float) getHeight() / 800f;
        float scale = Math.min(scaleX, scaleY);

        float offsetX = (getWidth() - (480f * scale)) / 2;
        float offsetY = (getHeight() - (800f * scale)) / 2 + 0;

        // 生の座標を 480:800 世界の座標に変換
        float touchX = (event.getX() - offsetX) / scale;
        float touchY = (event.getY() - offsetY) / scale;

        // 以降、event.getX() の代わりに touchX を、
        // event.getY() の代わりに touchY を使って判定してください。
        TouchX = (int) touchX;
        TouchY = (int) touchY;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (page == TITLE_PAGE) {
                    // イラスト内の青いボード付近（おおよそ下半分）をタップしたら開始
                    if (TouchY > 600 && TouchX < 240) {
                        setActionBarVisible(true); // ゲームが始まるので表示する
                        page = RESET; // RESETに飛ばしてゲーム開始
                        invalidate();
                    }
                    break;
                }
                // *****************************************************
                // * DOWN 処理
                // *****************************************************

                switch (page) {
                    case RESET:
                        // 駒をクリア
                        for (yoko = 0; yoko < YOKO; yoko++) {
                            for (tate = 0; tate < TATE; tate++) {
                                pos[yoko][tate] = 0;
                            }
                        }
                        // LOGをクリア
                        for (count = 0; count < YOKO * TATE; count++) {
                            log[count][0] = 0;
                            log[count][1] = 0;
                        }
                        count = 0;
                        // COMから開始
                        page = SELECT;
                        invalidate();
                        break;
                    case SELECT2:
                        // SELECT
                        if (((mode == VS_MAN) && (SELECT_COM_Y_TOP <= TouchY && TouchY <= SPACE1_Y_TOP)) ||
                                (SELECT_MAN_Y_TOP <= TouchY && TouchY <= SPACE3_Y_TOP)) {
                            select_on = player;
                            set_yoko();
                            invalidate();
                        }
                        // BACK
                        if (BOARDX <= TouchX && TouchX <= BOARDX + BOARDX_SIZE && BOARDY_TOP <= TouchY
                                && TouchY <= BOARDY_TOP + BOARDY_SIZE) {
                            set_back();
                        }

                        break;
                    case DROP:
                        break;
                    case CHECK:
                        break;
                    case RESULT:
                        // ボードをタッチしてBACK
                        if (BOARDX <= TouchX && TouchX <= BOARDX + BOARDX_SIZE && BOARDY_TOP <= TouchY
                                && TouchY <= BOARDY_TOP + BOARDY_SIZE) {
                            cancel_req = 1;
                            page = BACK;
                        }
                        if (SPACE2_Y_TOP <= TouchY && TouchY <= SPACE3_Y_TOP) {
                            // Next
                            if (240 < TouchX && TouchX < 340) {
                                cancel_req = 1;
                                // buru = 10;
                                // page = BURU;
                                // timer.schedule(new DropTask(), 500, 20);
                            }
                            // End (リザルト画面の「終了」ボタンエリア)
                            if (360 <= TouchX) {
//                                System.exit(0);
                                if (timer != null) timer.cancel(); // 実行中のタイマー（点滅など）を止める
                                setActionBarVisible(false); // タイトルに戻るので隠す
                                page = TITLE_PAGE;                 // タイトル画面の状態にする
                                invalidate();                      // 「今すぐ描き直して！」とシステムに命令する
                            }
                        }
                }
                // ボードをタップしてcmを切り替える
                if (CM_Y_TOP <= TouchY && TouchY <= CM_Y_TOP + CM_Y_SIZE) {
                    cm = (cm + 1) % 2;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // *****************************************************
                // * MOVE 処理
                // *****************************************************
                switch (page) {
                    case SELECT2:
                        if (select_on != SPACE) {
                            set_yoko();
                            invalidate();
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // *****************************************************
                // * UP 処理
                // *****************************************************
                switch (page) {
                    case RESET:
                        break;
                    case SELECT2:
                        if (select_on != SPACE) {
                            select_on = SPACE;
                            if (0 <= yoko && yoko <= 6) {
                                for (tate = 0; tate < TATE; tate++) {
                                    if ((pos[yoko][tate] & 0x03) == SPACE) {
                                        log[count][0] = yoko;
                                        log[count][1] = tate;
                                        page = DROP;
                                        // Timer の設定をする
                                        tate_disp = 6 * 2;
                                        timer.schedule(new DropTask(), 0, 40);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case DROP:
                        break;
                    case CHECK:
                        break;
                    case RESULT:
                        break;
                }
                break;
        }
        return true;
    }

    private void aiMove() {
        if (page != SELECT) return;
        if (player != COM) return;
        // アニメーション開始
        isThinking = true;
        startThinkingAnimation();
        // 思考開始時刻を記録
        long startTime = System.currentTimeMillis();
        // 盤面コピー（UIと分離するため重要）
        int[][] posCopy = copyBoard(pos);
        // 手数をカウントして深さを決定
        dynamicDepth = Connect4AI.getDynamicDepth(posCopy);
        new Thread(() -> {
            // 1. AIの思考（計算実行）
            int bestCol = Connect4AI.chooseBestMove(posCopy, dynamicDepth, first);
            // 2. 経過時間を計算
            long elapsedTime = System.currentTimeMillis() - startTime;
            long minDuration = 2000; // 最低2秒
            // 3. 2秒に満たない場合は、その分だけスレッドを眠らせる
            if (elapsedTime < minDuration) {
                try {
                    Thread.sleep(minDuration - elapsedTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 4. UIスレッドに戻って駒を落とす
            post(() -> {
                isThinking = false;
                applyMove(bestCol);
            });
        }).start();
    }
    private void setActionBarVisible(boolean visible) {
        Context context = getContext();
        if (context instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null) {
                if (visible) {
                    actionBar.show();
                } else {
                    actionBar.hide();
                }
            }
        }
    }
    public static int[][] copyBoard(int[][] board) {
        int[][] newBoard = new int[Connect4AI.WIDTH][Connect4AI.HEIGHT];

        for (int x = 0; x < Connect4AI.WIDTH; x++) {
            System.arraycopy(board[x], 0, newBoard[x], 0, Connect4AI.HEIGHT);
        }

        return newBoard;
    }

    // COMの決定した列に駒を移動させて落とす。
    private void applyMove(int bestCol) {
        if (bestCol < 0)
            return;

        isThinking = false; // 念のためここでもフラグを折る
        yoko_disp = bestCol; // 決定した列に駒を移動させる

        log[count][0] = bestCol;
        log[count][1] = findDropRow(bestCol);

        tate_disp = (TATE + 1) * 2;
        page = DROP;

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new DropTask(), 0, 40);
        invalidate();
    }
    // handler を使って、一定時間ごとに yoko_disp を更新し再描画させるメソッド
    private void startThinkingAnimation() {
        if (!isThinking) return;

        // 左右に移動させるロジック
        yoko_disp += animDir;
        if (yoko_disp >= 7) animDir = -1; // 右端まで行ったら左へ
        if (yoko_disp <= 1) animDir = 1;  // 左端まで行ったら右へ

        invalidate(); // 再描画

        // 100ミリ秒（0.1秒）後に自分自身を再度呼び出す
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startThinkingAnimation();
            }
        }, 100);
    }

    // 指定列に石が落ちる行を返す（なければ -1）
    private int findDropRow(int col) {
        for (int y = 0; y < TATE; y++) {   // 下から上へ
            if (pos[col][y] == Connect4AI.EMPTY) {
                return y;
            }
        }
        return -1; // その列は満杯
    }

    // ******************************************************************************
    //
    // ******************************************************************************
    public void new_com() {
        mode = VS_COM;
        page = RESET;
        com_win = 0;
        man_win = 0;
        // first = MAN;
        invalidate();
    }

    public void new_man() {
        mode = VS_MAN;
        page = RESET;
        com_win = 0;
        man_win = 0;
        // first = MAN;
        invalidate();
    }

    // ******************************************************************************
    // 4駒揃ったかを判定
    // ******************************************************************************
    private void markWinningLine(int x, int y, int p) {
        // 判定に使ったのと同じ方向ベクトル（Connect4AI.DIRECTIONS を参照）
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        for (int[] d : directions) {
            int dx = d[0];
            int dy = d[1];

            // その方向で4つ以上並んでいるか再確認
            if (1 + Connect4AI.countDir(pos, x, y, dx, dy, p) + Connect4AI.countDir(pos, x, y, -dx, -dy, p) >= 4) {
                // 中心（今置いた石）をマーク
                pos[x][y] |= 0x80;
                // 正方向に歩きながらマーク
                markDir(x, y, dx, dy, p);
                // 逆方向に歩きながらマーク
                markDir(x, y, -dx, -dy, p);
            }
        }
    }

    // 描画フラグを立てるための歩行用メソッド
    private void markDir(int x, int y, int dx, int dy, int p) {
        int nx = x + dx;
        int ny = y + dy;
        // p (1or2) と一致する間、0x80 を立て続ける
        while (nx >= 0 && nx < YOKO && ny >= 0 && ny < TATE && (pos[nx][ny] & 0x03) == p) {
            pos[nx][ny] |= 0x80;
            nx += dx;
            ny += dy;
        }
    }
}
