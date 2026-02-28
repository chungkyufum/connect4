package com.kyuhum.connect4;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

public class MainActivity extends AppCompatActivity {

    MainView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Viewをセット
        view = new MainView(this);
        setContentView(view);
    }

    // オプションメニューが表示される度に呼び出されます
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // メニューインフレーターを取得
        MenuInflater menuInflater = getMenuInflater();
        // xmlのリソースファイルを使用してメニューにアイテムを追加
        menuInflater.inflate(R.menu.options_menu, menu);
        // できたらtrueを返す
        return true;
    }

    // オプションメニューアイテムが選択された時に呼び出されます
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        int itemId = item.getItemId();

        if (itemId == R.id.item1) {
            view.new_com();
            ret = true;
        } else if (itemId == R.id.item2) {
            view.new_man();
            ret = true;
        } else {
            ret = super.onOptionsItemSelected(item);
        }
        return ret;
    }
}
