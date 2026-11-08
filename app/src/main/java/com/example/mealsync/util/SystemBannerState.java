package com.example.mealsync.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SystemBannerState {

    public enum Banner {
        OFFLINE,
        AIRPLANE_MODE
    }

    private SystemBannerState() {
    }

    public static List<Banner> visibleBanners(boolean offline, boolean airplaneMode) {
        List<Banner> banners = new ArrayList<>(2);
        if (offline) {
            banners.add(Banner.OFFLINE);
        }
        if (airplaneMode) {
            banners.add(Banner.AIRPLANE_MODE);
        }
        return Collections.unmodifiableList(banners);
    }
}
