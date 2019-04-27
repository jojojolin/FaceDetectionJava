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
import org.opencv.core.Point;
import org.opencv.core.Rect;
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
	
	// check-boxes for enabling/disabling classifier
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
	
	// face cascade classifier
	private CascadeClassifier faceCascade;
	private CascadeClassifier noseCascade;
	private int absoluteFaceSize;
	private Mat moustache;
	private Mat prevFrame;
	private MatOfPoint2f prevCorners2f;
	private Moustache mos;
	
	
	
	
	/**
	 * Initialize the controller, at start time
	 */
	protected void init(){
		
		this.capture=new VideoCapture();
		//Load cascades
		this.faceCascade= new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt2.xml");
		this.noseCascade=new CascadeClassifier("data/otherCascades/haarcascade_mcs_nose.xml");
		
		this.absoluteFaceSize=0;
		//Load with all 4 channels
		this.moustache=Imgcodecs.imread("data/whitemoustache.png",-1);
		this.mos = new Moustache(moustache);
		
		// Set a fixed width for the frame
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
		System.out.println("start Button is pressed!Now triggers startCamera in the controller");
		if (!this.cameraActive)
		{	
			System.out.println("Ready to activate the camera:"+this.capture.isOpened());
			// start the video capture
			this.capture.open(0);
			System.out.println("Opened the capture.");
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						Mat frame = grabFrame();
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				// Grab a frame every 33 ms (30 frames/sec)
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// Update the button content
				this.button.setText("Stop Camera");
			}
			else
			{
				// Log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// Camera is not active at this point
			this.cameraActive = false;
			// Update again the button content
			this.button.setText("Start Camera");
			// Stop the timer
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
		
		//Convert to gray scale to decrease noise level
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		//Equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);//This is important! without this the moustache keeps resizing
		
		//Compute minimum face size (20% of the frame height, in our case)
		if(this.absoluteFaceSize==0)
		{
			int height = grayFrame.rows();
			
			if (Math.round(height*0.2f)>0){
				this.absoluteFaceSize = Math.round(height*0.2f);
			}
		}
		
		/*** Major change:
		 * 		Recognition (Face + nose) get executed in the very first initialization run/frame only.
		 * 		Enough information is stored to enable tracking in the rest of the frames
		 * 		This is done to reduce time complexity and achieve real-time streaming
		 * ***/
		if(prevFrame == null){
			//Detect faces
			this.faceCascade.detectMultiScale(grayFrame,faces,1.1,2,0|Objdetect.CASCADE_SCALE_IMAGE, 
					new Size(this.absoluteFaceSize,this.absoluteFaceSize), new Size());
			for (Rect rect: faces.toArray())
			{
				int ox = rect.x;
				int oy = rect.y;
				
				Runnable trackpoints = new Runnable(){
					@Override
					public void run()
					{
						System.out.println("Running trackpoints~");
						//Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0,255,0), 3);
						Mat roi_gray=grayFrame.submat(rect);
						//Mat roi_color=frame.submat(rect);
						prevFrame=roi_gray;
						MatOfPoint prevCorners=new MatOfPoint();
						//The more points we track the longer tracking lasts
						Imgproc.goodFeaturesToTrack(prevFrame,prevCorners,200,0.01, 10);
						Point[] prevpt = prevCorners.toArray();
						for(Point p : prevpt){
							p.x+=ox;
							p.y+=oy;
						}
						prevCorners2f=new MatOfPoint2f(prevpt);
						prevFrame=grayFrame;
					}
				};
				
				Runnable noseDetection = new Runnable(){
					@Override
					public void run(){
						System.out.println("Running noseDetection~");
						Mat roi_gray=grayFrame.submat(rect);
						//For storing detected nose later
						MatOfRect nose= new MatOfRect();
						//Detect nose
						noseCascade.detectMultiScale(roi_gray,nose);
						
						for(Rect noseOne:nose.toArray()){
							System.out.println("nose detected!");
							//Localize sticker
							mos.initPos(noseOne, ox, oy,roi_gray.width(),roi_gray.height());
							//Append sticker to frame/ Image processing
							appendSticker(frame,mos);
							break;//one nose per face!
						}
					}
				};
				//Create 2 threads
				Thread t1 = new Thread(trackpoints);
				Thread t2 = new Thread(noseDetection);
				//Fire them!
				t1.start();
				t2.start();
				//Make sure they both terminated properly in this initial run
				try
		        { 
		            t1.join(); 
		            t2.join(); 
		        } 
		        catch(Exception e) 
		        { 
		            System.out.println("Interrupted"); 
		        } 
				
				break;
			}
		}
		else{
			Mat nextFrame=grayFrame;
			if(nextFrame.size()!=prevFrame.size())
				Imgproc.resize(prevFrame, prevFrame, nextFrame.size());
			MatOfPoint2f nextCorners2f=new MatOfPoint2f();
			MatOfFloat err=new MatOfFloat();
			MatOfByte status=new MatOfByte();
			Video.calcOpticalFlowPyrLK(prevFrame, nextFrame, prevCorners2f, nextCorners2f,status, err);
			
			//Consider doing a rescan in the future - if the no. of points with status 1 is below perhaps 50
			int counter = 0;
			double sum_x = 0;
			double sum_y = 0;
			int size = status.toArray().length;
			Point[] prevpt = prevCorners2f.toArray();
			Point[] currentpt= nextCorners2f.toArray();
			for(int i=0; i<size;i++){
				if(status.toArray()[i]==1){
					counter++;
					sum_x+=currentpt[i].x-prevpt[i].x;
					sum_y+=currentpt[i].y-prevpt[i].y;
					/**Uncomment the line below to test for tracking**/
					//Imgproc.circle(frame,prevpt[i], 3, new Scalar(0,255,0));
				}
					
			}
			sum_x/=size;
			sum_y/=size;
			mos.move(sum_x, sum_y);
			appendSticker(frame,mos);
			System.out.println("Counter: "+counter+", dist moved x = "+sum_x+", dist moved y = "+sum_y);
			//Update
			prevFrame=nextFrame;
			prevCorners2f=nextCorners2f;
		}
	}
	
	
	private void appendSticker(Mat frame, Sticker sticker){ //Parameters are copies of references
		//Split resized sticker into to 4 channels
		List<Mat> rgba=new ArrayList<Mat>();
		System.out.println("Inside appendSticker, check getMat() "+sticker.getMat());
		Core.split(sticker.getMat(), rgba);
		
		//Get alpha - transparency - layer and create masks
		Mat mask=rgba.get(3);
		Mat mask_inv=new Mat();
		Core.bitwise_not(mask, mask_inv);
		rgba.remove(3);
		Mat rgbsticker=new Mat();
		Core.merge(rgba, rgbsticker);
		
		Mat fg=new Mat();
		Core.bitwise_and(rgbsticker, rgbsticker, fg, mask);
		Mat bg=new Mat();
		Point topLeftSticker= sticker.getTopLeftPos();
		System.out.println("Top left sticker is :"+topLeftSticker.x+", "+topLeftSticker.y);
		Size sz= rgbsticker.size();
		Mat roi=frame.submat(new Rect((int)topLeftSticker.x,(int)topLeftSticker.y,(int)sz.width,(int)sz.height));
		Core.bitwise_and(roi, roi, bg, mask_inv);
		Core.add(fg, bg, roi);
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
