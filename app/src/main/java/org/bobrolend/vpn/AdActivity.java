package org.bobrolend.vpn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

public class AdActivity extends AppCompatActivity {

    private static final long AD_DURATION_MS = 5000L;

    private ImageView bannerImage;
    private TextView countdownLabel;
    private ImageButton closeButton;

    private CountDownTimer countDownTimer;
    private OnBackPressedCallback blockBackCallback;

    private final OkHttpClient httpClient = new OkHttpClient();

    private Call bannerCall;
    private Call imageCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        bannerImage = findViewById(R.id.banner_image);
        countdownLabel = findViewById(R.id.countdown_label);
        closeButton = findViewById(R.id.btn_close);

        closeButton.setVisibility(View.GONE);

        closeButton.setOnClickListener(v -> finish());

        blockBackCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {}
        };

        getOnBackPressedDispatcher().addCallback(
            this,
            blockBackCallback
        );

        loadBanner();
    }

    private void loadBanner() {
        String apiUrl = BuildConfig.adApiUrl;

        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            finish();
            return;
        }

        apiUrl = apiUrl.replaceAll("/+$", "") + "/banner/get";

        Request request = new Request.Builder()
            .url(apiUrl)
            .get()
            .build();

        bannerCall = httpClient.newCall(request);

        bannerCall.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> finish());
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response ignored = response) {

                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    if (response.body() == null) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    String responseBody = response.body().string();

                    JSONObject json = new JSONObject(responseBody);

                    if (!json.has("url") || json.isNull("url")) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    String imageUrl = json.getString("url").trim();

                    if (imageUrl.isEmpty()) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    Uri uri = Uri.parse(imageUrl);

                    String scheme = uri.getScheme();

                    if (scheme == null ||
                        (!scheme.equalsIgnoreCase("http")
                            && !scheme.equalsIgnoreCase("https"))) {

                        runOnUiThread(() -> finish());
                        return;
                    }

                    downloadImage(imageUrl);

                }
                catch (Exception e) {
                    runOnUiThread(() -> finish());
                }
            }
        });
    }

    private void downloadImage(String imageUrl) {

        Request request = new Request.Builder()
            .url(imageUrl)
            .get()
            .build();

        imageCall = httpClient.newCall(request);

        imageCall.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> finish());
                }
            }

            @Override
            public void onResponse(Call call, Response response) {

                try (Response ignored = response) {

                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    if (response.body() == null) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    byte[] imageBytes = response.body().bytes();

                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            imageBytes,
                            0,
                            imageBytes.length
                    );

                    if (bitmap == null) {
                        runOnUiThread(() -> finish());
                        return;
                    }

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            bitmap.recycle();
                            return;
                        }

                        bannerImage.setImageBitmap(bitmap);

                        startCountdown();
                    });

                }
                catch (Exception e) {
                    runOnUiThread(() -> finish());
                }
            }
        });
    }

    private void startCountdown() {

        countdownLabel.setVisibility(View.VISIBLE);
        closeButton.setVisibility(View.GONE);

        countDownTimer = new CountDownTimer(
            AD_DURATION_MS,
            1000L
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = (millisUntilFinished + 999L) / 1000L;

                countdownLabel.setText(
                    String.valueOf(secondsLeft)
                );
            }

            @Override
            public void onFinish() {
                countdownLabel.setVisibility(View.GONE);
                closeButton.setVisibility(View.VISIBLE);

                blockBackCallback.setEnabled(false);
            }
        };

        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (bannerCall != null) {
            bannerCall.cancel();
            bannerCall = null;
        }

        if (imageCall != null) {
            imageCall.cancel();
            imageCall = null;
        }

        super.onDestroy();
    }
}