## Android app for ECG data collection
This is the source code for "	A three-dimensional liquid diode for soft integrated permeable electronics" to be published on *Nature*.

## 1. Android app code
### 1.1. System requirement
* Android >= 10.0
* Bluetooth >= 4.0

### 2. Installation
Go to ./app/release, download the compiled APK file to an android phone and open it to install.

### 3. Compiler Environment
* Android Studio Dolphin 2021.3.1

### 4. Dependencies
* MPAndroidChart: v3.1.0
* appcompat: 1.3.1
* baseble: 2.0.5
* constraintlayout: 2.0.4
* espresso-core: 3.4.0
* junit: 1.1.3
* material: 1.4.0

### 5. Test
This app has tested on
* Redmi Note 9T, MIUI Global 12.5.6(Android 11)
* Xiaomi Mix 2s, MIUI 12.5.1(Android 10)

### 6. Instructions for running
* After the app installed on an Android device, open up the bluetooth service of the device, and power on ECG system. Then use the botton at top right to connect the Bluetooth device.
* The chart view shows the real-time ECG signal, and will start right after device connected.


## Cite
If you find this useful, cite this paper below
```
Zhang, B., Li, J., Zhou, J. et al. A three-dimensional liquid diode for soft integrated permeable electronics. Nature (2024).
```
Thank you !!
