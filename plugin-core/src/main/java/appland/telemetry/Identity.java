package appland.telemetry;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Identity {
    private static final @NotNull String MachineIdKey = "appland.machine_id";
    private static final @NotNull Set<String> IgnoredMacAddresses = Set.of("00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "ac:de:48:00:11:22");

    /**
     * Retrieves a cached machine id from the application properties.
     * If there's no value available, then it calculates a new machine id and stores it in the properties.
     */
    public static @Nullable String getOrCreateMachineId() {
        var machineId = PropertiesComponent.getInstance().getValue(MachineIdKey);
        if (StringUtil.isEmpty(machineId)) {
            machineId = issueMachineId();
            if (StringUtil.isNotEmpty(machineId)) {
                PropertiesComponent.getInstance().setValue(MachineIdKey, machineId);
            }
        }
        return machineId;
    }

    private static @Nullable String issueMachineId() {
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

                var mac = createHexByteStream(address).collect(Collectors.joining(":"));
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

        try {
            var hashBytes = MessageDigest.getInstance("SHA-256").digest(machineId.getBytes());
            return createHexByteStream(hashBytes).collect(Collectors.joining());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * @param bytes Data to return as hex byte strings
     * @return Stream of string hex byte representations of the original bytes array
     */
    static @NotNull Stream<String> createHexByteStream(byte[] bytes) {
        var buffer = ByteBuffer.wrap(bytes);
        return Stream.generate(() -> String.format("%02x", buffer.get())).limit(buffer.capacity());
    }
}
