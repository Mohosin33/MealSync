package com.example.mealsync.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SystemBannerStateTest {

    @Test
    public void noSystemIssuesShowsNoBanners() {
        assertEquals(Collections.emptyList(),
                SystemBannerState.visibleBanners(false, false));
    }

    @Test
    public void offlineShowsOfflineBanner() {
        assertEquals(Collections.singletonList(SystemBannerState.Banner.OFFLINE),
                SystemBannerState.visibleBanners(true, false));
    }

    @Test
    public void airplaneModeShowsAirplaneBanner() {
        assertEquals(Collections.singletonList(SystemBannerState.Banner.AIRPLANE_MODE),
                SystemBannerState.visibleBanners(false, true));
    }

    @Test
    public void offlineAndAirplaneModeShowBothInOrder() {
        assertEquals(Arrays.asList(
                        SystemBannerState.Banner.OFFLINE,
                        SystemBannerState.Banner.AIRPLANE_MODE),
                SystemBannerState.visibleBanners(true, true));
    }
}
