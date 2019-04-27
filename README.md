# FaceDetectionJava

This repo contains source code to the Java program that runs on any os with JVM.

## Prerequisite
* A webcam is required to run the program.
* OpenCV > v3.0.0

## Setting up in Eclipse workspace
This project can be imported directly into the Eclipse worksapce

### Import project
File > Import > Git > Projects from Git > Clone URI > copy & paste the URI > put a destination dir > Import as a general project > Decide a name > Finish

### Make it a Java project
In the project folder, edit .project file by adding '''org.eclipse.jdt.core.javanature''' under 'natures > nature'

### Add OpenCV library
Eclipse > Preferences > Java > Build Path > User Libraries > New > Name it (e.g. opencv-4.1.0) > Click on the new lib > Add External JARs > browse to your JAR dir > Open > Edit Native library location > External Folder > the dir that contains JAR and dylib > OK

Right click on the project folder > Build Path > Add Libraries > User Library > opencv-XXX > Finish

## How it works
This program works like this:
  1. Input - Read one frame of the streaming video from the webcam every 33ms
  2. Image Processing - Detect frace(s) in the very first frame and append sticker(s) at the calculated location(s)
  3. Output - Display the processed frame to the interface (FXML)
  
## APIs used
* OpenCV4.1.0
* FXML
