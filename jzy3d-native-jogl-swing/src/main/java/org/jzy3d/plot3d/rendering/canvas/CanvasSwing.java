package org.jzy3d.plot3d.rendering.canvas;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jzy3d.awt.AWTHelper;
import org.jzy3d.chart.IAnimator;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.chart.factories.NativePainterFactory;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.painters.IPainter;
import org.jzy3d.painters.NativeDesktopPainter;
import org.jzy3d.plot3d.GPUInfo;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.jzy3d.plot3d.rendering.view.Renderer3d;
import org.jzy3d.plot3d.rendering.view.View;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * @author Martin Pernollet
 */
public class CanvasSwing extends GLJPanel implements IScreenCanvas, INativeCanvas {
  private static final long serialVersionUID = 980088854683562436L;

  protected View view;
  protected Renderer3d renderer;
  protected IAnimator animator;
  protected List<ICanvasListener> canvasListeners = new ArrayList<>();

  protected ScheduledExecutorService exec = new ScheduledThreadPoolExecutor(1);

  /**
   * Initialize a Canvas3d attached to a {@link Scene}, with a given rendering {@link Quality}.
   */
  public CanvasSwing(IChartFactory factory, Scene scene, Quality quality, GLCapabilitiesImmutable glci) {
    super(glci);

    view = scene.newView(this, quality);
    view.getPainter().setCanvas(this);

    renderer = newRenderer(factory);
    addGLEventListener(renderer);

    // swing specific
    setFocusable(true);
    requestFocusInWindow();

    setAutoSwapBufferMode(quality.isAutoSwapBuffer());

    animator = factory.getPainterFactory().newAnimator(this);
    if (quality.isAnimated()) {
      animator.start();
    }

    if (ALLOW_WATCH_PIXEL_SCALE)
      watchPixelScale();

    if (quality.isPreserveViewportSize())
      setPixelScale(newPixelScaleIdentity());
  }

  protected void watchPixelScale() {
    exec.schedule(new PixelScaleWatch() {
      @Override
      public double getPixelScaleY() {
        return CanvasSwing.this.getPixelScaleY();
      }

      @Override
      public double getPixelScaleX() {
        return CanvasSwing.this.getPixelScaleX();
      }

      @Override
      protected void firePixelScaleChanged(double pixelScaleX, double pixelScaleY) {
        CanvasSwing.this.firePixelScaleChanged(pixelScaleX, pixelScaleY);
      }
    }, 0, TimeUnit.SECONDS);
  }


  private float[] newPixelScaleIdentity() {
    return new float[] {ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE};
  }

  private Renderer3d newRenderer(IChartFactory factory) {
    return ((NativePainterFactory) factory.getPainterFactory()).newRenderer3D(view);
  }

  @Override
  public double getLastRenderingTimeMs() {
    return renderer.getLastRenderingTimeMs();
  }

  @Override
  public void setPixelScale(float[] scale) {
    setSurfaceScale(scale);
  }

  /**
   * Pixel scale is used to model the pixel ratio thay may be introduced by HiDPI or Retina
   * displays.
   */
  @Override
  public Coord2d getPixelScale() {
    return new Coord2d(getPixelScaleX(), getPixelScaleY());
  }

  @Override
  public Coord2d getPixelScaleJVM() {
    return new Coord2d(AWTHelper.getPixelScaleX(this), AWTHelper.getPixelScaleY(this));
  }

  public double getPixelScaleX() {
    return getSurfaceWidth() / (double) getWidth();
  }

  public double getPixelScaleY() {
    return getSurfaceHeight() / (double) getHeight();
  }

  @Override
  public IAnimator getAnimation() {
    return animator;
  }

  @Override
  public void dispose() {
    if (animator != null)
      animator.stop();
    if (renderer != null)
      renderer.dispose(this);
    renderer = null;
    view = null;
  }

