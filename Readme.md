# Mediapipe Landmarker

This Android application uses Google's [Mediapipe](https://ai.google.dev/edge/mediapipe/solutions/guide) library to show hand landmarks. The project is implemented in Java, as there is no official Java code available for this functionality; all examples are in Kotlin. This repository provides a solution for developers who prefer working with Java for Android development.

<img src="demo.gif" alt="Demo GIF" width="250"/>

## Features
 - Detects and visualizes hand landmarks in real time using the front camera.
 - Built entirely in Java, making it accessible for developers who prefer Java over Kotlin.
 - Provides a starting point for integrating Mediapipe into Android apps with Java.

## Requirements
 - Android Studio
 - Android device or emulator with a camera
 - Mediapipe library dependencies for Android
 - JDK 8 or higher

## Getting Started
Follow these steps to build and run the application:

### 1. Clone the repository
```bash
git clone https://github.com/tanujn45/MediapipeHandmarkerJava.git
```

### 2. Open the project in Android Studio
 - Open Android Studio and choose "Open an existing project."
 - Navigate to the cloned repository and select it.

### 3. Set up the Mediapipe dependencies
 - Open the `build.gradle` file and ensure that the required dependencies for Mediapipe are added under `dependencies`:

 ```gradle
 dependencies {
    implementation 'com.google.mediapipe:mediapipe-android:0.8.10'
}
```
 - Sync the project with Gradle.

### 4. Configure camera permissions
Make sure that you have the necessary camera permissions in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-feature android:name="android.hardware.camera" />
```

### 5. Build and Run the App
Connect your Android device via USB or use an emulator with camera support.
Press Run in Android Studio to install and launch the app on your device.

### 6. View Hand Landmarks
Once the app is running, you should be able to see hand landmarks displayed over your hand through the camera in real time.

## Code Explanation
The application uses Mediapipe’s Hand Tracking solution to detect hand landmarks. The core logic is implemented in Java, and here’s an overview of how it works:

 - **Camera Setup**: Uses Android's `CameraX API` to access the device camera.
 - **Mediapipe Integration**: Sets up the Mediapipe hand tracking pipeline to process the camera feed and extract hand landmarks.
 - **Landmark Visualization**: Uses `Canvas` to draw the detected hand landmarks on the screen.

## Contributing
Feel free to fork the repository, open issues, or submit pull requests. If you have any suggestions for improvements or new features, don't hesitate to contribute!
