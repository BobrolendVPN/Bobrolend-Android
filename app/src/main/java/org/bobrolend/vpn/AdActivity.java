package org.bobrolend.vpn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Полноэкранный баннер, который показывается сразу после подключения VPN.
 * Первые AD_DURATION_MS кнопка "назад" заблокирована, кнопки закрытия нет.
 * По истечении времени появляется кнопка закрытия и разблокируется "назад".
 *
 * ВАЖНО: системные Home / Recent Apps заблокировать нельзя — это ограничение
 * платформы Android для обычных приложений (не kiosk/device-owner режим).
 * Перехватывается только внутриприложенческая навигация "назад".
 */
public class AdActivity extends AppCompatActivity {
    private static final long AD_DURATION_MS = 5000; // минимум 5 секунд показа

    private ImageView bannerImage;
    private TextView countdownLabel;
    private ImageButton closeButton;

    private CountDownTimer countDownTimer;
    private OnBackPressedCallback blockBackCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        bannerImage = findViewById(R.id.banner_image);
        countdownLabel = findViewById(R.id.countdown_label);
        closeButton = findViewById(R.id.btn_close);

        closeButton.setOnClickListener(v -> finish());

        // Блокируем аппаратную/жестовую кнопку "назад" на время показа рекламы
        blockBackCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // намеренно ничего не делаем — просто поглощаем нажатие
            }
        };
        getOnBackPressedDispatcher().addCallback(this, blockBackCallback);

        loadBanner();
    }

    private void loadBanner() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url(BuildConfig.adApiUrl + "/banner/get")
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Не удалось получить баннер — не держим пользователя без причины
                runOnUiThread(AdActivity.this::finish);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(AdActivity.this::finish);
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String imageUrl = json.getString("url");
                    downloadImage(imageUrl);
                }
                catch (Exception e) {
                    runOnUiThread(AdActivity.this::finish);
                }
            }
        });
    }

    private void downloadImage(String imageUrl) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(imageUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(AdActivity.this::finish);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(AdActivity.this::finish);
                    return;
                }

                byte[] bytes = response.body().bytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                runOnUiThread(() -> {
                    if (bitmap == null) {
                        finish();
                        return;
                    }
                    bannerImage.setImageBitmap(bitmap);
                    startCountdown();
                });
            }
        });
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(AD_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = (millisUntilFinished / 1000) + 1;
                countdownLabel.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                countdownLabel.setVisibility(View.GONE);
                closeButton.setVisibility(View.VISIBLE);
                blockBackCallback.setEnabled(false); // теперь "назад" тоже закрывает баннер
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
