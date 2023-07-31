import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.Color;
import java.awt.Point;

class Functions { 
	Color colorMultiply(Color color, float coeff, boolean reduction) {
		final float multiplier = 1 - coeff;
		if(reduction) {
			return new Color(
				color.getRed() * multiplier / 255f,
				color.getGreen() * multiplier / 255f,
				color.getBlue() * multiplier / 255f
				);
		};
		return new Color(
			color.getRed() * coeff / 255f + multiplier,
			color.getGreen() * coeff / 255f + multiplier,
			color.getBlue() * coeff / 255f + multiplier
			); 
	};

	float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	};

	double distance(double x1, double y1, double x2, double y2) { // calculates euclidean distance using pythagorean theorom
		final double deltaX = Math.abs(x1 - x2);
		final double deltaY = Math.abs(y1 - y2);

		return Math.hypot(deltaX, deltaY);
	};

	double distance(Point p1, Point p2) { // same as above, but taking point inputs
		final double deltaX = Math.abs(p1.x - p2.x);
		final double deltaY = Math.abs(p1.y - p2.y);

		return Math.hypot(deltaX, deltaY);
	};

	Point pointAdd(Point p1, Point p2) {
		return new Point(p1.x + p2.x, p1.y + p2.y);
	};

	Point pointSub(Point p1, Point p2) {
		return new Point(p1.x - p2.x, p1.y - p2.y);
	};

	BufferedImage deepCopy(BufferedImage bi, int x, int y, int h, int w) {
		final ColorModel cm = bi.getColorModel();
		return new BufferedImage(cm, bi.copyData(null), cm.isAlphaPremultiplied(), null).getSubimage(x, y, h, w);
	}
};