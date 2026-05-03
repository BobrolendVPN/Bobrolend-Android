package org.bobrolend.lib.v2ray.services;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.bobrolend.lib.v2ray.R;
import org.bobrolend.lib.v2ray.interfaces.TrafficListener;
import org.bobrolend.lib.v2ray.utils.Utilities;

public class NotificationService {
    private NotificationManager mNotificationManager = null;
    private NotificationCompat.Builder notifcationBuilder;
    public boolean isNotificationOnGoing;
    private final int NOTIFICATION_ID = 1;

    public TrafficListener trafficListener = new TrafficListener() {
        @Override
        public void onTrafficChanged(long uploadSpeed, long downloadSpeed, long uploadedTraffic, long downloadedTraffic) {
            if (mNotificationManager != null && notifcationBuilder != null) {
                if (isNotificationOnGoing) {
                    notifcationBuilder.setSubText("Traffic ↓" + Utilities.parseTraffic(downloadedTraffic, false, false) + "  ↑" + Utilities.parseTraffic(uploadedTraffic, false, false));
                    notifcationBuilder.setContentText("Tap to open application.\n Download : ↓" + Utilities.parseTraffic(downloadSpeed, false, true) + " | Upload : ↑" + Utilities.parseTraffic(uploadSpeed, false, true));
                    mNotificationManager.notify(NOTIFICATION_ID, notifcationBuilder.build());
                }
            }
        }
    };

    public NotificationService(final Service service) {
        String channelId = "BOBROLEND_VPN";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Bobrolend VPN",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(service, channelId)
                .setContentTitle("Bobrolend VPN")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            service.startForeground(1, notification);
        }
    }

    public void setConnectedNotification(String remark, int iconResource) {
        if (mNotificationManager != null && notifcationBuilder != null) {
            if (isNotificationOnGoing) {
                notifcationBuilder.setSmallIcon(iconResource);
                notifcationBuilder.setContentTitle(remark);
                notifcationBuilder.setContentText("");
                mNotificationManager.notify(NOTIFICATION_ID, notifcationBuilder.build());
            }
        }
    }

    public void dismissNotification() {
        if (mNotificationManager != null) {
            isNotificationOnGoing = false;
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }
}