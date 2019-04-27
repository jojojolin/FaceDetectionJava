# FaceDetectionJava

This repo contains source code to the Java program that runs on any os with JVM.

## Prerequisite
A webcam is required to run the program.

## How it works
This program works like this:
  1. Input - Read one frame of the streaming video from the webcam every 33ms
  2. Image Processing - Detect frace(s) in the very first frame and append sticker(s) at the calculated location(s)
  3. Output - Display the processed frame to the interface (FXML)
  
## APIs used
* OpenCV4.1.0
* FXML
