import org.opencv.core.Core;

public class Test {
	
	public static void main(String[] args) throws InterruptedException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		new FaceDetector().run();
	}
}
