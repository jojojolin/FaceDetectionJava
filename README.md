# FaceDetectionJava

This repository contains source code of a Computer vision program in Java that can run on any OS that has a JVM.

## How it works
This program consists of 5 parts:
  1. Input - Read the streaming video from the webcam at the rate 33ms/frame.
  2. Image Processing - Detect frace(s) from the frame and append sticker(s) at the calculated location(s)
  3. Find feature points - Find good feature points to track from the roi (the detected face)
  4. Tracking - track the faces and move stickers accordingly based on the average distance moved by the valid featrue points
  5. Output - Display the processed frame to the interface (FXML)


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

### Run the program
Select the project folder and click on Run As > 2 Java Application > FXHelloCV
  
## APIs used
* OpenCV4.1.0
* FXML
