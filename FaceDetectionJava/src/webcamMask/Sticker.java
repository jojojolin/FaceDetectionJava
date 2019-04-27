package webcamMask;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Sticker {
	private Mat sticker;
	private Size size;
	private Point topLeft;
	
	public Sticker(Mat sticker){
		this.sticker=sticker;
		this.size = this.sticker.size();
		this.topLeft = new Point();
	}
	
	
	public void resize(int w, int h){
		size = new Size(w,h);
		Imgproc.resize(sticker, sticker, size);
	}
	
	public void move(double distx, double disty){
		topLeft.x+=distx;
		topLeft.y+=disty;
	}
	
	public Point getTopLeftPos(){
		return topLeft;
	}
	
	public Mat getMat(){
		return sticker;
	}
	public Size getSize(){
		return size;
	}
	public void setTopLeftPos(int x, int y){
		this.topLeft.x=x;
		this.topLeft.y=y;
	}
}
