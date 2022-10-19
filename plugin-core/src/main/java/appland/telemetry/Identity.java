package appland.telemetry;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.intellij.ide.util.PropertiesComponent;

public class Identity {
  @NotNull private final static String MachineIdKey = "appland.machine_id";
  @NotNull private final static Set<String> IgnoredMacAddresses =
      Set.of("00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "ac:de:48:00:11:22");

  protected static String issueMachineId() {
    // Loop through all the network interfaces and return the first non-loopback
    // interface that has a hardware address.
    String machineId = null;
    try {
      var networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        var networkInterface = networkInterfaces.nextElement();
        var address = networkInterface.getHardwareAddress();
        if (address == null || address.length == 0) {
          continue;
        }

        var buffer = ByteBuffer.wrap(address);
        String mac = Stream
          .generate(() -> String.format("%02x", buffer.get()))
          .limit(buffer.capacity())
          .collect(Collectors.joining(":"));
        if (IgnoredMacAddresses.contains(mac)) {
          continue;
        }

        machineId = mac;
        break;
      }
    } catch (SocketException e) {
      // ignore, we'll generate a random ID as a fallback below
    }

    if (machineId == null) {
      machineId = UUID.randomUUID().toString();
    }

    String machineIdDigest = null;
    try {
      var alg = MessageDigest.getInstance("SHA-256");
      var hash = ByteBuffer.wrap(alg.digest(machineId.getBytes()));

      machineIdDigest = Stream
        .generate(() -> String.format("%02x", hash.get()))
        .limit(hash.capacity())
        .collect(Collectors.joining());
    } catch (NoSuchAlgorithmException e) {
      return "";
    }

    PropertiesComponent.getInstance().setValue(MachineIdKey, machineIdDigest);
    return machineIdDigest;
  }

  public static String getMachineId() {
    var machineId = PropertiesComponent.getInstance().getValue(MachineIdKey);
    if (machineId == null || machineId.isEmpty()) {
      machineId = issueMachineId();
    }
    return machineId;
  }
}
