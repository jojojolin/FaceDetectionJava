package webcamMask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import webcamUtils.Utils;

public class Controller
{
	// the FXML button
	@FXML
	private Button button;
	// the FXML area for showing the current frame
	@FXML
	private ImageView currentFrame;
	
	// checkboxes for enabling/disabling classifier
	@FXML
	private CheckBox haar;
	@FXML
	private CheckBox lbp;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	// the id of the camera to be used
	private static int cameraId = 0;
	
	// face cascade classifier
	private CascadeClassifier faceCascade;
	private CascadeClassifier noseCascade;
	private int absoluteFaceSize;
	private Mat moustache;
	private Mat prevFrame;
	private MatOfPoint2f prevCorners2f;
	
	
	
	
	/**
	 * Initialize the controller, at start time
	 */
	protected void init(){
		
		this.capture=new VideoCapture();
		this.faceCascade= new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt2.xml");
		this.noseCascade=new CascadeClassifier("data/otherCascades/haarcascade_mcs_nose.xml");
		this.absoluteFaceSize=0;
		//load moustache with all 4 channels
		this.moustache=Imgcodecs.imread("data/whitemoustache.png",-1);
		
		// set a fixed width for the frame
		currentFrame.setFitWidth(600);
		currentFrame.setPreserveRatio(true);
	}
	
	
	/**
	 * The action triggered by pushing the button on the GUI
	 *
	 * @param event
	 *            the push button event
	 */
	@FXML
	protected void startCamera(ActionEvent event)
	{
		if (!this.cameraActive)
		{	
			// start the video capture
			this.capture.open(cameraId);
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				this.button.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.button.setText("Start Camera");
			// stop the timer
			this.stopAcquisition();
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame()
	{
		// init everything 
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					this.detectAndDisplay(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	
	/**
	 * Method for face detection and tracking
	 * 
	 * @param frame
	 * it looks for faces in this frame
	 */
	private void detectAndDisplay(Mat frame){
		
		MatOfRect faces= new MatOfRect();
		Mat grayFrame= new Mat();
		
		//convert the frame in gray  scale
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		//equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);
		
		//compute minimum face size (20% of the frame height, in our case)
		if(this.absoluteFaceSize==0)
		{
			int height = grayFrame.rows();
			
			if (Math.round(height*0.2f)>0){
				this.absoluteFaceSize = Math.round(height*0.2f);
			}
		}
		
		//detect faces
		this.faceCascade.detectMultiScale(grayFrame,faces,1.1,2,0|Objdetect.CASCADE_SCALE_IMAGE, 
				new Size(this.absoluteFaceSize,this.absoluteFaceSize), new Size());
		
		for (Rect rect: faces.toArray())
		{
			//Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0,255,0), 3);
			Mat roi_gray=grayFrame.submat(rect);
			Mat roi_color=frame.submat(rect);
			/*
			if(prevFrame==null)
			{
				prevFrame=roi_gray;
				MatOfPoint prevCorners=new MatOfPoint();
				Imgproc.goodFeaturesToTrack(prevFrame,prevCorners,200,0.01, 10);
				prevCorners2f=new MatOfPoint2f(prevCorners.toArray());
				prevFrame=grayFrame;
			}
			else
			{
				Mat nextFrame=grayFrame;
				if(nextFrame.size()!=prevFrame.size())
					Imgproc.resize(prevFrame, prevFrame, nextFrame.size());
				MatOfPoint nextCorners=new MatOfPoint();
				Imgproc.goodFeaturesToTrack(nextFrame,nextCorners,200,0.01,10);
				MatOfPoint2f nextCorners2f=new MatOfPoint2f(nextCorners.toArray());
				MatOfFloat err=new MatOfFloat();
				MatOfByte status=new MatOfByte();
				Video.calcOpticalFlowPyrLK(prevFrame, nextFrame, prevCorners2f, nextCorners2f,status, err);
				for(int i=0; i<status.toArray().length;i++){
					if(status.toArray()[i]==1)
						//System.out.println(".");
						Imgproc.circle(roi_color,prevCorners2f.toArray()[i], 3, new Scalar(0,255,0));
				}
				//update
				prevFrame=nextFrame;
				prevCorners2f=nextCorners2f;
			}*/
			
			//for storing detected nose later
			MatOfRect nose= new MatOfRect();
			
			//detect noses
			noseCascade.detectMultiScale(roi_gray,nose);
			
			for(Rect noseOne:nose.toArray()){
				double brX=noseOne.br().x;
				double brY=noseOne.br().y;
				
				double nw=noseOne.width;
				double mw=nw*3;
				double mh=mw/moustache.width()*moustache.height();
				
				//centralise moustache
				double x1=brX-nw-mw/10;
				double x2=brX+mw/10;
				double y1=brY-mh/4;
				double y2=brY+mh/4;
				
				//recalculate size of moustache
				mw=x2-x1;
				mh=y2-y1;
				
				//check for clipping
				if (x1<0)
					x1=0;
				if(y1<0)
					y1=0;
				if(x2>roi_color.width())
					x2=roi_color.width();
				if(y2>roi_color.height())
					y2=roi_color.height(); 
				
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
				break;
			}
		}
	}

	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	
	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	
}