  /**
   * Force repaint and ensure that GL2 rendering will occur in the GUI thread, wherever the caller
   * stands.
   */
  @Override
  public void forceRepaint() {
    if (true) {
      // -- Method1 --
      // Display() is required to use the GLCanvas procedure and to ensure
      // that GL2 rendering occurs in the
      // GUI thread.
      // Actually it seems to be a bad idea, because this call implies a
      // rendering out of the excepted GL2 thread,
      // which:
      // - is slower than rendering in GL2 Thread
      // - throws java.lang.InterruptedException when rendering occurs
      // while closing the window
      display();
    } else {
      // -- Method2 --
      // Composite.repaint() is required with post/pre rendering, for
      // triggering PostRenderer rendering
      // at each frame (instead of ). The counterpart is that OpenGL2
      // rendering will occurs in the caller thread
      // and thus in the thread where the shoot() method was invoked (such
      // as AWT if shoot() is triggered
      // by a mouse event.
      repaint();
    }
  }


  @Override
  public void screenshot(File file) throws IOException {
    if (!file.getParentFile().exists())
      file.getParentFile().mkdirs();

    TextureData screen = screenshot();
    TextureIO.write(screen, file);
  }

  @Override
  public TextureData screenshot() {

    // setupPrint(1, 1, 1, getRendererWidth(), getRendererHeight());

    if (!isVisible() || !isRealized()) {
      throw new RuntimeException(
          "Can't make a screenshot out of a Swing canvas without making it visible. "
          + "Either call chart.open(), add chart.getCanvas() to an application, or use an OffscreenChartFactory");
      // because the display() method of GLJPanel skip invocation of renderer.display() if
      // the panel is not visible.s
    }



    renderer.nextDisplayUpdateScreenshot();
    display();
    TextureData screenshot = renderer.getLastScreenshot();

    // releasePrint();

    return screenshot;
  }

  /* */

  @Override
  public GLAutoDrawable getDrawable() {
    return this;
  }

  /** Provide a reference to the View that renders into this canvas. */
  @Override
  public View getView() {
    return view;
  }

  /**
   * Provide the actual renderer width for the open gl camera settings, which is obtained after a
   * resize event.
   */
  @Override
  public int getRendererWidth() {
    return (renderer != null ? renderer.getWidth() : 0);
  }

  /**
   * Provide the actual renderer height for the open gl camera settings, which is obtained after a
   * resize event.
   */
  @Override
  public int getRendererHeight() {
    return (renderer != null ? renderer.getHeight() : 0);
  }

  @Override
  public Renderer3d getRenderer() {
    return renderer;
  }

  @Override
  public String getDebugInfo() {
    IPainter painter = getView().getPainter();
    
    GLCapabilitiesImmutable caps = getChosenGLCapabilities();
    
    GL gl = (GL) painter.acquireGL();
    GPUInfo info = GPUInfo.load(gl);
    painter.releaseGL();
    
    return "Capabilities  : " + caps + "\n" + info.toString();
  }
  
  @Override
  public void addMouseController(Object o) {
    addMouseListener((MouseListener) o);

    if (o instanceof MouseWheelListener)
      addMouseWheelListener((MouseWheelListener) o);
    if (o instanceof MouseMotionListener)
      addMouseMotionListener((MouseMotionListener) o);
  }

  @Override
  public void addKeyController(Object o) {
    addKeyListener((KeyListener) o);
  }

  @Override
  public void removeMouseController(Object o) {
    removeMouseListener((MouseListener) o);
  }

  @Override
  public void removeKeyController(Object o) {
    removeKeyListener((KeyListener) o);
  }


  @Override
  public void addCanvasListener(ICanvasListener listener) {
    canvasListeners.add(listener);
  }

  @Override
  public void removeCanvasListener(ICanvasListener listener) {
    canvasListeners.remove(listener);
  }

  @Override
  public List<ICanvasListener> getCanvasListeners() {
    return canvasListeners;
  }

  protected void firePixelScaleChanged(double pixelScaleX, double pixelScaleY) {
    for (ICanvasListener listener : canvasListeners) {
      listener.pixelScaleChanged(pixelScaleX, pixelScaleY);
    }
  }
  
  @Override
  public boolean isNative() {
    return true;
  }
}
