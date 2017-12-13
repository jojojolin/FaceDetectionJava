import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetector {
	public void run(){
		/*CascadeClassifier faceDetector =new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt2.xml");
		Mat image =Imgcodecs.imread("data/trump.jpeg");
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);
		System.out.println(String.format("Detected %s faces",
				faceDetections.toArray().length));
		for (Rect rect : faceDetections.toArray()) {
			Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x
					+ rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
		}
		String filename = "result/trump.png";
		System.out.println(String.format("Writing %s", filename));
		Imgcodecs.imwrite(filename, image);*/
		Mat image= Imgcodecs.imread("data/SimilarFaceSearching.png");
		Mat moustache=Imgcodecs.imread("data/moustache.png",-1);
		
		CascadeClassifier faceCascade=new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt2.xml");
		CascadeClassifier noseCascade= new CascadeClassifier("data/otherCascades/haarcascade_mcs_nose.xml");
		
		Mat grayImage=new Mat();
		//convert the frame in gray  scale
		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
		//equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayImage, grayImage);
		
		//MatOfRect noses=new MatOfRect();
		MatOfRect faces= new MatOfRect();
		faceCascade.detectMultiScale(grayImage, faces);
		
		for(Rect face: faces.toArray())
		{
			System.out.println("face found!");
			Mat roi_color=image.submat(face);
			Mat roi_gray=grayImage.submat(face);
			
			MatOfRect noses=new MatOfRect();
			noseCascade.detectMultiScale(roi_gray, noses);
			
			for(Rect noseOne: noses.toArray()){
				System.out.println("nose found!");
				double brX=noseOne.br().x;
				double brY=noseOne.br().y;
				
				double nw=noseOne.width;
				double mw=nw*3;
				double mh=mw/moustache.width()*moustache.height();
				
				//centralise moustache
				double x1=brX-nw-mw/4;
				double x2=brX+mw/4;
				double y1=brY-mh/4;
				double y2=brY+mh/4;
				
				//recalculate size of moustache
				mw=x2-x1;
				mh=y2-y1;
				
				Size sz=new Size((int)mw,(int)mh);
				Mat resizedMoustache=new Mat();
				//resize moustache
				Imgproc.resize(moustache, resizedMoustache, sz);
				
				//split to 4 channels
				List<Mat> rgba=new ArrayList<Mat>();
				Core.split(resizedMoustache, rgba);
				
				//get alpha layer and create masks
				Mat mask=rgba.get(3);
				Mat mask_inv=new Mat();
				Core.bitwise_not(mask, mask_inv);
				rgba.remove(3);
				Mat rgbMoustache=new Mat();
				Core.merge(rgba, rgbMoustache);
				
				Mat fg=new Mat();
				Core.bitwise_and(rgbMoustache, rgbMoustache, fg, mask);
				Mat bg=new Mat();
				Mat roi=roi_color.submat(new Rect((int)x1,(int)y1,(int)mw,(int)mh));
				Core.bitwise_and(roi, roi, bg, mask_inv);
				Core.add(fg, bg, roi);
			}
		}
		String fileName="result/faceswithMous.jpeg";
		System.out.println(String.format("Writing %s", fileName));
		Imgcodecs.imwrite(fileName, image);
	}
}
