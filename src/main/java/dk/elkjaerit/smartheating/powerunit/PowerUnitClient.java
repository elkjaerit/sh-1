package dk.elkjaerit.smartheating.powerunit;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudiot.v1.CloudIot;
import com.google.api.services.cloudiot.v1.CloudIotScopes;
import com.google.api.services.cloudiot.v1.model.ModifyCloudToDeviceConfigRequest;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceRequest;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import dk.elkjaerit.smartheating.ProjectInfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class PowerUnitClient {

  private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
  private static GoogleCredentials credential;

  private static final CloudIot service;

  static {
    try {
      credential = GoogleCredentials.getApplicationDefault().createScoped(CloudIotScopes.all());
      service = getCloudIot();
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException("Could no start", e);
    }
  }

  public static void sendCommand(String deviceId, String data) throws IOException {
    final String devicePath =
        String.format(
            "projects/%s/locations/%s/registries/%s/devices/%s",
            ProjectInfo.PROJECT_ID, ProjectInfo.CLOUD_REGION, ProjectInfo.REGISTRY_NAME, deviceId);

    SendCommandToDeviceRequest req = buildRequest(data);

    SendCommandToDeviceResponse execute =
        service
            .projects()
            .locations()
            .registries()
            .devices()
            .sendCommandToDevice(devicePath, req)
            .execute();
  }

  public static void sendConfiguration(String deviceId, String data)
      throws GeneralSecurityException, IOException {
    final CloudIot service = getCloudIot();

    final String devicePath =
        String.format(
            "projects/%s/locations/%s/registries/%s/devices/%s",
            ProjectInfo.PROJECT_ID, ProjectInfo.CLOUD_REGION, ProjectInfo.REGISTRY_NAME, deviceId);

    ModifyCloudToDeviceConfigRequest req = new ModifyCloudToDeviceConfigRequest();
    Base64.Encoder encoder = Base64.getEncoder();
    String encPayload = encoder.encodeToString(data.getBytes(StandardCharsets.UTF_8.name()));
    req.setBinaryData(encPayload);

    service
        .projects()
        .locations()
        .registries()
        .devices()
        .modifyCloudToDeviceConfig(devicePath, req)
        .execute();
  }

  private static SendCommandToDeviceRequest buildRequest(String data)
      throws UnsupportedEncodingException {
    SendCommandToDeviceRequest req = new SendCommandToDeviceRequest();

    // Data sent through the wire has to be base64 encoded.
    Base64.Encoder encoder = Base64.getEncoder();
    String encPayload = encoder.encodeToString(data.getBytes(StandardCharsets.UTF_8.name()));
    req.setBinaryData(encPayload);
    return req;
  }

  private static CloudIot getCloudIot() throws GeneralSecurityException, IOException {
    HttpRequestInitializer init = new HttpCredentialsAdapter(credential);
    return new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
        .setApplicationName("smart-heating")
        .build();
  }
}
