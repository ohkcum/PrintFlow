# PrintFlowLite Android Print Service

> This is a reference package indicating that the Android Print Service is maintained in a **separate repository**.

## About the Android App

PrintFlowLite supports Android printing through the **Android Print Service** API, which allows apps to act as print destinations on Android devices.

The Android Print Service app for PrintFlowLite is available at:

**Repository**: [gitlab.com/savapage-android/savapage-android-print](https://gitlab.com/savapage-android/savapage-android-print)

**Package**: `org.printflowlite.android.print`

**License**: GNU AGPL v3

**Distributions**:
- F-Droid: [org.savapage.android.print](https://f-droid.org/packages/org.savapage.android.print/)
- Source: [GitLab](https://gitlab.com/savapage-android/savapage-android-print)

## Features

- **Native Android Print Service** — appears in the Android print dialog alongside other printers
- **Auto-discovery** — discovers PrintFlowLite servers on the local network via mDNS/Bonjour
- **Manual configuration** — add PrintFlowLite servers by hostname/IP and port
- **TLS support** — secure printing over HTTPS
- **IPP everywhere** — uses the standard IPP protocol

## Building from Source

The Android app requires:

- Android SDK 28+ (Android 9 Pie)
- Android Gradle Plugin 3.5.0
- Java JDK 8+

```bash
# Clone the repository
git clone https://gitlab.com/savapage-android/savapage-android-print.git
cd savapage-android-print

# Build the debug APK
./gradlew assembleDebug

# Build the release APK (requires signing config)
./gradlew assembleRelease
```

## Architecture

The Android Print Service is a separate Android application that:

1. Registers as a print service with the Android system
2. Discovers PrintFlowLite servers on the network via DNS-SD/mDNS
3. Converts print jobs to IPP requests
4. Sends print jobs to the PrintFlowLite IPP server

## Forking for PrintFlowLite

To create a PrintFlowLite-branded Android app:

1. Fork the repository: `https://gitlab.com/savapage-android/savapage-android-print`
2. Replace branding:
   - App name: `savapage-android-print` → `printflowlite-android-print`
   - Package: `org.savapage.android.print` → `org.printflowlite.android.print`
   - App icon and colors
3. Update server discovery URL patterns
4. Build and distribute

## Integration

The Android Print Service communicates with PrintFlowLite via standard IPP:

- **Discovery**: DNS-SD service type `_ipp._tcp` on local network
- **Printing**: IPP POST to `https://server:8632/ipp/print`
- **Authentication**: HTTP Basic Auth or session cookie

## Documentation

- [Android Printing - SavaPage Manual](https://www.savapage.org/docs/manual/ch-printer-android.html)
- [Android Print App - SavaPage Manual](https://www.savapage.org/docs/manual/ch-printer.html#ch-printer-android)

## References

- [PrintFlowLite Documentation](../docs/)
- [SavaPage Android Print Service](https://wiki.savapage.org/) (community wiki)
