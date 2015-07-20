package org.processmining.plugins.inductiveVisualMiner.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JPanel;

import nl.tue.astar.AStarThread.Canceller;

import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.VideoFormatKeys;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;
import org.processmining.plugins.InductiveMiner.Function;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.NavigableSVGPanel;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerGraphPanel;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.animation.Scaler;
import org.processmining.plugins.inductiveVisualMiner.animation.graphviztoken.GraphVizTokens;
import org.processmining.plugins.inductiveVisualMiner.colouringmode.ColouringMode;

import com.kitfox.svg.SVGDiagram;

public class ExportAnimation {

	public static boolean saveAVItoFile(GraphVizTokens tokens, final AlignedLogVisualisationInfo info,
			final ColouringMode colourMode, final SVGDiagram svg, final Dot dot, final File file, final JPanel panel,
			final Scaler scaler) throws IOException, NoninvertibleTransformException {

		final GuaranteedProgressMonitor progressMonitor = new GuaranteedProgressMonitor(panel, "",
				"Preparing animation", 0, 100);

		final ImageOutputStream streamOutMovie = new FileImageOutputStream(file);

		//set up progress monitor and new canceller
		final Canceller canceller = new Canceller() {
			public boolean isCancelled() {
				return progressMonitor.isCanceled();
			}
		};

		//set constants for this animation
		double timeMargin = 0.5; //idle time before first tokens start and after final token ends
		int framerate = 30;
		int width = 1500; //width of the movie

		//compute the time bounds for the movie
		double minDuration = scaler.getMinInUserTime() - timeMargin;
		double maxDuration = scaler.getMaxInUserTime() + timeMargin;

		//compute the height from the width
		int height = (int) (width * (svg.getHeight() / (svg.getWidth() * 1.0)));

		progressMonitor.setNote("Rendering animation..");

		//compute the number of frames
		long frames = Math.round((maxDuration - minDuration) * (framerate * 1.0));

		//create format
		Format format = new Format(VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_MJPG,
				VideoFormatKeys.DepthKey, 24);
		format = format.prepend(FormatKeys.MediaTypeKey, MediaType.VIDEO, //
				FormatKeys.FrameRateKey, new Rational(framerate, 1),//
				VideoFormatKeys.WidthKey, width, //
				VideoFormatKeys.HeightKey, height);

		// Create a buffered image for this format
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		//render the movie
		AVIWriter out = null;
		try {
			// Create the writer
			out = new AVIWriter(streamOutMovie);

			// Add a track to the writer
			out.addTrack(format);
			out.setPalette(0, img.getColorModel());

			// initialize the animation
			g.setBackground(Color.WHITE);
			g.clearRect(0, 0, img.getWidth(), img.getHeight());
			
			AffineTransform transform = new AffineTransform();
			transform.scale(height / svg.getHeight(), height / svg.getHeight());
			AffineTransform transformInverse = transform.createInverse();

			//			System.out.println("frames " + frames);

			for (long frame = 0; frame < frames; frame++) {

				double time = minDuration + ((frame / (1.0 * frames)) * (maxDuration - minDuration));

				//paint the background
				NavigableSVGPanel.drawSVG(g, svg, 0, 0, width, height);

				//transform
				g.transform(transform);
				
				//paint the tokens
				InductiveVisualMinerGraphPanel.paintTokens(g, tokens, time, false, false);
				
				g.transform(transformInverse);

				// write it to the writer
				out.write(0, img, 1);

				//write progress
				progressMonitor.setProgress((int) Math.round(frame / (frames / 100.0)));
				if (canceller.isCancelled()) {
					return false;
				}
			}

		} finally {
			// Close the writer
			if (out != null) {
				out.close();
			}

			// Dispose the graphics object
			g.dispose();
		}

		progressMonitor.close();
		return !canceller.isCancelled();
	}

	public static PipedInputStream copy(final Function<PipedOutputStream, Object> f) throws IOException {
		PipedInputStream in = new PipedInputStream();
		final PipedOutputStream out = new PipedOutputStream(in);
		new Thread(new Runnable() {
			public void run() {
				try {
					f.call(out);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return in;
	}
}
