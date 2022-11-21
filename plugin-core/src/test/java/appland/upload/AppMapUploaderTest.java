package appland.upload;

import appland.AppMapBaseTest;
import appland.settings.AppMapProjectSettingsService;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

public class AppMapUploaderTest extends AppMapBaseTest {
    @Rule
    public final WireMockRule serverRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @Test
    public void appMapUpload() throws InterruptedException {
        serverRule.stubFor(post("/api/appmaps/create_upload")
                .willReturn(ok("{\"id\":248,\"token\":\"d70c362d-7bd3-4363-bb19-f8d88f6a3496\"}")));

        var appmapFile = myFixture.configureByText("sample.appmap.json", "{\"version\": \"1.5.1\"}");

        var projectSettings = AppMapProjectSettingsService.getState(getProject());
        var oldConfirmUpload = projectSettings.isConfirmAppMapUpload();
        try {
            projectSettings.setCloudServerUrl(serverRule.baseUrl());
            projectSettings.setConfirmAppMapUpload(false);

            var latch = new CountDownLatch(1);
            var result = new AtomicReference<String>();
            AppMapUploader.uploadAppMap(getProject(), appmapFile.getVirtualFile(), newValue -> {
                latch.countDown();
                result.set(newValue);
            });

            var ok = latch.await(1000, TimeUnit.MILLISECONDS);
            assertTrue("Upload response must not timeout", ok);

            var expectedURL = String.format("%s/scenario_uploads/%d?token=%s",
                    serverRule.baseUrl(), 248, "d70c362d-7bd3-4363-bb19-f8d88f6a3496");
            assertEquals(expectedURL, result.get());
        } finally {
            projectSettings.setCloudServerUrl(null);
            projectSettings.setConfirmAppMapUpload(oldConfirmUpload);
        }
    }
}