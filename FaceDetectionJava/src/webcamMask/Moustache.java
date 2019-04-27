package webcamMask;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class Moustache extends Sticker{

	public Moustache(Mat sticker) {
		super(sticker);
	}

	public void initPos(Rect nose, int ox, int oy, int roi_w, int roi_h) { //origin of nose relative to the entire frame
		double brX = nose.br().x;
		double brY = nose.br().y;
		double nw = nose.width;
		double mw = nw*2;
		double mh = mw/this.getSize().width*this.getSize().height;
		
		//Centralize
		double x1=brX-nw-mw/10;
		double x2=brX+mw/10;
		double y1=brY-mh/4;
		double y2=brY+mh/4;
		
		//recalculate size
		mw=x2-x1;
		mh=y2-y1;
		System.out.println("Initial moustache has w: "+ mw+"and h: " +mh);
		this.resize((int)mw, (int)mh);
		this.setTopLeftPos((int)x1+ox,(int)y1+oy);
	}
}
