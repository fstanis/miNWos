# miNWos

**DISCLAIMER:** This is not an official Google product.

miNWos is a simple Android app that can be used to list all networks your device
is connected to and display useful information about them, such as meteredness.

## Usage

When launched, miNWos displays all the networks currently available to your
device.

`NOT_METERED` and `TEMPORARILY_NOT_METERED` are flags that indicate the
network's current capabilities. For more information, see
[`NetworkCapabilities` in the Android documentation](https://developer.android.com/reference/android/net/NetworkCapabilities).

The default (active) network is always at the top and highlighted yellow. On
some devices, only the default network can be displayed.

miNWos can also display detailed information about the cellular network and your
SIM subscriptions. To do that, select the "Phone state" tab.
